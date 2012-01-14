/* ALL INCLUDES */
#include <linux/kernel.h>
#include <linux/device.h>
#include <linux/err.h>
#include <linux/file.h>
#include <linux/fs.h>
#include <linux/io.h>
#include <linux/spinlock.h>
#include <linux/mm.h>
#include <linux/mm_types.h>
#include <linux/scatterlist.h>
#include <linux/slab.h>
#include <linux/vmalloc.h>
#include <linux/genalloc.h>
#include <linux/anon_inodes.h>
#include <linux/list.h>
#include <linux/miscdevice.h>
#include <linux/rbtree.h>
#include <linux/sched.h>
#include <linux/seq_file.h>
#include <linux/uaccess.h>
#include <linux/debugfs.h>

#include <linux/platform_device.h>

#include <asm/mach/map.h>
#include <asm/page.h>

#include "ion.h"
#include "ion_priv.h"
#include "tiler.h"
#include "omap_ion.h"
#include "omap_ion_priv.h"

#define DEBUG

/*
 * drivers/gpu/ion/ion_heap.c
 *
 * Copyright (C) 2011 Google, Inc.
 *
 * This software is licensed under the terms of the GNU General Public
 * License version 2, as published by the Free Software Foundation, and
 * may be copied, distributed, and modified under those terms.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 */

struct ion_heap *ion_heap_create(struct ion_platform_heap *heap_data)
{
	struct ion_heap *heap = NULL;

	switch (heap_data->type) {
	case ION_HEAP_TYPE_SYSTEM_CONTIG:
		heap = ion_system_contig_heap_create(heap_data);
		break;
	case ION_HEAP_TYPE_SYSTEM:
		heap = ion_system_heap_create(heap_data);
		break;
	case ION_HEAP_TYPE_CARVEOUT:
		heap = ion_carveout_heap_create(heap_data);
		break;
	default:
		pr_err("%s: Invalid heap type %d\n", __func__,
		       heap_data->type);
		return ERR_PTR(-EINVAL);
	}

	if (IS_ERR_OR_NULL(heap)) {
		pr_err("%s: error creating heap %s type %d base %lu size %u\n",
		       __func__, heap_data->name, heap_data->type,
		       heap_data->base, heap_data->size);
		return ERR_PTR(-EINVAL);
	}

	heap->name = heap_data->name;
	heap->id = heap_data->id;
	return heap;
}

void ion_heap_destroy(struct ion_heap *heap)
{
	if (!heap)
		return;

	switch (heap->type) {
	case ION_HEAP_TYPE_SYSTEM_CONTIG:
		ion_system_contig_heap_destroy(heap);
		break;
	case ION_HEAP_TYPE_SYSTEM:
		ion_system_heap_destroy(heap);
		break;
	case ION_HEAP_TYPE_CARVEOUT:
		ion_carveout_heap_destroy(heap);
		break;
	default:
		pr_err("%s: Invalid heap type %d\n", __func__,
		       heap->type);
	}
}

/*
 * drivers/gpu/ion/ion_system_heap.c
 *
 * Copyright (C) 2011 Google, Inc.
 *
 * This software is licensed under the terms of the GNU General Public
 * License version 2, as published by the Free Software Foundation, and
 * may be copied, distributed, and modified under those terms.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 */

static int ion_system_heap_allocate(struct ion_heap *heap,
				     struct ion_buffer *buffer,
				     unsigned long size, unsigned long align,
				     unsigned long flags)
{
	buffer->priv_virt = vmalloc_user(size);
	if (!buffer->priv_virt)
		return -ENOMEM;
	return 0;
}

void ion_system_heap_free(struct ion_buffer *buffer)
{
	vfree(buffer->priv_virt);
}

struct scatterlist *ion_system_heap_map_dma(struct ion_heap *heap,
					    struct ion_buffer *buffer)
{
	struct scatterlist *sglist;
	struct page *page;
	int i;
	int npages = PAGE_ALIGN(buffer->size) / PAGE_SIZE;
	void *vaddr = buffer->priv_virt;

	sglist = vmalloc(npages * sizeof(struct scatterlist));
	if (!sglist)
		return ERR_PTR(-ENOMEM);
	memset(sglist, 0, npages * sizeof(struct scatterlist));
	sg_init_table(sglist, npages);
	for (i = 0; i < npages; i++) {
		page = vmalloc_to_page(vaddr);
		if (!page)
			goto end;
		sg_set_page(&sglist[i], page, PAGE_SIZE, 0);
		vaddr += PAGE_SIZE;
	}
	/* XXX do cache maintenance for dma? */
	return sglist;
end:
	vfree(sglist);
	return NULL;
}

void ion_system_heap_unmap_dma(struct ion_heap *heap,
			       struct ion_buffer *buffer)
{
	/* XXX undo cache maintenance for dma? */
	if (buffer->sglist)
		vfree(buffer->sglist);
}

void *ion_system_heap_map_kernel(struct ion_heap *heap,
				 struct ion_buffer *buffer)
{
	return buffer->priv_virt;
}

void ion_system_heap_unmap_kernel(struct ion_heap *heap,
				  struct ion_buffer *buffer)
{
}

int ion_system_heap_map_user(struct ion_heap *heap, struct ion_buffer *buffer,
			     struct vm_area_struct *vma)
{
	return remap_vmalloc_range(vma, buffer->priv_virt, vma->vm_pgoff);
}

static struct ion_heap_ops vmalloc_ops = {
	.allocate = ion_system_heap_allocate,
	.free = ion_system_heap_free,
	.map_dma = ion_system_heap_map_dma,
	.unmap_dma = ion_system_heap_unmap_dma,
	.map_kernel = ion_system_heap_map_kernel,
	.unmap_kernel = ion_system_heap_unmap_kernel,
	.map_user = ion_system_heap_map_user,
};

struct ion_heap *ion_system_heap_create(struct ion_platform_heap *unused)
{
	struct ion_heap *heap;

	heap = kzalloc(sizeof(struct ion_heap), GFP_KERNEL);
	if (!heap)
		return ERR_PTR(-ENOMEM);
	heap->ops = &vmalloc_ops;
	heap->type = ION_HEAP_TYPE_SYSTEM;
	return heap;
}

void ion_system_heap_destroy(struct ion_heap *heap)
{
	kfree(heap);
}

static int ion_system_contig_heap_allocate(struct ion_heap *heap,
					   struct ion_buffer *buffer,
					   unsigned long len,
					   unsigned long align,
					   unsigned long flags)
{
	buffer->priv_virt = kzalloc(len, GFP_KERNEL);
	if (!buffer->priv_virt)
		return -ENOMEM;
	return 0;
}

void ion_system_contig_heap_free(struct ion_buffer *buffer)
{
	kfree(buffer->priv_virt);
}

static int ion_system_contig_heap_phys(struct ion_heap *heap,
				       struct ion_buffer *buffer,
				       ion_phys_addr_t *addr, size_t *len)
{
	*addr = virt_to_phys(buffer->priv_virt);
	*len = buffer->size;
	return 0;
}

struct scatterlist *ion_system_contig_heap_map_dma(struct ion_heap *heap,
						   struct ion_buffer *buffer)
{
	struct scatterlist *sglist;

	sglist = vmalloc(sizeof(struct scatterlist));
	if (!sglist)
		return ERR_PTR(-ENOMEM);
	sg_init_table(sglist, 1);
	sg_set_page(sglist, virt_to_page(buffer->priv_virt), buffer->size, 0);
	return sglist;
}

int ion_system_contig_heap_map_user(struct ion_heap *heap,
				    struct ion_buffer *buffer,
				    struct vm_area_struct *vma)
{
	unsigned long pfn = __phys_to_pfn(virt_to_phys(buffer->priv_virt));
	return remap_pfn_range(vma, vma->vm_start, pfn + vma->vm_pgoff,
			       vma->vm_end - vma->vm_start,
			       vma->vm_page_prot);

}

