/******************************************************
 * nf_tproxy_core.h
 */
#define NFT_LOOKUP_ANY         0
#define NFT_LOOKUP_LISTENER    1
#define NFT_LOOKUP_ESTABLISHED 2

struct sock *
nf_tproxy_get_sock_v4(struct net *net, const u8 protocol,
		      const __be32 saddr, const __be32 daddr,
		      const __be16 sport, const __be16 dport,
		      const struct net_device *in, bool listening_only)
{
	struct sock *sk;

	/* look up socket */
	switch (protocol) {
	case IPPROTO_TCP:
		if (listening_only)
			sk = __inet_lookup_listener(net, &tcp_hashinfo,
						    daddr, ntohs(dport),
						    in->ifindex);
		else
			sk = __inet_lookup(net, &tcp_hashinfo,
					   saddr, sport, daddr, dport,
					   in->ifindex);
		break;
	case IPPROTO_UDP:
		sk = udp4_lib_lookup(net, saddr, sport, daddr, dport,
				     in->ifindex);
		break;
	default:
		WARN_ON(1);
		sk = NULL;
	}

	pr_debug("tproxy socket lookup: proto %u %08x:%u -> %08x:%u, listener only: %d, sock %p\n",
		 protocol, ntohl(saddr), ntohs(sport), ntohl(daddr), ntohs(dport), listening_only, sk);

	return sk;
}

static inline struct sock *
nf_tproxy_get_sock_v6(struct net *net, const u8 protocol,
		      const struct in6_addr *saddr, const struct in6_addr *daddr,
		      const __be16 sport, const __be16 dport,
		      const struct net_device *in, int lookup_type)
{
	struct sock *sk;

	/* look up socket */
	switch (protocol) {
	case IPPROTO_TCP:
		switch (lookup_type) {
		case NFT_LOOKUP_ANY:
			sk = inet6_lookup(net, &tcp_hashinfo,
					  saddr, sport, daddr, dport,
					  in->ifindex);
			break;
		case NFT_LOOKUP_LISTENER:
			sk = inet6_lookup_listener(net, &tcp_hashinfo,
						   daddr, ntohs(dport),
						   in->ifindex);

			/* NOTE: we return listeners even if bound to
			 * 0.0.0.0, those are filtered out in
			 * xt_socket, since xt_TPROXY needs 0 bound
			 * listeners too */

			break;
		case NFT_LOOKUP_ESTABLISHED:
			sk = __inet6_lookup_established(net, &tcp_hashinfo,
							saddr, sport, daddr, ntohs(dport),
							in->ifindex);
			break;
		default:
			WARN_ON(1);
			sk = NULL;
			break;
		}
		break;
	case IPPROTO_UDP:
                /* FIXME
		sk = udp6_lib_lookup(net, saddr, sport, daddr, dport, in->ifindex);
                */
		WARN_ON(1); /* HACK */
		if (sk && lookup_type != NFT_LOOKUP_ANY) {
			/* NOTE: we return listeners even if bound to
			 * 0.0.0.0, those are filtered out in
			 * xt_socket, since xt_TPROXY needs 0 bound
			 * listeners too */
			int connected = (sk->sk_state == TCP_ESTABLISHED);
			int wildcard = ipv6_addr_any(&inet6_sk(sk)->rcv_saddr);
			if ((lookup_type == NFT_LOOKUP_ESTABLISHED && (!connected || wildcard)) ||
			    (lookup_type == NFT_LOOKUP_LISTENER && connected)) {
				sock_put(sk);
				sk = NULL;
			}
		}
		break;
	default:
		WARN_ON(1);
		sk = NULL;
	}

	pr_debug("tproxy socket lookup: proto %u %pI6:%u -> %pI6:%u, lookup type: %d, sock %p\n",
		 protocol, saddr, ntohs(sport), daddr, ntohs(dport), lookup_type, sk);

	return sk;
}

/**************************************************************
 * xt_socket.c missing functions 
 */
