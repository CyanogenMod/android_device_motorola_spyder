package com.motorola.android.server.ims;

import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.util.Log;
import com.motorola.android.ims.IIMSCSms.Stub;
import com.motorola.android.ims.IIMSCSmsCallback;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.WeakHashMap;

public class IMSCSmsService extends IIMSCSms.Stub
  implements IMSCNative.IMSCSmsCallback
{
  private static final String TAG = "IMSCSmsService";
  private static IMSCSmsService mSingletonInstance;
  private Vector<IIMSCSmsCallback> mCallbacks = new Vector();
  private Map<IBinder, CallbackDeathRecipient> mRecipients = new WeakHashMap();
  private int mReplySeq = 1;
  private Vector<IMSCSmsMoEvent> mSmsMoEvents = new Vector();
  private Vector<IMSCSmsMtEvent> mSmsMtEvents = new Vector();

  /** @deprecated */
  public static IMSCSmsService getInstance()
  {
    monitorenter;
    try
    {
      if (mSingletonInstance == null)
        mSingletonInstance = new IMSCSmsService();
      IMSCSmsService localIMSCSmsService = mSingletonInstance;
      monitorexit;
      return localIMSCSmsService;
    }
    finally
    {
      localObject = finally;
      monitorexit;
    }
    throw localObject;
  }

  /** @deprecated */
  private int getNextReplySeq()
  {
    monitorenter;
    try
    {
      int i = this.mReplySeq;
      this.mReplySeq = (i + 1);
      monitorexit;
      return i;
    }
    finally
    {
      localObject = finally;
      monitorexit;
    }
    throw localObject;
  }

  private int registerCallback(long paramLong, IIMSCSmsCallback paramIIMSCSmsCallback)
  {
    Log.i("IMSCSmsService", "registerCallback, regId: " + paramLong + ", callback: " + paramIIMSCSmsCallback);
    int i;
    if ((0L == paramLong) || (paramIIMSCSmsCallback == null))
      i = -1;
    while (true)
    {
      return i;
      IMSCNative localIMSCNative = IMSCNative.getInstance();
      if (localIMSCNative == null)
      {
        i = -1;
        continue;
      }
      synchronized (this.mCallbacks)
      {
        try
        {
          CallbackDeathRecipient localCallbackDeathRecipient = new CallbackDeathRecipient(paramIIMSCSmsCallback, paramLong);
          paramIIMSCSmsCallback.asBinder().linkToDeath(localCallbackDeathRecipient, 0);
          this.mRecipients.put(paramIIMSCSmsCallback.asBinder(), localCallbackDeathRecipient);
          this.mCallbacks.add(paramIIMSCSmsCallback);
          localIMSCNative.imscSetSmsCallback(paramLong, this);
          i = 0;
        }
        catch (RemoteException localRemoteException)
        {
          Log.e("IMSCSmsService", "registerCallback: callback already gone, exception: " + localRemoteException);
          i = -1;
        }
      }
    }
  }

  // ERROR //
  private int unRegisterCallback(long paramLong, IIMSCSmsCallback paramIIMSCSmsCallback)
  {
    // Byte code:
    //   0: ldc 14
    //   2: new 66	java/lang/StringBuilder
    //   5: dup
    //   6: invokespecial 67	java/lang/StringBuilder:<init>	()V
    //   9: ldc 136
    //   11: invokevirtual 73	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   14: lload_1
    //   15: invokevirtual 76	java/lang/StringBuilder:append	(J)Ljava/lang/StringBuilder;
    //   18: ldc 78
    //   20: invokevirtual 73	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   23: aload_3
    //   24: invokevirtual 81	java/lang/StringBuilder:append	(Ljava/lang/Object;)Ljava/lang/StringBuilder;
    //   27: invokevirtual 85	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   30: invokestatic 91	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   33: pop
    //   34: ldc2_w 92
    //   37: lload_1
    //   38: lcmp
    //   39: ifeq +7 -> 46
    //   42: aload_3
    //   43: ifnonnull +10 -> 53
    //   46: bipush 255
    //   48: istore 5
    //   50: iload 5
    //   52: ireturn
    //   53: invokestatic 98	com/motorola/android/server/ims/IMSCNative:getInstance	()Lcom/motorola/android/server/ims/IMSCNative;
    //   56: astore 6
    //   58: aload 6
    //   60: ifnonnull +10 -> 70
    //   63: bipush 255
    //   65: istore 5
    //   67: goto -17 -> 50
    //   70: aload_0
    //   71: getfield 41	com/motorola/android/server/ims/IMSCSmsService:mCallbacks	Ljava/util/Vector;
    //   74: astore 7
    //   76: aload 7
    //   78: monitorenter
    //   79: ldc 14
    //   81: new 66	java/lang/StringBuilder
    //   84: dup
    //   85: invokespecial 67	java/lang/StringBuilder:<init>	()V
    //   88: ldc 138
    //   90: invokevirtual 73	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   93: aload_0
    //   94: getfield 41	com/motorola/android/server/ims/IMSCSmsService:mCallbacks	Ljava/util/Vector;
    //   97: invokevirtual 141	java/util/Vector:size	()I
    //   100: invokevirtual 144	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
    //   103: ldc 146
    //   105: invokevirtual 73	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   108: aload_0
    //   109: getfield 46	com/motorola/android/server/ims/IMSCSmsService:mRecipients	Ljava/util/Map;
    //   112: invokeinterface 147 1 0
    //   117: invokevirtual 144	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
    //   120: invokevirtual 85	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   123: invokestatic 150	android/util/Log:d	(Ljava/lang/String;Ljava/lang/String;)I
    //   126: pop
    //   127: aload_0
    //   128: getfield 41	com/motorola/android/server/ims/IMSCSmsService:mCallbacks	Ljava/util/Vector;
    //   131: invokevirtual 154	java/util/Vector:iterator	()Ljava/util/Iterator;
    //   134: astore 10
    //   136: aload 10
    //   138: invokeinterface 160 1 0
    //   143: ifeq +121 -> 264
    //   146: aload 10
    //   148: invokeinterface 164 1 0
    //   153: checkcast 103	com/motorola/android/ims/IIMSCSmsCallback
    //   156: invokeinterface 107 1 0
    //   161: astore 19
    //   163: aload 19
    //   165: aload_3
    //   166: invokeinterface 107 1 0
    //   171: invokevirtual 169	java/lang/Object:equals	(Ljava/lang/Object;)Z
    //   174: ifeq -38 -> 136
    //   177: aload_0
    //   178: getfield 46	com/motorola/android/server/ims/IMSCSmsService:mRecipients	Ljava/util/Map;
    //   181: aload 19
    //   183: invokeinterface 173 2 0
    //   188: checkcast 8	com/motorola/android/server/ims/IMSCSmsService$CallbackDeathRecipient
    //   191: astore 20
    //   193: aload 20
    //   195: ifnull +263 -> 458
    //   198: aload_0
    //   199: getfield 46	com/motorola/android/server/ims/IMSCSmsService:mRecipients	Ljava/util/Map;
    //   202: aload 19
    //   204: invokeinterface 176 2 0
    //   209: pop
    //   210: aload 19
    //   212: aload 20
    //   214: iconst_0
    //   215: invokeinterface 180 3 0
    //   220: pop
    //   221: ldc 14
    //   223: new 66	java/lang/StringBuilder
    //   226: dup
    //   227: invokespecial 67	java/lang/StringBuilder:<init>	()V
    //   230: ldc 182
    //   232: invokevirtual 73	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   235: aload 19
    //   237: invokevirtual 81	java/lang/StringBuilder:append	(Ljava/lang/Object;)Ljava/lang/StringBuilder;
    //   240: ldc 184
    //   242: invokevirtual 73	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   245: aload 20
    //   247: invokevirtual 81	java/lang/StringBuilder:append	(Ljava/lang/Object;)Ljava/lang/StringBuilder;
    //   250: invokevirtual 85	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   253: invokestatic 150	android/util/Log:d	(Ljava/lang/String;Ljava/lang/String;)I
    //   256: pop
    //   257: aload 10
    //   259: invokeinterface 186 1 0
    //   264: ldc 14
    //   266: new 66	java/lang/StringBuilder
    //   269: dup
    //   270: invokespecial 67	java/lang/StringBuilder:<init>	()V
    //   273: ldc 188
    //   275: invokevirtual 73	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   278: aload_0
    //   279: getfield 41	com/motorola/android/server/ims/IMSCSmsService:mCallbacks	Ljava/util/Vector;
    //   282: invokevirtual 141	java/util/Vector:size	()I
    //   285: invokevirtual 144	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
    //   288: ldc 146
    //   290: invokevirtual 73	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   293: aload_0
    //   294: getfield 46	com/motorola/android/server/ims/IMSCSmsService:mRecipients	Ljava/util/Map;
    //   297: invokeinterface 147 1 0
    //   302: invokevirtual 144	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
    //   305: invokevirtual 85	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   308: invokestatic 150	android/util/Log:d	(Ljava/lang/String;Ljava/lang/String;)I
    //   311: pop
    //   312: aload 7
    //   314: monitorexit
    //   315: iconst_0
    //   316: istore 12
    //   318: aload_0
    //   319: getfield 41	com/motorola/android/server/ims/IMSCSmsService:mCallbacks	Ljava/util/Vector;
    //   322: astore 13
    //   324: aload 13
    //   326: monitorenter
    //   327: aload_0
    //   328: getfield 41	com/motorola/android/server/ims/IMSCSmsService:mCallbacks	Ljava/util/Vector;
    //   331: invokevirtual 154	java/util/Vector:iterator	()Ljava/util/Iterator;
    //   334: astore 15
    //   336: aload 15
    //   338: invokeinterface 160 1 0
    //   343: ifeq +54 -> 397
    //   346: aload 15
    //   348: invokeinterface 164 1 0
    //   353: checkcast 103	com/motorola/android/ims/IIMSCSmsCallback
    //   356: invokeinterface 107 1 0
    //   361: astore 17
    //   363: aload_0
    //   364: getfield 46	com/motorola/android/server/ims/IMSCSmsService:mRecipients	Ljava/util/Map;
    //   367: aload 17
    //   369: invokeinterface 173 2 0
    //   374: checkcast 8	com/motorola/android/server/ims/IMSCSmsService$CallbackDeathRecipient
    //   377: astore 18
    //   379: aload 18
    //   381: ifnull -45 -> 336
    //   384: lload_1
    //   385: aload 18
    //   387: invokevirtual 192	com/motorola/android/server/ims/IMSCSmsService$CallbackDeathRecipient:getRegId	()J
    //   390: lcmp
    //   391: ifne -55 -> 336
    //   394: iconst_1
    //   395: istore 12
    //   397: aload 13
    //   399: monitorexit
    //   400: iload 12
    //   402: ifne +11 -> 413
    //   405: aload 6
    //   407: lload_1
    //   408: aconst_null
    //   409: invokevirtual 127	com/motorola/android/server/ims/IMSCNative:imscSetSmsCallback	(JLcom/motorola/android/server/ims/IMSCNative$IMSCSmsCallback;)Z
    //   412: pop
    //   413: iconst_0
    //   414: istore 5
    //   416: goto -366 -> 50
    //   419: astore 22
    //   421: ldc 14
    //   423: new 66	java/lang/StringBuilder
    //   426: dup
    //   427: invokespecial 67	java/lang/StringBuilder:<init>	()V
    //   430: ldc 194
    //   432: invokevirtual 73	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   435: aload 22
    //   437: invokevirtual 81	java/lang/StringBuilder:append	(Ljava/lang/Object;)Ljava/lang/StringBuilder;
    //   440: invokevirtual 85	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   443: invokestatic 197	android/util/Log:w	(Ljava/lang/String;Ljava/lang/String;)I
    //   446: pop
    //   447: goto -190 -> 257
    //   450: astore 8
    //   452: aload 7
    //   454: monitorexit
    //   455: aload 8
    //   457: athrow
    //   458: ldc 14
    //   460: new 66	java/lang/StringBuilder
    //   463: dup
    //   464: invokespecial 67	java/lang/StringBuilder:<init>	()V
    //   467: ldc 199
    //   469: invokevirtual 73	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   472: aload_3
    //   473: invokevirtual 81	java/lang/StringBuilder:append	(Ljava/lang/Object;)Ljava/lang/StringBuilder;
    //   476: ldc 201
    //   478: invokevirtual 73	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   481: aload 19
    //   483: invokevirtual 81	java/lang/StringBuilder:append	(Ljava/lang/Object;)Ljava/lang/StringBuilder;
    //   486: invokevirtual 85	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   489: invokestatic 197	android/util/Log:w	(Ljava/lang/String;Ljava/lang/String;)I
    //   492: pop
    //   493: goto -236 -> 257
    //   496: astore 14
    //   498: aload 13
    //   500: monitorexit
    //   501: aload 14
    //   503: athrow
    //
    // Exception table:
    //   from	to	target	type
    //   198	257	419	java/util/NoSuchElementException
    //   79	193	450	finally
    //   198	257	450	finally
    //   257	315	450	finally
    //   421	455	450	finally
    //   458	493	450	finally
    //   327	400	496	finally
    //   498	501	496	finally
  }

  public boolean cancelMessage(long paramLong1, long paramLong2)
  {
    int i = 0;
    while (true)
    {
      synchronized (this.mSmsMoEvents)
      {
        Iterator localIterator = this.mSmsMoEvents.iterator();
        if (!localIterator.hasNext())
          continue;
        IMSCSmsMoEvent localIMSCSmsMoEvent = (IMSCSmsMoEvent)localIterator.next();
        if ((paramLong1 != localIMSCSmsMoEvent.getRegId()) || (paramLong2 != localIMSCSmsMoEvent.getMsgId()))
          continue;
        localIterator.remove();
        i = 1;
        if (i == 0)
        {
          Log.e("IMSCSmsService", "No matched msg is found in mSmsMoEvents.");
          j = 0;
          return j;
        }
      }
      boolean bool = false;
      IMSCNative localIMSCNative = IMSCNative.getInstance();
      if (localIMSCNative != null)
        bool = localIMSCNative.imscSmsCancelMsg(paramLong1, paramLong2);
      int j = bool;
    }
  }

  public Vector<IIMSCSmsCallback> getCallbacks()
  {
    return this.mCallbacks;
  }

  public IBinder getIMSCSmsService()
  {
    return asBinder();
  }

  public int getSmsProtocolType()
  {
    int i = 0;
    IMSCConfReader localIMSCConfReader = IMSCConfReader.getInstance();
    if (localIMSCConfReader != null)
      i = localIMSCConfReader.getSmsFormat();
    Log.i("IMSCSmsService", "IMSCConfReader.getSmsFormat: " + i);
    int j = 0;
    if (i == 0)
      j = 0;
    while (true)
    {
      return j;
      if (i == 1)
      {
        j = 1;
        continue;
      }
      if (i != 2)
        continue;
      j = 2;
    }
  }

  public void onAirplaneModeChanged(boolean paramBoolean)
  {
    Log.i("IMSCSmsService", "onAirplaneModeChanged: " + paramBoolean);
  }

  public void onShutdown()
  {
    Log.i("IMSCSmsService", "onShutdown");
  }

  public void onSmsMoCallback(IMSCSmsMoEvent paramIMSCSmsMoEvent)
  {
    long l1 = paramIMSCSmsMoEvent.getRegId();
    int i = paramIMSCSmsMoEvent.getReplySeq();
    int j = paramIMSCSmsMoEvent.getStatusCode();
    byte[] arrayOfByte = paramIMSCSmsMoEvent.getSmsBody();
    long l2 = 0L;
    synchronized (this.mSmsMoEvents)
    {
      Iterator localIterator1 = this.mSmsMoEvents.iterator();
      while (localIterator1.hasNext())
      {
        IMSCSmsMoEvent localIMSCSmsMoEvent = (IMSCSmsMoEvent)localIterator1.next();
        if ((l1 != localIMSCSmsMoEvent.getRegId()) || (i != localIMSCSmsMoEvent.getReplySeq()))
          continue;
        l2 = localIMSCSmsMoEvent.getMsgId();
        localIterator1.remove();
      }
      if (l2 != 0L);
    }
    Log.i("IMSCSmsService", "onSmsMoCallback: " + paramIMSCSmsMoEvent);
    synchronized (this.mCallbacks)
    {
      try
      {
        Iterator localIterator2 = this.mCallbacks.iterator();
        while (localIterator2.hasNext())
          ((IIMSCSmsCallback)localIterator2.next()).onMessageResponse(l1, l2, j, arrayOfByte);
      }
      catch (RemoteException localRemoteException)
      {
        Log.e("IMSCSmsService", "onSmsMoCallback exception: " + localRemoteException);
      }
    }
  }

  public void onSmsMtCallback(IMSCSmsMtEvent paramIMSCSmsMtEvent)
  {
    long l1 = paramIMSCSmsMtEvent.getRegId();
    long l2 = paramIMSCSmsMtEvent.getMsgId();
    String str = paramIMSCSmsMtEvent.getFromAddress();
    int i = paramIMSCSmsMtEvent.getMsgFormat();
    byte[] arrayOfByte = paramIMSCSmsMtEvent.getSmsBody();
    int j;
    if (l2 != 0L)
      j = 0;
    synchronized (this.mSmsMtEvents)
    {
      Iterator localIterator2 = this.mSmsMtEvents.iterator();
      while (localIterator2.hasNext())
      {
        if (l2 != ((IMSCSmsMtEvent)localIterator2.next()).getMsgId())
          continue;
        j = 1;
      }
      IMSCSmsMtEvent localIMSCSmsMtEvent;
      if (j == 0)
      {
        localIMSCSmsMtEvent = new IMSCSmsMtEvent();
        localIMSCSmsMtEvent.setRegId(l1);
        localIMSCSmsMtEvent.setMsgId(l2);
        localIMSCSmsMtEvent.setFromAddress(str);
        localIMSCSmsMtEvent.setMsgFormat(i);
        localIMSCSmsMtEvent.setSmsBody(arrayOfByte);
      }
      synchronized (this.mSmsMtEvents)
      {
        this.mSmsMtEvents.add(localIMSCSmsMtEvent);
        Log.i("IMSCSmsService", "onSmsMtCallback: " + paramIMSCSmsMtEvent);
      }
    }
    synchronized (this.mCallbacks)
    {
      try
      {
        Iterator localIterator1 = this.mCallbacks.iterator();
        while (localIterator1.hasNext())
          ((IIMSCSmsCallback)localIterator1.next()).onMessageReceived(l1, l2, str, i, arrayOfByte);
      }
      catch (RemoteException localRemoteException)
      {
        Log.e("IMSCSmsService", "onSmsMtCallback exception: " + localRemoteException);
        return;
      }
      localObject2 = finally;
      monitorexit;
      throw localObject2;
      localObject3 = finally;
      monitorexit;
      throw localObject3;
    }
  }

  public int registerCallback(IIMSCSmsCallback paramIIMSCSmsCallback)
  {
    Log.i("IMSCSmsService", "registerCallback: " + paramIIMSCSmsCallback);
    try
    {
      long l = paramIIMSCSmsCallback.getRegistrationId();
      i = registerCallback(l, paramIIMSCSmsCallback);
      return i;
    }
    catch (RemoteException localRemoteException)
    {
      while (true)
      {
        Log.e("IMSCSmsService", "registerCallback failed, exception: " + localRemoteException);
        int i = -1;
      }
    }
  }

  public long sendMessage(long paramLong, String paramString, int paramInt, byte[] paramArrayOfByte)
  {
    long l = 0L;
    int i = 1;
    IMSCNative localIMSCNative = IMSCNative.getInstance();
    if (localIMSCNative != null)
    {
      i = getNextReplySeq();
      l = localIMSCNative.imscSmsSendMsg(paramLong, i, paramString, paramInt, paramArrayOfByte);
    }
    IMSCSmsMoEvent localIMSCSmsMoEvent;
    if (l != 0L)
    {
      localIMSCSmsMoEvent = new IMSCSmsMoEvent();
      localIMSCSmsMoEvent.setRegId(paramLong);
      localIMSCSmsMoEvent.setMsgId(l);
      localIMSCSmsMoEvent.setReplySeq(i);
      localIMSCSmsMoEvent.setToAddress(paramString);
      localIMSCSmsMoEvent.setMsgFormat(paramInt);
      localIMSCSmsMoEvent.setSmsBody(paramArrayOfByte);
    }
    synchronized (this.mSmsMoEvents)
    {
      this.mSmsMoEvents.add(localIMSCSmsMoEvent);
      Log.i("IMSCSmsService", "The sent MO message, " + localIMSCSmsMoEvent);
      return l;
    }
  }

  public boolean sendResponse(long paramLong1, long paramLong2, int paramInt, byte[] paramArrayOfByte)
  {
    int i = 0;
    while (true)
    {
      synchronized (this.mSmsMtEvents)
      {
        Iterator localIterator = this.mSmsMtEvents.iterator();
        if (!localIterator.hasNext())
          continue;
        if (paramLong2 != ((IMSCSmsMtEvent)localIterator.next()).getMsgId())
          continue;
        localIterator.remove();
        i = 1;
        if (i == 0)
        {
          Log.e("IMSCSmsService", "No matched msg is found in mSmsMtEvents.");
          j = 0;
          return j;
        }
      }
      boolean bool = false;
      IMSCNative localIMSCNative = IMSCNative.getInstance();
      if (localIMSCNative != null)
        bool = localIMSCNative.imscSmsSendResponse(paramLong1, paramInt, paramLong2, paramArrayOfByte);
      Log.i("IMSCSmsService", "The MT message response sent, regId: " + paramLong1 + " msgId:" + paramLong2 + " response:" + paramInt + " result:" + bool);
      int j = bool;
    }
  }

  public int unRegisterCallback(IIMSCSmsCallback paramIIMSCSmsCallback)
  {
    Log.i("IMSCSmsService", "unRegisterCallback: " + paramIIMSCSmsCallback);
    int i;
    if (paramIIMSCSmsCallback == null)
      i = -1;
    while (true)
    {
      return i;
      try
      {
        long l = paramIIMSCSmsCallback.getRegistrationId();
        i = unRegisterCallback(l, paramIIMSCSmsCallback);
      }
      catch (RemoteException localRemoteException)
      {
        Log.e("IMSCSmsService", "unRegisterCallback failed, exception: " + localRemoteException);
        i = -1;
      }
    }
  }

  private class CallbackDeathRecipient
    implements IBinder.DeathRecipient
  {
    private final WeakReference<IIMSCSmsCallback> mCallback;
    private final long mRegId;

    public CallbackDeathRecipient(IIMSCSmsCallback paramLong, long arg3)
    {
      this.mCallback = new WeakReference(paramLong);
      Object localObject;
      this.mRegId = localObject;
    }

    public void binderDied()
    {
      Log.i("IMSCSmsService", "CallbackDeathRecipient: associated callback " + this.mCallback.get() + " died.");
      IMSCSmsService.this.unRegisterCallback(this.mRegId, (IIMSCSmsCallback)this.mCallback.get());
    }

    public long getRegId()
    {
      return this.mRegId;
    }
  }
}

/* Location:           /home/dhacker29/jd/classes_dex2jar.jar
 * Qualified Name:     com.motorola.android.server.ims.IMSCSmsService
 * JD-Core Version:    0.6.0
 */