static struct ion_heap_ops kmalloc_ops = {
	.allocate = ion_system_contig_heap_allocate,
	.free = ion_system_contig_heap_free,
	.phys = ion_system_contig_heap_phys,
	.map_dma = ion_system_contig_heap_map_dma,
	.unmap_dma = ion_system_heap_unmap_dma,
	.map_kernel = ion_system_heap_map_kernel,
	.unmap_kernel = ion_system_heap_unmap_kernel,
	.map_user = ion_system_contig_heap_map_user,
};

struct ion_heap *ion_system_contig_heap_create(struct ion_platform_heap *unused)
{
	struct ion_heap *heap;

	heap = kzalloc(sizeof(struct ion_heap), GFP_KERNEL);
	if (!heap)
		return ERR_PTR(-ENOMEM);
	heap->ops = &kmalloc_ops;
	heap->type = ION_HEAP_TYPE_SYSTEM_CONTIG;
	return heap;
}

void ion_system_contig_heap_destroy(struct ion_heap *heap)
{
	kfree(heap);
}


/*
 * drivers/gpu/ion/ion_carveout_heap.c
 *
 * Copyright (C) 2011 Google, Inc.
 *
 * This software is licensed under the terms of the GNU General Public
 * License version 2, as published by the Free Software Foundation, and
 * may be copied, distributed, and modified under those terms.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 */

struct ion_carveout_heap {
	struct ion_heap heap;
	struct gen_pool *pool;
	ion_phys_addr_t base;
};

ion_phys_addr_t ion_carveout_allocate(struct ion_heap *heap,
				      unsigned long size,
				      unsigned long align)
{
	struct ion_carveout_heap *carveout_heap =
		container_of(heap, struct ion_carveout_heap, heap);
	unsigned long offset = gen_pool_alloc(carveout_heap->pool, size);

	if (!offset)
		return ION_CARVEOUT_ALLOCATE_FAIL;

	return offset;
}

void ion_carveout_free(struct ion_heap *heap, ion_phys_addr_t addr,
		       unsigned long size)
{
	struct ion_carveout_heap *carveout_heap =
		container_of(heap, struct ion_carveout_heap, heap);

	if (addr == ION_CARVEOUT_ALLOCATE_FAIL)
		return;
	gen_pool_free(carveout_heap->pool, addr, size);
}

static int ion_carveout_heap_phys(struct ion_heap *heap,
				  struct ion_buffer *buffer,
				  ion_phys_addr_t *addr, size_t *len)
{
	*addr = buffer->priv_phys;
	*len = buffer->size;
	return 0;
}

static int ion_carveout_heap_allocate(struct ion_heap *heap,
				      struct ion_buffer *buffer,
				      unsigned long size, unsigned long align,
				      unsigned long flags)
{
	buffer->priv_phys = ion_carveout_allocate(heap, size, align);
	return buffer->priv_phys == ION_CARVEOUT_ALLOCATE_FAIL ? -ENOMEM : 0;
}

static void ion_carveout_heap_free(struct ion_buffer *buffer)
{
	struct ion_heap *heap = buffer->heap;

	ion_carveout_free(heap, buffer->priv_phys, buffer->size);
	buffer->priv_phys = ION_CARVEOUT_ALLOCATE_FAIL;
}

struct scatterlist *ion_carveout_heap_map_dma(struct ion_heap *heap,
					      struct ion_buffer *buffer)
{
	return ERR_PTR(-EINVAL);
}

void ion_carveout_heap_unmap_dma(struct ion_heap *heap,
				 struct ion_buffer *buffer)
{
	return;
}

void *ion_carveout_heap_map_kernel(struct ion_heap *heap,
				   struct ion_buffer *buffer)
{
	return __arch_ioremap(buffer->priv_phys, buffer->size,
			      MT_MEMORY_NONCACHED);
}

void ion_carveout_heap_unmap_kernel(struct ion_heap *heap,
				    struct ion_buffer *buffer)
{
	__arch_iounmap(buffer->vaddr);
	buffer->vaddr = NULL;
	return;
}

int ion_carveout_heap_map_user(struct ion_heap *heap, struct ion_buffer *buffer,
			       struct vm_area_struct *vma)
{
	return remap_pfn_range(vma, vma->vm_start,
			       __phys_to_pfn(buffer->priv_phys) + vma->vm_pgoff,
			       buffer->size,
			       pgprot_noncached(vma->vm_page_prot));
}

static struct ion_heap_ops carveout_heap_ops = {
	.allocate = ion_carveout_heap_allocate,
	.free = ion_carveout_heap_free,
	.phys = ion_carveout_heap_phys,
	.map_user = ion_carveout_heap_map_user,
	.map_kernel = ion_carveout_heap_map_kernel,
	.unmap_kernel = ion_carveout_heap_unmap_kernel,
};

struct ion_heap *ion_carveout_heap_create(struct ion_platform_heap *heap_data)
{
	struct ion_carveout_heap *carveout_heap;

	carveout_heap = kzalloc(sizeof(struct ion_carveout_heap), GFP_KERNEL);
	if (!carveout_heap)
		return ERR_PTR(-ENOMEM);

	carveout_heap->pool = gen_pool_create(12, -1);
	if (!carveout_heap->pool) {
		kfree(carveout_heap);
		return ERR_PTR(-ENOMEM);
	}
	carveout_heap->base = heap_data->base;
	gen_pool_add(carveout_heap->pool, carveout_heap->base, heap_data->size,
		     -1);
	carveout_heap->heap.ops = &carveout_heap_ops;
	carveout_heap->heap.type = ION_HEAP_TYPE_CARVEOUT;

	return &carveout_heap->heap;
}

void ion_carveout_heap_destroy(struct ion_heap *heap)
{
	struct ion_carveout_heap *carveout_heap =
	     container_of(heap, struct  ion_carveout_heap, heap);

	gen_pool_destroy(carveout_heap->pool);
	kfree(carveout_heap);
	carveout_heap = NULL;
}

/*
 * drivers/gpu/ion/ion.c
 *
 * Copyright (C) 2011 Google, Inc.
 *
 * This software is licensed under the terms of the GNU General Public
 * License version 2, as published by the Free Software Foundation, and
 * may be copied, distributed, and modified under those terms.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 */


/**
 * struct ion_device - the metadata of the ion device node
 * @dev:		the actual misc device
 * @buffers:	an rb tree of all the existing buffers
 * @lock:		lock protecting the buffers & heaps trees
 * @heaps:		list of all the heaps in the system
 * @user_clients:	list of all the clients created from userspace
 */
struct ion_device {
	struct miscdevice dev;
	struct rb_root buffers;
	struct mutex lock;
	struct rb_root heaps;
	long (*custom_ioctl) (struct ion_client *client, unsigned int cmd,
			      unsigned long arg);
	struct rb_root user_clients;
	struct rb_root kernel_clients;
	struct dentry *debug_root;
};

/**
 * struct ion_client - a process/hw block local address space
 * @ref:		for reference counting the client
 * @node:		node in the tree of all clients
 * @dev:		backpointer to ion device
 * @handles:		an rb tree of all the handles in this client
 * @lock:		lock protecting the tree of handles
 * @heap_mask:		mask of all supported heaps
 * @name:		used for debugging
 * @task:		used for debugging
 *
 * A client represents a list of buffers this client may access.
 * The mutex stored here is used to protect both handles tree
 * as well as the handles themselves, and should be held while modifying either.
 */
struct ion_client {
	struct kref ref;
	struct rb_node node;
	struct ion_device *dev;
	struct rb_root handles;
	struct mutex lock;
	unsigned int heap_mask;
	const char *name;
	struct task_struct *task;
	pid_t pid;
	struct dentry *debug_root;
};

/**
 * ion_handle - a client local reference to a buffer
 * @ref:		reference count
 * @client:		back pointer to the client the buffer resides in
 * @buffer:		pointer to the buffer
 * @node:		node in the client's handle rbtree
 * @kmap_cnt:		count of times this client has mapped to kernel
 * @dmap_cnt:		count of times this client has mapped for dma
 * @usermap_cnt:	count of times this client has mapped for userspace
 *
 * Modifications to node, map_cnt or mapping should be protected by the
 * lock in the client.  Other fields are never changed after initialization.
 */