static int
extract_icmp4_fields(const struct sk_buff *skb,
		    u8 *protocol,
		    __be32 *raddr,
		    __be32 *laddr,
		    __be16 *rport,
		    __be16 *lport)
{
	unsigned int outside_hdrlen = ip_hdrlen(skb);
	struct iphdr *inside_iph, _inside_iph;
	struct icmphdr *icmph, _icmph;
	__be16 *ports, _ports[2];

	icmph = skb_header_pointer(skb, outside_hdrlen,
				   sizeof(_icmph), &_icmph);
	if (icmph == NULL)
		return 1;

	switch (icmph->type) {
	case ICMP_DEST_UNREACH:
	case ICMP_SOURCE_QUENCH:
	case ICMP_REDIRECT:
	case ICMP_TIME_EXCEEDED:
	case ICMP_PARAMETERPROB:
		break;
	default:
		return 1;
	}

	inside_iph = skb_header_pointer(skb, outside_hdrlen +
					sizeof(struct icmphdr),
					sizeof(_inside_iph), &_inside_iph);
	if (inside_iph == NULL)
		return 1;

	if (inside_iph->protocol != IPPROTO_TCP &&
	    inside_iph->protocol != IPPROTO_UDP)
		return 1;

	ports = skb_header_pointer(skb, outside_hdrlen +
				   sizeof(struct icmphdr) +
				   (inside_iph->ihl << 2),
				   sizeof(_ports), &_ports);
	if (ports == NULL)
		return 1;

	/* the inside IP packet is the one quoted from our side, thus
	 * its saddr is the local address */
	*protocol = inside_iph->protocol;
	*laddr = inside_iph->saddr;
	*lport = ports[0];
	*raddr = inside_iph->daddr;
	*rport = ports[1];

	return 0;
}

static int
extract_icmp6_fields(const struct sk_buff *skb,
		     unsigned int outside_hdrlen,
		     int *protocol,
		     struct in6_addr **raddr,
		     struct in6_addr **laddr,
		     __be16 *rport,
		     __be16 *lport)
{
	struct ipv6hdr *inside_iph, _inside_iph;
	struct icmp6hdr *icmph, _icmph;
	__be16 *ports, _ports[2];
	u8 inside_nexthdr;
	int inside_hdrlen;

	icmph = skb_header_pointer(skb, outside_hdrlen,
				   sizeof(_icmph), &_icmph);
	if (icmph == NULL)
		return 1;

	if (icmph->icmp6_type & ICMPV6_INFOMSG_MASK)
		return 1;

	inside_iph = skb_header_pointer(skb, outside_hdrlen + sizeof(_icmph), sizeof(_inside_iph), &_inside_iph);
	if (inside_iph == NULL)
		return 1;
	inside_nexthdr = inside_iph->nexthdr;

	inside_hdrlen = ipv6_skip_exthdr(skb, outside_hdrlen + sizeof(_icmph) + sizeof(_inside_iph), &inside_nexthdr);
	if (inside_hdrlen < 0)
		return 1; /* hjm: Packet has no/incomplete transport layer headers. */

	if (inside_nexthdr != IPPROTO_TCP &&
	    inside_nexthdr != IPPROTO_UDP)
		return 1;

	ports = skb_header_pointer(skb, inside_hdrlen,
				   sizeof(_ports), &_ports);
	if (ports == NULL)
		return 1;

	/* the inside IP packet is the one quoted from our side, thus
	 * its saddr is the local address */
	*protocol = inside_nexthdr;
	*laddr = &inside_iph->saddr;
	*lport = ports[0];
	*raddr = &inside_iph->daddr;
	*rport = ports[1];

	return 0;
}
void
xt_socket_put_sk(struct sock *sk)
{
	if (sk->sk_state == TCP_TIME_WAIT)
		inet_twsk_put(inet_twsk(sk));
	else
		sock_put(sk);
}