struct ion_handle {
	struct kref ref;
	struct ion_client *client;
	struct ion_buffer *buffer;
	struct rb_node node;
	unsigned int kmap_cnt;
	unsigned int dmap_cnt;
	unsigned int usermap_cnt;
};

/* MISSING FUNCTIONS */
static inline void rb_init_node(struct rb_node *rb)
{
	rb->rb_parent_color = 0;
	rb->rb_right = NULL;
	rb->rb_left = NULL;
	RB_CLEAR_NODE(rb);
}


/****************************************************************
** kernel/cred.c */

/*
static inline int read_cred_subscribers(const struct cred *cred)
{
	return 0;
}

static inline void alter_cred_subscribers(const struct cred *_cred, int n)
{
}

void exit_creds(struct task_struct *tsk)
{
	struct cred *cred;

	//kdebug("exit_creds(%u,%p,%p,{%d,%d})", tsk->pid, tsk->real_cred, tsk->cred,
	//       atomic_read(&tsk->cred->usage),
	//       read_cred_subscribers(tsk->cred));

	cred = (struct cred *) tsk->real_cred;
	tsk->real_cred = NULL;
	validate_creds(cred);
	alter_cred_subscribers(cred, -1);
	put_cred(cred);

	cred = (struct cred *) tsk->cred;
	tsk->cred = NULL;
	validate_creds(cred);
	alter_cred_subscribers(cred, -1);
	put_cred(cred);

	cred = (struct cred *) tsk->replacement_session_keyring;
	if (cred) {
		tsk->replacement_session_keyring = NULL;
		validate_creds(cred);
		put_cred(cred);
	}
}
*/
/**************************************************
** kernel/fork.c */

/*
#include <linux/taskstats_kern.h>
#include <linux/delayacct.h>
#include <linux/profile.h>

static struct kmem_cache *signal_cachep;
static ATOMIC_NOTIFIER_HEAD(task_free_notifier);

static inline void free_signal_struct(struct signal_struct *sig)
{
	taskstats_tgid_free(sig);
	kmem_cache_free(signal_cachep, sig);
}

static inline void put_signal_struct(struct signal_struct *sig)
{
	if (atomic_dec_and_test(&sig->sigcnt))
		free_signal_struct(sig);
}

void __put_task_struct(struct task_struct *tsk)
{
	WARN_ON(!tsk->exit_state);
	WARN_ON(atomic_read(&tsk->usage));
	WARN_ON(tsk == current);

	exit_creds(tsk);
	delayacct_tsk_free(tsk);
	put_signal_struct(tsk->signal);

	atomic_notifier_call_chain(&task_free_notifier, 0, tsk);
	if (!profile_handoff_task(tsk))
		free_task(tsk);
}
*/
/*********************************************************************************************************/

/* this function should only be called while dev->lock is held */
static void ion_buffer_add(struct ion_device *dev,
			   struct ion_buffer *buffer)
{
	struct rb_node **p = &dev->buffers.rb_node;
	struct rb_node *parent = NULL;
	struct ion_buffer *entry;

	while (*p) {
		parent = *p;
		entry = rb_entry(parent, struct ion_buffer, node);

		if (buffer < entry) {
			p = &(*p)->rb_left;
		} else if (buffer > entry) {
			p = &(*p)->rb_right;
		} else {
			pr_err("%s: buffer already found.", __func__);
			BUG();
		}
	}

	rb_link_node(&buffer->node, parent, p);
	rb_insert_color(&buffer->node, &dev->buffers);
}

/* this function should only be called while dev->lock is held */
static struct ion_buffer *ion_buffer_create(struct ion_heap *heap,
				     struct ion_device *dev,
				     unsigned long len,
				     unsigned long align,
				     unsigned long flags)
{
	struct ion_buffer *buffer;
	int ret;

	buffer = kzalloc(sizeof(struct ion_buffer), GFP_KERNEL);
	if (!buffer)
		return ERR_PTR(-ENOMEM);

	buffer->heap = heap;
	kref_init(&buffer->ref);

	ret = heap->ops->allocate(heap, buffer, len, align, flags);
	if (ret) {
		kfree(buffer);
		return ERR_PTR(ret);
	}
	buffer->dev = dev;
	buffer->size = len;
	mutex_init(&buffer->lock);
	ion_buffer_add(dev, buffer);
	return buffer;
}

static void ion_buffer_destroy(struct kref *kref)
{
	struct ion_buffer *buffer = container_of(kref, struct ion_buffer, ref);
	struct ion_device *dev = buffer->dev;

	buffer->heap->ops->free(buffer);
	mutex_lock(&dev->lock);
	rb_erase(&buffer->node, &dev->buffers);
	mutex_unlock(&dev->lock);
	kfree(buffer);
}

static void ion_buffer_get(struct ion_buffer *buffer)
{
	kref_get(&buffer->ref);
}

static int ion_buffer_put(struct ion_buffer *buffer)
{
	return kref_put(&buffer->ref, ion_buffer_destroy);
}

static struct ion_handle *ion_handle_create(struct ion_client *client,
				     struct ion_buffer *buffer)
{
	struct ion_handle *handle;

	handle = kzalloc(sizeof(struct ion_handle), GFP_KERNEL);
	if (!handle)
		return ERR_PTR(-ENOMEM);
	kref_init(&handle->ref);
	rb_init_node(&handle->node);
	handle->client = client;
	ion_buffer_get(buffer);
	handle->buffer = buffer;

	return handle;
}

static void ion_handle_destroy(struct kref *kref)
{
	struct ion_handle *handle = container_of(kref, struct ion_handle, ref);
	/* XXX Can a handle be destroyed while it's map count is non-zero?:
	   if (handle->map_cnt) unmap
	 */
	ion_buffer_put(handle->buffer);
	mutex_lock(&handle->client->lock);
	if (!RB_EMPTY_NODE(&handle->node))
		rb_erase(&handle->node, &handle->client->handles);
	mutex_unlock(&handle->client->lock);
	kfree(handle);
}

struct ion_buffer *ion_handle_buffer(struct ion_handle *handle)
{
	return handle->buffer;
}

static void ion_handle_get(struct ion_handle *handle)
{
	kref_get(&handle->ref);
}

static int ion_handle_put(struct ion_handle *handle)
{
	return kref_put(&handle->ref, ion_handle_destroy);
}

static struct ion_handle *ion_handle_lookup(struct ion_client *client,
					    struct ion_buffer *buffer)
{
	struct rb_node *n;

	for (n = rb_first(&client->handles); n; n = rb_next(n)) {
		struct ion_handle *handle = rb_entry(n, struct ion_handle,
						     node);
		if (handle->buffer == buffer)
			return handle;
	}
	return NULL;
}

static bool ion_handle_validate(struct ion_client *client, struct ion_handle *handle)
{
	struct rb_node *n = client->handles.rb_node;

	while (n) {
		struct ion_handle *handle_node = rb_entry(n, struct ion_handle,
							  node);
		if (handle < handle_node)
			n = n->rb_left;
		else if (handle > handle_node)
			n = n->rb_right;
		else
			return true;
	}
	return false;
}

static void ion_handle_add(struct ion_client *client, struct ion_handle *handle)
{
	struct rb_node **p = &client->handles.rb_node;
	struct rb_node *parent = NULL;
	struct ion_handle *entry;

	while (*p) {
		parent = *p;
		entry = rb_entry(parent, struct ion_handle, node);

		if (handle < entry)
			p = &(*p)->rb_left;
		else if (handle > entry)
			p = &(*p)->rb_right;
		else
			WARN(1, "%s: buffer already found.", __func__);
	}

	rb_link_node(&handle->node, parent, p);
	rb_insert_color(&handle->node, &client->handles);
}

struct ion_handle *ion_alloc(struct ion_client *client, size_t len,
			     size_t align, unsigned int flags)
{
	struct rb_node *n;
	struct ion_handle *handle;
	struct ion_device *dev = client->dev;
	struct ion_buffer *buffer = NULL;

	/*
	 * traverse the list of heaps available in this system in priority
	 * order.  If the heap type is supported by the client, and matches the
	 * request of the caller allocate from it.  Repeat until allocate has
	 * succeeded or all heaps have been tried
	 */
	mutex_lock(&dev->lock);
	for (n = rb_first(&dev->heaps); n != NULL; n = rb_next(n)) {
		struct ion_heap *heap = rb_entry(n, struct ion_heap, node);
		/* if the client doesn't support this heap type */
		if (!((1 << heap->type) & client->heap_mask))
			continue;
		/* if the caller didn't specify this heap type */
		if (!((1 << heap->id) & flags))
			continue;
		buffer = ion_buffer_create(heap, dev, len, align, flags);
		if (!IS_ERR_OR_NULL(buffer))
			break;
	}
	mutex_unlock(&dev->lock);

	if (IS_ERR_OR_NULL(buffer))
		return ERR_PTR(PTR_ERR(buffer));

	handle = ion_handle_create(client, buffer);

	if (IS_ERR_OR_NULL(handle))
		goto end;

	/*
	 * ion_buffer_create will create a buffer with a ref_cnt of 1,
	 * and ion_handle_create will take a second reference, drop one here
	 */
	ion_buffer_put(buffer);

	mutex_lock(&client->lock);
	ion_handle_add(client, handle);
	mutex_unlock(&client->lock);
	return handle;

end:
	ion_buffer_put(buffer);
	return handle;
}

void ion_free(struct ion_client *client, struct ion_handle *handle)
{
	bool valid_handle;

	BUG_ON(client != handle->client);

	mutex_lock(&client->lock);
	valid_handle = ion_handle_validate(client, handle);
	mutex_unlock(&client->lock);

	if (!valid_handle) {
		WARN("%s: invalid handle passed to free.\n", __func__);
		return;
	}
	ion_handle_put(handle);
}

static void ion_client_get(struct ion_client *client);
static int ion_client_put(struct ion_client *client);

static bool _ion_map(int *buffer_cnt, int *handle_cnt)
{
	bool map;

	BUG_ON(*handle_cnt != 0 && *buffer_cnt == 0);

	if (*buffer_cnt)
		map = false;
	else
		map = true;
	if (*handle_cnt == 0)
		(*buffer_cnt)++;
	(*handle_cnt)++;
	return map;
}

static bool _ion_unmap(int *buffer_cnt, int *handle_cnt)
{
	BUG_ON(*handle_cnt == 0);
	(*handle_cnt)--;
	if (*handle_cnt != 0)
		return false;
	BUG_ON(*buffer_cnt == 0);
	(*buffer_cnt)--;
	if (*buffer_cnt == 0)
		return true;
	return false;
}

int ion_phys(struct ion_client *client, struct ion_handle *handle,
	     ion_phys_addr_t *addr, size_t *len)
{
	struct ion_buffer *buffer;
	int ret;

	mutex_lock(&client->lock);
	if (!ion_handle_validate(client, handle)) {
		mutex_unlock(&client->lock);
		return -EINVAL;
	}

	buffer = handle->buffer;

	if (!buffer->heap->ops->phys) {
		pr_err("%s: ion_phys is not implemented by this heap.\n",
		       __func__);
		mutex_unlock(&client->lock);
		return -ENODEV;
	}
	mutex_unlock(&client->lock);
	ret = buffer->heap->ops->phys(buffer->heap, buffer, addr, len);
	return ret;
}

void *ion_map_kernel(struct ion_client *client, struct ion_handle *handle)
{
	struct ion_buffer *buffer;
	void *vaddr;

	mutex_lock(&client->lock);
	if (!ion_handle_validate(client, handle)) {
		pr_err("%s: invalid handle passed to map_kernel.\n",
		       __func__);
		mutex_unlock(&client->lock);
		return ERR_PTR(-EINVAL);
	}

	buffer = handle->buffer;
	mutex_lock(&buffer->lock);

	if (!handle->buffer->heap->ops->map_kernel) {
		pr_err("%s: map_kernel is not implemented by this heap.\n",
		       __func__);
		mutex_unlock(&buffer->lock);
		mutex_unlock(&client->lock);
		return ERR_PTR(-ENODEV);
	}

	if (_ion_map(&buffer->kmap_cnt, &handle->kmap_cnt)) {
		vaddr = buffer->heap->ops->map_kernel(buffer->heap, buffer);
		if (IS_ERR_OR_NULL(vaddr))
			_ion_unmap(&buffer->kmap_cnt, &handle->kmap_cnt);
		buffer->vaddr = vaddr;
	} else {
		vaddr = buffer->vaddr;
	}
	mutex_unlock(&buffer->lock);
	mutex_unlock(&client->lock);
	return vaddr;
}

struct scatterlist *ion_map_dma(struct ion_client *client,
				struct ion_handle *handle)
{
	struct ion_buffer *buffer;
	struct scatterlist *sglist;

	mutex_lock(&client->lock);
	if (!ion_handle_validate(client, handle)) {
		pr_err("%s: invalid handle passed to map_dma.\n",
		       __func__);
		mutex_unlock(&client->lock);
		return ERR_PTR(-EINVAL);
	}
	buffer = handle->buffer;
	mutex_lock(&buffer->lock);

	if (!handle->buffer->heap->ops->map_dma) {
		pr_err("%s: map_kernel is not implemented by this heap.\n",
		       __func__);
		mutex_unlock(&buffer->lock);
		mutex_unlock(&client->lock);
		return ERR_PTR(-ENODEV);
	}
	if (_ion_map(&buffer->dmap_cnt, &handle->dmap_cnt)) {
		sglist = buffer->heap->ops->map_dma(buffer->heap, buffer);
		if (IS_ERR_OR_NULL(sglist))
			_ion_unmap(&buffer->dmap_cnt, &handle->dmap_cnt);
		buffer->sglist = sglist;
	} else {
		sglist = buffer->sglist;
	}
	mutex_unlock(&buffer->lock);
	mutex_unlock(&client->lock);
	return sglist;
}

void ion_unmap_kernel(struct ion_client *client, struct ion_handle *handle)
{
	struct ion_buffer *buffer;

	mutex_lock(&client->lock);
	buffer = handle->buffer;
	mutex_lock(&buffer->lock);
	if (_ion_unmap(&buffer->kmap_cnt, &handle->kmap_cnt)) {
		buffer->heap->ops->unmap_kernel(buffer->heap, buffer);
		buffer->vaddr = NULL;
	}
	mutex_unlock(&buffer->lock);
	mutex_unlock(&client->lock);
}

void ion_unmap_dma(struct ion_client *client, struct ion_handle *handle)
{
	struct ion_buffer *buffer;

	mutex_lock(&client->lock);
	buffer = handle->buffer;
	mutex_lock(&buffer->lock);
	if (_ion_unmap(&buffer->dmap_cnt, &handle->dmap_cnt)) {
		buffer->heap->ops->unmap_dma(buffer->heap, buffer);
		buffer->sglist = NULL;
	}
	mutex_unlock(&buffer->lock);
	mutex_unlock(&client->lock);
}


struct ion_buffer *ion_share(struct ion_client *client,
				 struct ion_handle *handle)
{
	bool valid_handle;

	mutex_lock(&client->lock);
	valid_handle = ion_handle_validate(client, handle);
	mutex_unlock(&client->lock);
	if (!valid_handle) {
		WARN("%s: invalid handle passed to share.\n", __func__);
		return ERR_PTR(-EINVAL);
	}

	/* do not take an extra reference here, the burden is on the caller
	 * to make sure the buffer doesn't go away while it's passing it
	 * to another client -- ion_free should not be called on this handle
	 * until the buffer has been imported into the other client
	 */
	return handle->buffer;
}