struct sock*
xt_socket_get4_sk(const struct sk_buff *skb, struct xt_action_param *par)
{
	const struct iphdr *iph = ip_hdr(skb);
	struct udphdr _hdr, *hp = NULL;
	struct sock *sk;
	__be32 daddr, saddr;
	__be16 dport, sport;
	u8 protocol;
#ifdef XT_SOCKET_HAVE_CONNTRACK
	struct nf_conn const *ct;
	enum ip_conntrack_info ctinfo;
#endif

	if (iph->protocol == IPPROTO_UDP || iph->protocol == IPPROTO_TCP) {
		hp = skb_header_pointer(skb, ip_hdrlen(skb),
					sizeof(_hdr), &_hdr);
		if (hp == NULL)
			return NULL;

		protocol = iph->protocol;
		saddr = iph->saddr;
		sport = hp->source;
		daddr = iph->daddr;
		dport = hp->dest;

	} else if (iph->protocol == IPPROTO_ICMP) {
		if (extract_icmp4_fields(skb, &protocol, &saddr, &daddr,
					&sport, &dport))
			return NULL;
	} else {
		return NULL;
	}

#ifdef XT_SOCKET_HAVE_CONNTRACK
	/* Do the lookup with the original socket address in case this is a
	 * reply packet of an established SNAT-ted connection. */

	ct = nf_ct_get(skb, &ctinfo);
	if (ct && !nf_ct_is_untracked(ct) &&
	    ((iph->protocol != IPPROTO_ICMP &&
	      ctinfo == IP_CT_ESTABLISHED_REPLY) ||
	     (iph->protocol == IPPROTO_ICMP &&
	      ctinfo == IP_CT_RELATED_REPLY)) &&
	    (ct->status & IPS_SRC_NAT_DONE)) {

		daddr = ct->tuplehash[IP_CT_DIR_ORIGINAL].tuple.src.u3.ip;
		dport = (iph->protocol == IPPROTO_TCP) ?
			ct->tuplehash[IP_CT_DIR_ORIGINAL].tuple.src.u.tcp.port :
			ct->tuplehash[IP_CT_DIR_ORIGINAL].tuple.src.u.udp.port;
	}
#endif

	sk = nf_tproxy_get_sock_v4(dev_net(skb->dev), protocol,
				   saddr, daddr, sport, dport, par->in, NFT_LOOKUP_ANY);

	pr_debug("proto %hhu %pI4:%hu -> %pI4:%hu (orig %pI4:%hu) sock %p\n",
		 protocol, &saddr, ntohs(sport),
		 &daddr, ntohs(dport),
		 &iph->daddr, hp ? ntohs(hp->dest) : 0, sk);

	return sk;
}

struct sock*
xt_socket_get6_sk(const struct sk_buff *skb, struct xt_action_param *par)
{
	struct ipv6hdr *iph = ipv6_hdr(skb);
	struct udphdr _hdr, *hp = NULL;
	struct sock *sk;
	struct in6_addr *daddr, *saddr;
	__be16 dport, sport;
	int thoff = 0, tproto = -1;

	/* FIXME: tproto = ipv6_find_hdr(skb, &thoff, -1, NULL); */
	if (tproto < 0) {
		pr_debug("unable to find transport header in IPv6 packet, dropping\n");
		return NF_DROP;
	}

	if (tproto == IPPROTO_UDP || tproto == IPPROTO_TCP) {
		hp = skb_header_pointer(skb, thoff,
					sizeof(_hdr), &_hdr);
		if (hp == NULL)
			return NULL;

		saddr = &iph->saddr;
		sport = hp->source;
		daddr = &iph->daddr;
		dport = hp->dest;

	} else if (tproto == IPPROTO_ICMPV6) {
		if (extract_icmp6_fields(skb, thoff, &tproto, &saddr, &daddr,
					 &sport, &dport))
			return NULL;
	} else {
		return NULL;
	}

	sk = nf_tproxy_get_sock_v6(dev_net(skb->dev), tproto,
				   saddr, daddr, sport, dport, par->in, NFT_LOOKUP_ANY);
	pr_debug("proto %hhd %pI6:%hu -> %pI6:%hu "
		 "(orig %pI6:%hu) sock %p\n",
		 tproto, saddr, ntohs(sport),
		 daddr, ntohs(dport),
		 &iph->daddr, hp ? ntohs(hp->dest) : 0, sk);
	return sk;
}