struct ion_handle *ion_import(struct ion_client *client,
			      struct ion_buffer *buffer)
{
	struct ion_handle *handle = NULL;

	mutex_lock(&client->lock);
	/* if a handle exists for this buffer just take a reference to it */
	handle = ion_handle_lookup(client, buffer);
	if (!IS_ERR_OR_NULL(handle)) {
		ion_handle_get(handle);
		goto end;
	}
	handle = ion_handle_create(client, buffer);
	if (IS_ERR_OR_NULL(handle))
		goto end;
	ion_handle_add(client, handle);
end:
	mutex_unlock(&client->lock);
	return handle;
}

static const struct file_operations ion_share_fops;

struct ion_handle *ion_import_fd(struct ion_client *client, int fd)
{
	struct file *file = fget(fd);
	struct ion_handle *handle;

	if (!file) {
		pr_err("%s: imported fd not found in file table.\n", __func__);
		return ERR_PTR(-EINVAL);
	}
	if (file->f_op != &ion_share_fops) {
		pr_err("%s: imported file is not a shared ion file.\n",
		       __func__);
		handle = ERR_PTR(-EINVAL);
		goto end;
	}
	handle = ion_import(client, file->private_data);
end:
	fput(file);
	return handle;
}

static int ion_debug_client_show(struct seq_file *s, void *unused)
{
	struct ion_client *client = s->private;
	struct rb_node *n;
	size_t sizes[ION_NUM_HEAPS] = {0};
	const char *names[ION_NUM_HEAPS] = {0};
	int i;

	mutex_lock(&client->lock);
	for (n = rb_first(&client->handles); n; n = rb_next(n)) {
		struct ion_handle *handle = rb_entry(n, struct ion_handle,
						     node);
		enum ion_heap_type type = handle->buffer->heap->type;

		if (!names[type])
			names[type] = handle->buffer->heap->name;
		sizes[type] += handle->buffer->size;
	}
	mutex_unlock(&client->lock);

	seq_printf(s, "%16.16s: %16.16s\n", "heap_name", "size_in_bytes");
	for (i = 0; i < ION_NUM_HEAPS; i++) {
		if (!names[i])
			continue;
		seq_printf(s, "%16.16s: %16u %d\n", names[i], sizes[i],
			   atomic_read(&client->ref.refcount));
	}
	return 0;
}

static int ion_debug_client_open(struct inode *inode, struct file *file)
{
	return single_open(file, ion_debug_client_show, inode->i_private);
}

static const struct file_operations debug_client_fops = {
	.open = ion_debug_client_open,
	.read = seq_read,
	.llseek = seq_lseek,
	.release = single_release,
};

static struct ion_client *ion_client_lookup(struct ion_device *dev,
					    struct task_struct *task)
{
	struct rb_node *n = dev->user_clients.rb_node;
	struct ion_client *client;

	mutex_lock(&dev->lock);
	while (n) {
		client = rb_entry(n, struct ion_client, node);
		if (task == client->task) {
			ion_client_get(client);
			mutex_unlock(&dev->lock);
			return client;
		} else if (task < client->task) {
			n = n->rb_left;
		} else if (task > client->task) {
			n = n->rb_right;
		}
	}
	mutex_unlock(&dev->lock);
	return NULL;
}

struct ion_client *ion_client_create(struct ion_device *dev,
				     unsigned int heap_mask,
				     const char *name)
{
	struct ion_client *client;
	struct task_struct *task;
	struct rb_node **p;
	struct rb_node *parent = NULL;
	struct ion_client *entry;
	char debug_name[64];
	pid_t pid;

	get_task_struct(current->group_leader);
	task_lock(current->group_leader);
	pid = task_pid_nr(current->group_leader);
	/* don't bother to store task struct for kernel threads,
	   they can't be killed anyway */
	if (current->group_leader->flags & PF_KTHREAD) {
		//put_task_struct(current->group_leader);
		task = NULL;
	} else {
		task = current->group_leader;
	}
	task_unlock(current->group_leader);

	/* if this isn't a kernel thread, see if a client already
	   exists */
	if (task) {
		client = ion_client_lookup(dev, task);
		if (!IS_ERR_OR_NULL(client)) {
			//put_task_struct(current->group_leader);
			return client;
		}
	}

	client = kzalloc(sizeof(struct ion_client), GFP_KERNEL);
	if (!client) {
		//put_task_struct(current->group_leader);
		return ERR_PTR(-ENOMEM);
	}

	client->dev = dev;
	client->handles = RB_ROOT;
	mutex_init(&client->lock);
	client->name = name;
	client->heap_mask = heap_mask;
	client->task = task;
	client->pid = pid;
	kref_init(&client->ref);

	mutex_lock(&dev->lock);
	if (task) {
		p = &dev->user_clients.rb_node;
		while (*p) {
			parent = *p;
			entry = rb_entry(parent, struct ion_client, node);

			if (task < entry->task)
				p = &(*p)->rb_left;
			else if (task > entry->task)
				p = &(*p)->rb_right;
		}
		rb_link_node(&client->node, parent, p);
		rb_insert_color(&client->node, &dev->user_clients);
	} else {
		p = &dev->kernel_clients.rb_node;
		while (*p) {
			parent = *p;
			entry = rb_entry(parent, struct ion_client, node);

			if (client < entry)
				p = &(*p)->rb_left;
			else if (client > entry)
				p = &(*p)->rb_right;
		}
		rb_link_node(&client->node, parent, p);
		rb_insert_color(&client->node, &dev->kernel_clients);
	}

	snprintf(debug_name, 64, "%u", client->pid);
	client->debug_root = debugfs_create_file(debug_name, 0664,
						 dev->debug_root, client,
						 &debug_client_fops);
	mutex_unlock(&dev->lock);

	return client;
}

static void _ion_client_destroy(struct kref *kref)
{
	struct ion_client *client = container_of(kref, struct ion_client, ref);
	struct ion_device *dev = client->dev;
	struct rb_node *n;

	pr_debug("%s: %d\n", __func__, __LINE__);
	while ((n = rb_first(&client->handles))) {
		struct ion_handle *handle = rb_entry(n, struct ion_handle,
						     node);
		ion_handle_destroy(&handle->ref);
	}
	mutex_lock(&dev->lock);
	if (client->task) {
		rb_erase(&client->node, &dev->user_clients);
		//put_task_struct(client->task);
	} else {
		rb_erase(&client->node, &dev->kernel_clients);
	}
	debugfs_remove_recursive(client->debug_root);
	mutex_unlock(&dev->lock);

	kfree(client);
}

static void ion_client_get(struct ion_client *client)
{
	kref_get(&client->ref);
}

static int ion_client_put(struct ion_client *client)
{
	return kref_put(&client->ref, _ion_client_destroy);
}

void ion_client_destroy(struct ion_client *client)
{
	ion_client_put(client);
}

static int ion_share_release(struct inode *inode, struct file* file)
{
	struct ion_buffer *buffer = file->private_data;

	pr_debug("%s: %d\n", __func__, __LINE__);
	/* drop the reference to the buffer -- this prevents the
	   buffer from going away because the client holding it exited
	   while it was being passed */
	ion_buffer_put(buffer);
	return 0;
}

static void ion_vma_open(struct vm_area_struct *vma)
{

	struct ion_buffer *buffer = vma->vm_file->private_data;
	struct ion_handle *handle = vma->vm_private_data;
	struct ion_client *client;

	pr_debug("%s: %d\n", __func__, __LINE__);
	/* check that the client still exists and take a reference so
	   it can't go away until this vma is closed */
	client = ion_client_lookup(buffer->dev, current->group_leader);
	if (IS_ERR_OR_NULL(client)) {
		vma->vm_private_data = NULL;
		return;
	}
	pr_debug("%s: %d client_cnt %d handle_cnt %d alloc_cnt %d\n",
		 __func__, __LINE__,
		 atomic_read(&client->ref.refcount),
		 atomic_read(&handle->ref.refcount),
		 atomic_read(&buffer->ref.refcount));
}

static void ion_vma_close(struct vm_area_struct *vma)
{
	struct ion_handle *handle = vma->vm_private_data;
	struct ion_buffer *buffer = vma->vm_file->private_data;
	struct ion_client *client;

	pr_debug("%s: %d\n", __func__, __LINE__);
	/* this indicates the client is gone, nothing to do here */
	if (!handle)
		return;
	client = handle->client;
	pr_debug("%s: %d client_cnt %d handle_cnt %d alloc_cnt %d\n",
		 __func__, __LINE__,
		 atomic_read(&client->ref.refcount),
		 atomic_read(&handle->ref.refcount),
		 atomic_read(&buffer->ref.refcount));
	ion_handle_put(handle);
	ion_client_put(client);
	pr_debug("%s: %d client_cnt %d handle_cnt %d alloc_cnt %d\n",
		 __func__, __LINE__,
		 atomic_read(&client->ref.refcount),
		 atomic_read(&handle->ref.refcount),
		 atomic_read(&buffer->ref.refcount));
}

static struct vm_operations_struct ion_vm_ops = {
	.open = ion_vma_open,
	.close = ion_vma_close,
};

static int ion_share_mmap(struct file *file, struct vm_area_struct *vma)
{
	struct ion_buffer *buffer = file->private_data;
	unsigned long size = vma->vm_end - vma->vm_start;
	struct ion_client *client;
	struct ion_handle *handle;
	int ret;

	pr_debug("%s: %d\n", __func__, __LINE__);
	/* make sure the client still exists, it's possible for the client to
	   have gone away but the map/share fd still to be around, take
	   a reference to it so it can't go away while this mapping exists */
	client = ion_client_lookup(buffer->dev, current->group_leader);
	if (IS_ERR_OR_NULL(client)) {
		pr_err("%s: trying to mmap an ion handle in a process with no "
		       "ion client\n", __func__);
		return -EINVAL;
	}

	if ((size > buffer->size) || (size + (vma->vm_pgoff << PAGE_SHIFT) >
				     buffer->size)) {
		pr_err("%s: trying to map larger area than handle has available"
		       "\n", __func__);
		ret = -EINVAL;
		goto err;
	}

	/* find the handle and take a reference to it */
	handle = ion_import(client, buffer);
	if (IS_ERR_OR_NULL(handle)) {
		ret = -EINVAL;
		goto err;
	}

	if (!handle->buffer->heap->ops->map_user) {
		pr_err("%s: this heap does not define a method for mapping "
		       "to userspace\n", __func__);
		ret = -EINVAL;
		goto err1;
	}

	mutex_lock(&buffer->lock);
	/* now map it to userspace */
	ret = buffer->heap->ops->map_user(buffer->heap, buffer, vma);
	mutex_unlock(&buffer->lock);
	if (ret) {
		pr_err("%s: failure mapping buffer to userspace\n",
		       __func__);
		goto err1;
	}

	vma->vm_ops = &ion_vm_ops;
	/* move the handle into the vm_private_data so we can access it from
	   vma_open/close */
	vma->vm_private_data = handle;
	pr_debug("%s: %d client_cnt %d handle_cnt %d alloc_cnt %d\n",
		 __func__, __LINE__,
		 atomic_read(&client->ref.refcount),
		 atomic_read(&handle->ref.refcount),
		 atomic_read(&buffer->ref.refcount));
	return 0;

err1:
	/* drop the reference to the handle */
	ion_handle_put(handle);
err:
	/* drop the reference to the client */
	ion_client_put(client);
	return ret;
}

static const struct file_operations ion_share_fops = {
	.owner		= THIS_MODULE,
	.release	= ion_share_release,
	.mmap		= ion_share_mmap,
};

static int ion_ioctl_share(struct file *parent, struct ion_client *client,
			   struct ion_handle *handle)
{
	int fd = get_unused_fd();
	struct file *file;

	if (fd < 0)
		return -ENFILE;

	file = anon_inode_getfile("ion_share_fd", &ion_share_fops,
				  handle->buffer, O_RDWR);
	if (IS_ERR_OR_NULL(file))
		goto err;
	ion_buffer_get(handle->buffer);
	fd_install(fd, file);

	return fd;

err:
	put_unused_fd(fd);
	return -ENFILE;
}

static long ion_ioctl(struct file *filp, unsigned int cmd, unsigned long arg)
{
	struct ion_client *client = filp->private_data;

	switch (cmd) {
	case ION_IOC_ALLOC:
	{
		struct ion_allocation_data data;

		if (copy_from_user(&data, (void __user *)arg, sizeof(data)))
			return -EFAULT;
		data.handle = ion_alloc(client, data.len, data.align,
					     data.flags);
		if (copy_to_user((void __user *)arg, &data, sizeof(data)))
			return -EFAULT;
		break;
	}
	case ION_IOC_FREE:
	{
		struct ion_handle_data data;
		bool valid;

		if (copy_from_user(&data, (void __user *)arg,
				   sizeof(struct ion_handle_data)))
			return -EFAULT;
		mutex_lock(&client->lock);
		valid = ion_handle_validate(client, data.handle);
		mutex_unlock(&client->lock);
		if (!valid)
			return -EINVAL;
		ion_free(client, data.handle);
		break;
	}
	case ION_IOC_MAP:
	case ION_IOC_SHARE:
	{
		struct ion_fd_data data;

		if (copy_from_user(&data, (void __user *)arg, sizeof(data)))
			return -EFAULT;
		mutex_lock(&client->lock);
		if (!ion_handle_validate(client, data.handle)) {
			pr_err("%s: invalid handle passed to share ioctl.\n",
			       __func__);
			mutex_unlock(&client->lock);
			return -EINVAL;
		}
		data.fd = ion_ioctl_share(filp, client, data.handle);
		mutex_unlock(&client->lock);
		if (copy_to_user((void __user *)arg, &data, sizeof(data)))
			return -EFAULT;
		break;
	}
	case ION_IOC_IMPORT:
	{
		struct ion_fd_data data;
		if (copy_from_user(&data, (void __user *)arg,
				   sizeof(struct ion_fd_data)))
			return -EFAULT;

		data.handle = ion_import_fd(client, data.fd);
		if (IS_ERR(data.handle))
			data.handle = NULL;
		if (copy_to_user((void __user *)arg, &data,
				 sizeof(struct ion_fd_data)))
			return -EFAULT;
		break;
	}
	case ION_IOC_CUSTOM:
	{
		struct ion_device *dev = client->dev;
		struct ion_custom_data data;

		if (!dev->custom_ioctl)
			return -ENOTTY;
		if (copy_from_user(&data, (void __user *)arg,
				sizeof(struct ion_custom_data)))
			return -EFAULT;
		return dev->custom_ioctl(client, data.cmd, data.arg);
	}
	default:
		return -ENOTTY;
	}
	return 0;
}

static int ion_release(struct inode *inode, struct file *file)
{
	struct ion_client *client = file->private_data;

	pr_debug("%s: %d\n", __func__, __LINE__);
	ion_client_put(client);
	return 0;
}

static int ion_open(struct inode *inode, struct file *file)
{
	struct miscdevice *miscdev = file->private_data;
	struct ion_device *dev = container_of(miscdev, struct ion_device, dev);
	struct ion_client *client;

	pr_debug("%s: %d\n", __func__, __LINE__);
	client = ion_client_create(dev, -1, "user");
	if (IS_ERR_OR_NULL(client))
		return PTR_ERR(client);
	file->private_data = client;

	return 0;
}

static const struct file_operations ion_fops = {
	.owner          = THIS_MODULE,
	.open           = ion_open,
	.release        = ion_release,
	.unlocked_ioctl = ion_ioctl,
};

static size_t ion_debug_heap_total(struct ion_client *client,
				   enum ion_heap_type type)
{
	size_t size = 0;
	struct rb_node *n;

	mutex_lock(&client->lock);
	for (n = rb_first(&client->handles); n; n = rb_next(n)) {
		struct ion_handle *handle = rb_entry(n,
						     struct ion_handle,
						     node);
		if (handle->buffer->heap->type == type)
			size += handle->buffer->size;
	}
	mutex_unlock(&client->lock);
	return size;
}

static int ion_debug_heap_show(struct seq_file *s, void *unused)
{
	struct ion_heap *heap = s->private;
	struct ion_device *dev = heap->dev;
	struct rb_node *n;

	seq_printf(s, "%16.s %16.s %16.s\n", "client", "pid", "size");
	for (n = rb_first(&dev->user_clients); n; n = rb_next(n)) {
		struct ion_client *client = rb_entry(n, struct ion_client,
						     node);
		//char task_comm[TASK_COMM_LEN];
		size_t size = ion_debug_heap_total(client, heap->type);
		if (!size)
			continue;

		//get_task_comm(task_comm, client->task);
		//seq_printf(s, "%16.s %16u %16u\n", task_comm, client->pid,
		//	   size);
	}

	for (n = rb_first(&dev->kernel_clients); n; n = rb_next(n)) {
		struct ion_client *client = rb_entry(n, struct ion_client,
						     node);
		size_t size = ion_debug_heap_total(client, heap->type);
		if (!size)
			continue;
		seq_printf(s, "%16.s %16u %16u\n", client->name, client->pid,
			   size);
	}
	return 0;
}

static int ion_debug_heap_open(struct inode *inode, struct file *file)
{
	return single_open(file, ion_debug_heap_show, inode->i_private);
}

static const struct file_operations debug_heap_fops = {
	.open = ion_debug_heap_open,
	.read = seq_read,
	.llseek = seq_lseek,
	.release = single_release,
};

void ion_device_add_heap(struct ion_device *dev, struct ion_heap *heap)
{
	struct rb_node **p = &dev->heaps.rb_node;
	struct rb_node *parent = NULL;
	struct ion_heap *entry;

	heap->dev = dev;
	mutex_lock(&dev->lock);
	while (*p) {
		parent = *p;
		entry = rb_entry(parent, struct ion_heap, node);

		if (heap->id < entry->id) {
			p = &(*p)->rb_left;
		} else if (heap->id > entry->id ) {
			p = &(*p)->rb_right;
		} else {
			pr_err("%s: can not insert multiple heaps with "
				"id %d\n", __func__, heap->id);
			goto end;
		}
	}

	rb_link_node(&heap->node, parent, p);
	rb_insert_color(&heap->node, &dev->heaps);
	debugfs_create_file(heap->name, 0664, dev->debug_root, heap,
			    &debug_heap_fops);
end:
	mutex_unlock(&dev->lock);
}

struct ion_device *ion_device_create(long (*custom_ioctl)
				     (struct ion_client *client,
				      unsigned int cmd,
				      unsigned long arg))
{
	struct ion_device *idev;
	int ret;

	idev = kzalloc(sizeof(struct ion_device), GFP_KERNEL);
	if (!idev)
		return ERR_PTR(-ENOMEM);

	idev->dev.minor = MISC_DYNAMIC_MINOR;
	idev->dev.name = "ion";
	idev->dev.fops = &ion_fops;
	idev->dev.parent = NULL;
	ret = misc_register(&idev->dev);
	if (ret) {
		pr_err("ion: failed to register misc device.\n");
		return ERR_PTR(ret);
	}

	idev->debug_root = debugfs_create_dir("ion", NULL);
	if (IS_ERR_OR_NULL(idev->debug_root))
		pr_err("ion: failed to create debug files.\n");

	idev->custom_ioctl = custom_ioctl;
	idev->buffers = RB_ROOT;
	mutex_init(&idev->lock);
	idev->heaps = RB_ROOT;
	idev->user_clients = RB_ROOT;
	idev->kernel_clients = RB_ROOT;
	return idev;
}

void ion_device_destroy(struct ion_device *dev)
{
	misc_deregister(&dev->dev);
	/* XXX need to free the heaps and clients ? */
	kfree(dev);
}



/***********************************
** omap_tiler_heap.c
*/

static int omap_tiler_heap_allocate(struct ion_heap *heap,
				    struct ion_buffer *buffer,
				    unsigned long size, unsigned long align,
				    unsigned long flags)
{
	if (size == 0)
		return 0;

	pr_err("%s: This should never be called directly -- use the "
	       "OMAP_ION_TILER_ALLOC flag to the ION_IOC_CUSTOM "
	       "instead\n", __func__);
	return -EINVAL;
}

struct omap_tiler_info {
	tiler_blk_handle tiler_handle;	/* handle of the allocation intiler */
	u32 n_phys_pages;		/* number of physical pages */
	u32 *phys_addrs;		/* array addrs of pages */
	u32 n_tiler_pages;		/* number of tiler pages */
	u32 *tiler_addrs;		/* array of addrs of tiler pages */
	u32 tiler_start;		/* start addr in tiler -- if not page
					   aligned this may not equal the
					   first entry onf tiler_addrs */
};

int omap_tiler_alloc(struct ion_heap *heap,
		     struct ion_client *client,
		     struct omap_ion_tiler_alloc_data *data)
{
	struct ion_handle *handle;
	struct ion_buffer *buffer;
	struct omap_tiler_info *info;
	int i, ret;

	if (data->fmt == TILER_PIXEL_FMT_PAGE && data->h != 1) {
		pr_err("%s: Page mode (1D) allocations must have a height "
		       "of one\n", __func__);
		return -EINVAL;
	}

	info = kzalloc(sizeof(struct omap_tiler_info), GFP_KERNEL);
	if (!info)
		return -ENOMEM;

	ret = tiler2_memsize(data->fmt, data->w, data->h, &info->n_phys_pages,
			    &info->n_tiler_pages);

	if (ret) {
		pr_err("%s: invalid tiler request w %u h %u fmt %u\n", __func__,
		       data->w, data->h, data->fmt);
		kfree(info);
		return ret;
	}

	BUG_ON(!info->n_phys_pages || !info->n_tiler_pages);

	info->phys_addrs = kzalloc(sizeof(u32) * info->n_phys_pages,
				   GFP_KERNEL);
	if (!info->phys_addrs) {
		ret = -ENOMEM;
		goto err_alloc_addrs;
	}

	info->tiler_addrs = kzalloc(sizeof(u32) * info->n_tiler_pages,
				   GFP_KERNEL);
	if (!info->tiler_addrs) {
		ret = -ENOMEM;
		goto err_tiler_addrs;
	}

	info->tiler_handle = tiler2_alloc_block_area(data->fmt, data->w, data->h,
						    &info->tiler_start,
						    info->tiler_addrs);
	if (IS_ERR_OR_NULL(info->tiler_handle)) {
		ret = PTR_ERR(info->tiler_handle);
		pr_err("%s: failure to allocate address space from tiler\n",
		       __func__);
		goto err_tiler_block_alloc;
	}

	for (i = 0; i < info->n_phys_pages; i++) {
		ion_phys_addr_t addr = ion_carveout_allocate(heap, PAGE_SIZE,
							     0);

		if (addr == ION_CARVEOUT_ALLOCATE_FAIL) {
			ret = -ENOMEM;
			goto err_alloc;
			pr_err("%s: failure to pages to back tiler "
			       "address space\n", __func__);
		}
		info->phys_addrs[i] = addr;
	}

	ret = tiler2_pin_block(info->tiler_handle, info->phys_addrs,
			      info->n_phys_pages);
	if (ret) {
		pr_err("%s: failure to pin pages to tiler\n", __func__);
		goto err_alloc;
	}

	data->stride = tiler2_block_vstride(info->tiler_handle);

	/* create an ion handle  for the allocation */
	handle = ion_alloc(client, 0, 0, 1 << OMAP_ION_HEAP_TILER);
	if (IS_ERR_OR_NULL(handle)) {
		ret = PTR_ERR(handle);
		pr_err("%s: failure to allocate handle to manage tiler"
		       " allocation\n", __func__);
		goto err;
	}

	buffer = ion_handle_buffer(handle);
	buffer->size = info->n_tiler_pages * PAGE_SIZE;
	buffer->priv_virt = info;
	data->handle = handle;
	return 0;

err:
	tiler2_unpin_block(info->tiler_handle);
err_alloc:
	tiler2_free_block_area(info->tiler_handle);
	for (i -= 1; i >= 0; i--)
		ion_carveout_free(heap, info->phys_addrs[i], PAGE_SIZE);
err_tiler_block_alloc:
	kfree(info->tiler_addrs);
err_tiler_addrs:
	kfree(info->phys_addrs);
err_alloc_addrs:
	kfree(info);
	return ret;
}

void omap_tiler_heap_free(struct ion_buffer *buffer)
{
	struct omap_tiler_info *info = buffer->priv_virt;
	int i;

	tiler2_unpin_block(info->tiler_handle);
	tiler2_free_block_area(info->tiler_handle);

	for (i = 0; i < info->n_phys_pages; i++)
		ion_carveout_free(buffer->heap, info->phys_addrs[i], PAGE_SIZE);

	kfree(info->tiler_addrs);
	kfree(info->phys_addrs);
	kfree(info);
}

static int omap_tiler_phys(struct ion_heap *heap,
			   struct ion_buffer *buffer,
			   ion_phys_addr_t *addr, size_t *len)
{
	struct omap_tiler_info *info = buffer->priv_virt;

	*addr = info->tiler_start;
	*len = buffer->size;
	return 0;
}

int omap_tiler_pages(struct ion_client *client, struct ion_handle *handle,
		     int *n, u32 **tiler_addrs)
{
	ion_phys_addr_t addr;
	size_t len;
	int ret;
	struct omap_tiler_info *info = ion_handle_buffer(handle)->priv_virt;

	/* validate that the handle exists in this client */
	ret = ion_phys(client, handle, &addr, &len);
	if (ret)
		return ret;

	*n = info->n_tiler_pages;
	*tiler_addrs = info->tiler_addrs;
	return 0;
}

int omap_tiler_heap_map_user(struct ion_heap *heap, struct ion_buffer *buffer,
			     struct vm_area_struct *vma)
{
	struct omap_tiler_info *info = buffer->priv_virt;
	unsigned long addr = vma->vm_start;
	u32 vma_pages = (vma->vm_end - vma->vm_start) / PAGE_SIZE;
	int n_pages = min(vma_pages, info->n_tiler_pages);
	int i, ret;

	for (i = vma->vm_pgoff; i < n_pages; i++, addr += PAGE_SIZE) {
		ret = remap_pfn_range(vma, addr,
				      __phys_to_pfn(info->tiler_addrs[i]),
				      PAGE_SIZE,
				      pgprot_noncached(vma->vm_page_prot));
		if (ret)
			return ret;
	}
	return 0;
}

static struct ion_heap_ops omap_tiler_ops = {
	.allocate = omap_tiler_heap_allocate,
	.free = omap_tiler_heap_free,
	.phys = omap_tiler_phys,
	.map_user = omap_tiler_heap_map_user,
};

struct ion_heap *omap_tiler_heap_create(struct ion_platform_heap *data)
{
	struct ion_heap *heap;

	heap = ion_carveout_heap_create(data);
	if (!heap)
		return ERR_PTR(-ENOMEM);
	heap->ops = &omap_tiler_ops;
	heap->type = OMAP_ION_HEAP_TYPE_TILER;
	heap->name = data->name;
	heap->id = data->id;
	return heap;
}

void omap_tiler_heap_destroy(struct ion_heap *heap)
{
	kfree(heap);
}


/***********************************
** omap_ion.c
*/

struct ion_device *omap_ion_device;
int num_heaps;
struct ion_heap **heaps;
struct ion_heap *tiler_heap;

int omap_ion_tiler_alloc(struct ion_client *client,
			 struct omap_ion_tiler_alloc_data *data)
{
	return omap_tiler_alloc(tiler_heap, client, data);
}

long omap_ion_ioctl(struct ion_client *client, unsigned int cmd,
		    unsigned long arg)
{
	printk(KERN_WARNING "omap_ion_ioctl");
	switch (cmd) {
	case OMAP_ION_TILER_ALLOC:
	{
		struct omap_ion_tiler_alloc_data data;
		int ret;

		if (!tiler_heap) {
			pr_err("%s: Tiler heap requested but no tiler "
			       "heap exists on this platform\n", __func__);
			return -EINVAL;
		}
		if (copy_from_user(&data, (void __user *)arg, sizeof(data)))
			return -EFAULT;
		ret = omap_ion_tiler_alloc(client, &data);
		if (ret)
			return ret;
		if (copy_to_user((void __user *)arg, &data,
				 sizeof(data)))
			return -EFAULT;
		break;
	}
	default:
		pr_err("%s: Unknown custom ioctl\n", __func__);
		return -ENOTTY;
	}
	return 0;
}

int omap_ion_probe(struct platform_device *pdev)
{
	struct ion_platform_data *pdata = pdev->dev.platform_data;
	int err;
	int i;

	printk(KERN_WARNING "omap_ion_probe");

	num_heaps = pdata->nr;

	heaps = kzalloc(sizeof(struct ion_heap *) * pdata->nr, GFP_KERNEL);

	omap_ion_device = ion_device_create(omap_ion_ioctl);
	if (IS_ERR_OR_NULL(omap_ion_device)) {
		kfree(heaps);
		return PTR_ERR(omap_ion_device);
	}

	/* create the heaps as specified in the board file */
	for (i = 0; i < num_heaps; i++) {
		struct ion_platform_heap *heap_data = &pdata->heaps[i];

		if (heap_data->type == OMAP_ION_HEAP_TYPE_TILER) {
			heaps[i] = omap_tiler_heap_create(heap_data);
			tiler_heap = heaps[i];
		} else {
			heaps[i] = ion_heap_create(heap_data);
		}
		if (IS_ERR_OR_NULL(heaps[i])) {
			err = PTR_ERR(heaps[i]);
			goto err;
		}
		ion_device_add_heap(omap_ion_device, heaps[i]);
		pr_info("%s: adding heap %s of type %d with %lx@%x\n",
			__func__, heap_data->name, heap_data->type,
			heap_data->base, heap_data->size);

	}

	platform_set_drvdata(pdev, omap_ion_device);
	return 0;
err:
	for (i = 0; i < num_heaps; i++) {
		if (heaps[i]) {
			if (heaps[i]->type == OMAP_ION_HEAP_TYPE_TILER)
				omap_tiler_heap_destroy(heaps[i]);
			else
				ion_heap_destroy(heaps[i]);
		}
	}
	kfree(heaps);
	return err;
}

int omap_ion_remove(struct platform_device *pdev)
{
	struct ion_device *idev = platform_get_drvdata(pdev);
	int i;

	printk(KERN_WARNING "omap_ion_remove");
	ion_device_destroy(idev);
	for (i = 0; i < num_heaps; i++)
		if (heaps[i]->type == OMAP_ION_HEAP_TYPE_TILER)
			omap_tiler_heap_destroy(heaps[i]);
		else
			ion_heap_destroy(heaps[i]);
	kfree(heaps);
	return 0;
}

//      .owner = THIS_MODULE,
static struct platform_driver ion_driver = {
	.probe = omap_ion_probe,
	.remove = omap_ion_remove,
	.driver = { .name = "ion-omap4" }
};

static int __init ion_init(void)
{
	printk(KERN_WARNING "ion_init");
	return platform_driver_register(&ion_driver);
}

static void __exit ion_exit(void)
{
	platform_driver_unregister(&ion_driver);
}

MODULE_LICENSE("GPL v2");
MODULE_AUTHOR("Google 2011");
module_init(ion_init);
module_exit(ion_exit);

