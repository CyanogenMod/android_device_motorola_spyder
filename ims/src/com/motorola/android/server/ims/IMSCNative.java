package com.motorola.android.server.ims;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

public class IMSCNative
{
  private static final int ACTION_START_ALARM_TIMER = 0;
  private static final int ACTION_STOP_ALARM_TIMER = 1;
  private static final int AKA_KEY_CALLBACK = 4;
  public static final int IMSC_DS_INTERFACE_EVDO = 0;
  public static final int IMSC_DS_INTERFACE_LTE = 2;
  public static final int IMSC_DS_INTERFACE_WIFI = 1;
  public static final int IMSC_MSG_LEVEL_ERROR = 3;
  public static final int IMSC_MSG_LEVEL_FATAL = 4;
  public static final int IMSC_MSG_LEVEL_HIGH = 2;
  public static final int IMSC_MSG_LEVEL_LOW = 0;
  public static final int IMSC_MSG_LEVEL_MED = 1;
  public static final long IMSC_OPERATION_ID_INVALID = 0L;
  public static final int IMSC_REGISTRATION_STATUS_REGISTERED = 2;
  public static final int IMSC_REGISTRATION_STATUS_REGISTERING = 1;
  public static final int IMSC_REGISTRATION_STATUS_UNREGISTERED = 0;
  public static final int IMSC_REGISTRATION_STATUS_UNREGISTERING = 3;
  public static final long IMSC_REG_ID_INVALID = 0L;
  public static final int IMSC_REG_SUBSCRIPTION_STATUS_FAILED = 4;
  public static final int IMSC_REG_SUBSCRIPTION_STATUS_SUBSCRIBED = 2;
  public static final int IMSC_REG_SUBSCRIPTION_STATUS_SUBSCRIBING = 1;
  public static final int IMSC_REG_SUBSCRIPTION_STATUS_UNSUBSCRIBED = 0;
  public static final int IMSC_REG_SUBSCRIPTION_STATUS_UNSUBSCRIBING = 3;
  public static final long IMSC_SMS_MESSAGE_ID_INVALID = 0L;
  private static final int INIT_MGR_CALLBACK = 0;
  private static final int REGISTRATION_CALLBACK = 1;
  private static final int SESSION_CACHE_SIZE = 32;
  private static final int SMS_MO_CALLBACK = 2;
  private static final int SMS_MT_CALLBACK = 3;
  private static final String TAG = "IMSCNative";
  private static final int WAKEUP_ALARM_CALLBACK = 5;
  private static boolean mNativeLibLoaded = false;
  private static IMSCNative mSingletonInstance;
  private long mDestroyRegId;
  private long mDestroyRegOperationId;
  private EventHandler mEventHandler;
  private IMSCMgrCallback mMgrCallback = null;
  private final LinkedHashMap<Long, IMSCRegistrationCallback> mRegistrationCache = new LinkedHashMap(32, 0.75F, true)
  {
    public boolean removeEldestEntry(Map.Entry paramEntry)
    {
      if (32 < size());
      for (int i = 1; ; i = 0)
        return i;
    }
  };
  private IMSCRegistrationEvent mRegistrationEvent = null;
  private final Object mRegistrationSyncObj = new Object();
  private final LinkedHashMap<Long, IMSCSmsCallback> mSmsCache = new LinkedHashMap(32, 0.75F, true)
  {
    public boolean removeEldestEntry(Map.Entry paramEntry)
    {
      if (32 < size());
      for (int i = 1; ; i = 0)
        return i;
    }
  };
  private long mUnRegistrationId;
  private long mUnRegistrationOperationId;

  static
  {
    try
    {
      System.loadLibrary("ims_client_jni");
      mNativeLibLoaded = true;
      label13: return;
    }
    catch (UnsatisfiedLinkError localUnsatisfiedLinkError)
    {
      while (true)
        Log.e("IMSCNative", "ERROR: Could not load libims_client_jni.so");
    }
    catch (SecurityException localSecurityException)
    {
      break label13;
    }
  }

  private IMSCNative()
  {
    Looper localLooper1 = Looper.myLooper();
    if (localLooper1 != null)
      this.mEventHandler = new EventHandler(this, localLooper1);
    while (true)
    {
      return;
      Looper localLooper2 = Looper.getMainLooper();
      if (localLooper2 != null)
      {
        this.mEventHandler = new EventHandler(this, localLooper2);
        continue;
      }
      this.mEventHandler = null;
    }
  }

  /** @deprecated */
  public static IMSCNative getInstance()
  {
    monitorenter;
    try
    {
      boolean bool = mNativeLibLoaded;
      if (!bool);
      for (IMSCNative localIMSCNative = null; ; localIMSCNative = mSingletonInstance)
      {
        return localIMSCNative;
        if (mSingletonInstance != null)
          continue;
        mSingletonInstance = new IMSCNative();
      }
    }
    finally
    {
      monitorexit;
    }
    throw localObject;
  }

  private boolean imscEventSyncCall(int paramInt1, int paramInt2, int paramInt3, Object paramObject)
  {
    int i = 0;
    switch (paramInt1)
    {
    default:
      Log.e("IMSCNative", "Unknown message type " + paramInt1);
    case 0:
    case 2:
    case 3:
    case 5:
    case 1:
    case 4:
    }
    while (true)
    {
      return i;
      synchronized (this.mRegistrationSyncObj)
      {
        this.mRegistrationEvent = ((IMSCRegistrationEvent)paramObject);
        this.mRegistrationSyncObj.notifyAll();
      }
      Object localObject1 = this.mRegistrationSyncObj;
      monitorenter;
      long l = paramInt2;
      try
      {
        if ((this.mUnRegistrationId == l) && (this.mUnRegistrationOperationId != 0L))
        {
          this.mRegistrationEvent = new IMSCRegistrationEvent();
          this.mRegistrationEvent.setRegId(l);
          this.mRegistrationEvent.setEventType(11);
          this.mRegistrationEvent.setEventData((byte[])(byte[])paramObject);
          this.mRegistrationSyncObj.notifyAll();
          i = 1;
        }
        monitorexit;
        continue;
      }
      finally
      {
        localObject2 = finally;
        monitorexit;
      }
    }
    throw localObject2;
  }

  private final native void imscNativeAlarmTimeout();

  private final native boolean imscNativeMgrDeInit();

  private final native boolean imscNativeMgrInit(String paramString, Object paramObject);

  private final native boolean imscNativeMgrIsRunning();

  private final native boolean imscNativeMgrReloadConfig();

  private final native Object imscNativeMgrRetrieveFTSInfo();

  private final native void imscNativeMgrSetDnsServer(String paramString);

  private final native void imscNativeMgrSetLocalIpAddr(byte[] paramArrayOfByte);

  private final native void imscNativeMgrSetLocalPort(int paramInt);

  private final native void imscNativeMgrSetLogLevel(int paramInt);

  private final native void imscNativeMgrSetPlatVersionStr(String paramString);

  private final native void imscNativeMgrSetSmsEvdoSigcomp(boolean paramBoolean);

  private final native void imscNativeMgrSetSmsEvdoSipTimers(int paramInt);

  private final native Object imscNativeParseOperationCompletedData(byte[] paramArrayOfByte);

  private final native Object imscNativeParseRegistrationFailedReason(byte[] paramArrayOfByte);

  private final native Object imscNativeParseRegistrationSuccessfulData(byte[] paramArrayOfByte);

  private final native String imscNativeParseServerName(byte[] paramArrayOfByte);

  private final native boolean imscNativeRegAddCredentials(long paramLong, String paramString1, String paramString2, String paramString3);

  private final native boolean imscNativeRegClearCredentials(long paramLong);

  private final native long imscNativeRegCreate();

  private final native long imscNativeRegDestroy(long paramLong);

  private final native boolean imscNativeRegGetCredentials(long paramLong, Object paramObject);

  private final native int imscNativeRegGetInterface(long paramLong);

  private final native long imscNativeRegGetNumOfRegistrations();

  private final native String imscNativeRegGetPublicURI(long paramLong);

  private final native String imscNativeRegGetRegistrar(long paramLong);

  private final native int imscNativeRegGetRegistrationStatus(long paramLong);

  private final native long[] imscNativeRegGetRegistrations();

  private final native int imscNativeRegGetSubscriptionStatus(long paramLong);

  private final native long imscNativeRegReRegister(long paramLong);

  private final native long imscNativeRegRegister(long paramLong);

  private final native boolean imscNativeRegSetInterface(long paramLong, int paramInt);

  private final native boolean imscNativeRegSetNwRegAttribute(long paramLong, boolean paramBoolean);

  private final native boolean imscNativeRegSetPCSCF(long paramLong, String paramString);

  private final native boolean imscNativeRegSetPCSCFPort(long paramLong, int paramInt);

  private final native boolean imscNativeRegSetPublicURI(long paramLong, String paramString);

  private final native boolean imscNativeRegSetRegistrar(long paramLong, String paramString);

  private final native boolean imscNativeRegSetSecAgreeSupport(long paramLong, boolean paramBoolean);

  private final native boolean imscNativeRegSetUACapabilities(long paramLong, String paramString);

  private final native long imscNativeRegUnRegister(long paramLong);

  private final native void imscNativeSetAKAKeys(long paramLong, int paramInt1, byte[] paramArrayOfByte1, int paramInt2, byte[] paramArrayOfByte2, int paramInt3, byte[] paramArrayOfByte3, int paramInt4, byte[] paramArrayOfByte4, int paramInt5);

  private final native boolean imscNativeSmsCancelMsg(long paramLong1, long paramLong2);

  private final native long imscNativeSmsSendMsg(long paramLong, int paramInt1, String paramString, int paramInt2, byte[] paramArrayOfByte);

  private final native boolean imscNativeSmsSendResponse(long paramLong1, int paramInt, long paramLong2, byte[] paramArrayOfByte);

  private final native boolean imscNativeSmsSetCallback(long paramLong, boolean paramBoolean);

  private void onMgrCallback(boolean paramBoolean)
  {
    Log.i("IMSCNative", "onMgrCallback: " + paramBoolean);
    if (this.mMgrCallback != null)
      this.mMgrCallback.onInitialized(paramBoolean);
  }

  private void onMgrWakeupAlarmCallback(int paramInt, long paramLong)
  {
    Log.i("IMSCNative", "onMgrWakeupAlarmCallback: " + paramInt + ", timer:" + paramLong);
    if (this.mMgrCallback != null)
    {
      if (paramInt != 0)
        break label58;
      this.mMgrCallback.onStartAlarmTimer(paramLong);
    }
    while (true)
    {
      return;
      label58: if (paramInt == 1)
      {
        this.mMgrCallback.onStopAlarmTimer();
        continue;
      }
    }
  }

  private void onRegistrationCallback(IMSCRegistrationEvent paramIMSCRegistrationEvent)
  {
    Log.i("IMSCNative", "onRegistrationCallback: " + paramIMSCRegistrationEvent);
    long l = paramIMSCRegistrationEvent.getRegId();
    if (0L != l)
      synchronized (this.mRegistrationCache)
      {
        IMSCRegistrationCallback localIMSCRegistrationCallback = (IMSCRegistrationCallback)this.mRegistrationCache.get(Long.valueOf(l));
        if (localIMSCRegistrationCallback != null)
          localIMSCRegistrationCallback.onRegistrationCallback(paramIMSCRegistrationEvent);
        if (paramIMSCRegistrationEvent.getEventType() == 10)
        {
          IMSCRegistrationEvent.IMSCOperationCompletedData localIMSCOperationCompletedData = paramIMSCRegistrationEvent.getOperationCompletedData();
          if ((l != this.mDestroyRegId) || (localIMSCOperationCompletedData == null) || (localIMSCOperationCompletedData.mOperationId != this.mDestroyRegOperationId) || (localIMSCOperationCompletedData.mResult != true));
        }
      }
    synchronized (this.mRegistrationCache)
    {
      this.mRegistrationCache.remove(Long.valueOf(l));
      return;
      localObject1 = finally;
      monitorexit;
      throw localObject1;
    }
  }

  // ERROR //
  private void onSmsMoCallback(IMSCSmsMoEvent paramIMSCSmsMoEvent)
  {
    // Byte code:
    //   0: ldc 63
    //   2: new 181	java/lang/StringBuilder
    //   5: dup
    //   6: invokespecial 182	java/lang/StringBuilder:<init>	()V
    //   9: ldc_w 354
    //   12: invokevirtual 188	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   15: aload_1
    //   16: invokevirtual 313	java/lang/StringBuilder:append	(Ljava/lang/Object;)Ljava/lang/StringBuilder;
    //   19: invokevirtual 195	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   22: invokestatic 292	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   25: pop
    //   26: aload_1
    //   27: invokevirtual 357	com/motorola/android/server/ims/IMSCSmsMoEvent:getRegId	()J
    //   30: lstore_3
    //   31: ldc2_w 41
    //   34: lload_3
    //   35: lcmp
    //   36: ifeq +53 -> 89
    //   39: aload_0
    //   40: getfield 128	com/motorola/android/server/ims/IMSCNative:mSmsCache	Ljava/util/LinkedHashMap;
    //   43: astore 12
    //   45: aload 12
    //   47: monitorenter
    //   48: aload_0
    //   49: getfield 128	com/motorola/android/server/ims/IMSCNative:mSmsCache	Ljava/util/LinkedHashMap;
    //   52: lload_3
    //   53: invokestatic 322	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   56: invokevirtual 328	java/util/LinkedHashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   59: checkcast 13	com/motorola/android/server/ims/IMSCNative$IMSCSmsCallback
    //   62: astore 14
    //   64: aload 12
    //   66: monitorexit
    //   67: aload 14
    //   69: ifnull +11 -> 80
    //   72: aload 14
    //   74: aload_1
    //   75: invokeinterface 358 2 0
    //   80: return
    //   81: astore 13
    //   83: aload 12
    //   85: monitorexit
    //   86: aload 13
    //   88: athrow
    //   89: aload_0
    //   90: getfield 128	com/motorola/android/server/ims/IMSCNative:mSmsCache	Ljava/util/LinkedHashMap;
    //   93: astore 5
    //   95: aload 5
    //   97: monitorenter
    //   98: aload_0
    //   99: getfield 128	com/motorola/android/server/ims/IMSCNative:mSmsCache	Ljava/util/LinkedHashMap;
    //   102: invokevirtual 362	java/util/LinkedHashMap:entrySet	()Ljava/util/Set;
    //   105: invokeinterface 368 1 0
    //   110: astore 7
    //   112: aload 7
    //   114: invokeinterface 373 1 0
    //   119: ifeq +81 -> 200
    //   122: aload 7
    //   124: invokeinterface 376 1 0
    //   129: checkcast 378	java/util/Map$Entry
    //   132: astore 8
    //   134: aload 8
    //   136: invokeinterface 381 1 0
    //   141: checkcast 318	java/lang/Long
    //   144: invokevirtual 384	java/lang/Long:longValue	()J
    //   147: lstore 9
    //   149: aload 8
    //   151: invokeinterface 387 1 0
    //   156: checkcast 13	com/motorola/android/server/ims/IMSCNative$IMSCSmsCallback
    //   159: astore 11
    //   161: ldc2_w 41
    //   164: lload 9
    //   166: lcmp
    //   167: ifeq -55 -> 112
    //   170: aload 11
    //   172: ifnull -60 -> 112
    //   175: aload_1
    //   176: lload 9
    //   178: invokevirtual 388	com/motorola/android/server/ims/IMSCSmsMoEvent:setRegId	(J)V
    //   181: aload 11
    //   183: aload_1
    //   184: invokeinterface 358 2 0
    //   189: goto -77 -> 112
    //   192: astore 6
    //   194: aload 5
    //   196: monitorexit
    //   197: aload 6
    //   199: athrow
    //   200: aload 5
    //   202: monitorexit
    //   203: goto -123 -> 80
    //
    // Exception table:
    //   from	to	target	type
    //   48	67	81	finally
    //   83	86	81	finally
    //   98	197	192	finally
    //   200	203	192	finally
  }

  // ERROR //
  private void onSmsMtCallback(IMSCSmsMtEvent paramIMSCSmsMtEvent)
  {
    // Byte code:
    //   0: ldc 63
    //   2: new 181	java/lang/StringBuilder
    //   5: dup
    //   6: invokespecial 182	java/lang/StringBuilder:<init>	()V
    //   9: ldc_w 390
    //   12: invokevirtual 188	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   15: aload_1
    //   16: invokevirtual 313	java/lang/StringBuilder:append	(Ljava/lang/Object;)Ljava/lang/StringBuilder;
    //   19: invokevirtual 195	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   22: invokestatic 292	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   25: pop
    //   26: aload_1
    //   27: invokevirtual 393	com/motorola/android/server/ims/IMSCSmsMtEvent:getRegId	()J
    //   30: lstore_3
    //   31: ldc2_w 41
    //   34: lload_3
    //   35: lcmp
    //   36: ifeq +53 -> 89
    //   39: aload_0
    //   40: getfield 128	com/motorola/android/server/ims/IMSCNative:mSmsCache	Ljava/util/LinkedHashMap;
    //   43: astore 12
    //   45: aload 12
    //   47: monitorenter
    //   48: aload_0
    //   49: getfield 128	com/motorola/android/server/ims/IMSCNative:mSmsCache	Ljava/util/LinkedHashMap;
    //   52: lload_3
    //   53: invokestatic 322	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   56: invokevirtual 328	java/util/LinkedHashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   59: checkcast 13	com/motorola/android/server/ims/IMSCNative$IMSCSmsCallback
    //   62: astore 14
    //   64: aload 12
    //   66: monitorexit
    //   67: aload 14
    //   69: ifnull +11 -> 80
    //   72: aload 14
    //   74: aload_1
    //   75: invokeinterface 394 2 0
    //   80: return
    //   81: astore 13
    //   83: aload 12
    //   85: monitorexit
    //   86: aload 13
    //   88: athrow
    //   89: aload_0
    //   90: getfield 128	com/motorola/android/server/ims/IMSCNative:mSmsCache	Ljava/util/LinkedHashMap;
    //   93: astore 5
    //   95: aload 5
    //   97: monitorenter
    //   98: aload_0
    //   99: getfield 128	com/motorola/android/server/ims/IMSCNative:mSmsCache	Ljava/util/LinkedHashMap;
    //   102: invokevirtual 362	java/util/LinkedHashMap:entrySet	()Ljava/util/Set;
    //   105: invokeinterface 368 1 0
    //   110: astore 7
    //   112: aload 7
    //   114: invokeinterface 373 1 0
    //   119: ifeq +81 -> 200
    //   122: aload 7
    //   124: invokeinterface 376 1 0
    //   129: checkcast 378	java/util/Map$Entry
    //   132: astore 8
    //   134: aload 8
    //   136: invokeinterface 381 1 0
    //   141: checkcast 318	java/lang/Long
    //   144: invokevirtual 384	java/lang/Long:longValue	()J
    //   147: lstore 9
    //   149: aload 8
    //   151: invokeinterface 387 1 0
    //   156: checkcast 13	com/motorola/android/server/ims/IMSCNative$IMSCSmsCallback
    //   159: astore 11
    //   161: ldc2_w 41
    //   164: lload 9
    //   166: lcmp
    //   167: ifeq -55 -> 112
    //   170: aload 11
    //   172: ifnull -60 -> 112
    //   175: aload_1
    //   176: lload 9
    //   178: invokevirtual 395	com/motorola/android/server/ims/IMSCSmsMtEvent:setRegId	(J)V
    //   181: aload 11
    //   183: aload_1
    //   184: invokeinterface 394 2 0
    //   189: goto -77 -> 112
    //   192: astore 6
    //   194: aload 5
    //   196: monitorexit
    //   197: aload 6
    //   199: athrow
    //   200: aload 5
    //   202: monitorexit
    //   203: goto -123 -> 80
    //
    // Exception table:
    //   from	to	target	type
    //   48	67	81	finally
    //   83	86	81	finally
    //   98	197	192	finally
    //   200	203	192	finally
  }

  private static void postEventFromNative(Object paramObject1, int paramInt1, int paramInt2, int paramInt3, Object paramObject2)
  {
    IMSCNative localIMSCNative = (IMSCNative)((WeakReference)paramObject1).get();
    if (localIMSCNative == null);
    while (true)
    {
      return;
      if ((!localIMSCNative.imscEventSyncCall(paramInt1, paramInt2, paramInt3, paramObject2)) && (localIMSCNative.mEventHandler != null))
      {
        Message localMessage = localIMSCNative.mEventHandler.obtainMessage(paramInt1, paramInt2, paramInt3, paramObject2);
        localIMSCNative.mEventHandler.sendMessage(localMessage);
        continue;
      }
    }
  }

  public long getDestroyRegOperationId()
  {
    return this.mDestroyRegOperationId;
  }

  public void imscAlarmTimeout()
  {
    imscNativeAlarmTimeout();
  }

  public boolean imscMgrDeInit()
  {
    this.mMgrCallback = null;
    synchronized (this.mRegistrationCache)
    {
      this.mRegistrationCache.clear();
    }
    synchronized (this.mSmsCache)
    {
      this.mSmsCache.clear();
      return imscNativeMgrDeInit();
      localObject1 = finally;
      monitorexit;
      throw localObject1;
    }
  }

  public boolean imscMgrInit(String paramString, IMSCMgrCallback paramIMSCMgrCallback)
  {
    this.mMgrCallback = paramIMSCMgrCallback;
    synchronized (this.mRegistrationCache)
    {
      this.mRegistrationCache.clear();
    }
    synchronized (this.mSmsCache)
    {
      this.mSmsCache.clear();
      return imscNativeMgrInit(paramString, new WeakReference(this));
      localObject1 = finally;
      monitorexit;
      throw localObject1;
    }
  }

  public boolean imscMgrIsRunning()
  {
    return imscNativeMgrIsRunning();
  }

  public boolean imscMgrReloadConfig()
  {
    return imscNativeMgrReloadConfig();
  }

  public IMSCMgrFTSInfo imscMgrRetrieveFTSInfo()
  {
    return (IMSCMgrFTSInfo)imscNativeMgrRetrieveFTSInfo();
  }

  public void imscMgrSetDnsServer(String paramString)
  {
    imscNativeMgrSetDnsServer(paramString);
  }

  public boolean imscMgrSetLocalIpAddr(String paramString)
  {
    String str = paramString.trim();
    if ((str.substring(0, 1).equals("[")) && (str.substring(str.length() - 1).equals("]")))
      str = str.substring(1, str.length() - 1);
    Object localObject = null;
    try
    {
      InetAddress localInetAddress = InetAddress.getByName(str);
      localObject = localInetAddress;
      if (localObject != null)
      {
        byte[] arrayOfByte = localObject.getAddress();
        if ((arrayOfByte != null) && ((arrayOfByte.length == 4) || (arrayOfByte.length == 16)))
        {
          imscNativeMgrSetLocalIpAddr(arrayOfByte);
          i = 1;
          return i;
        }
      }
    }
    catch (UnknownHostException localUnknownHostException)
    {
      while (true)
      {
        Log.i("IMSCNative", "UnknownHostException: " + str + " Exception:" + localUnknownHostException);
        continue;
        Log.e("IMSCNative", "Failed to set local ip address: " + str);
        int i = 0;
      }
    }
  }

  public void imscMgrSetLocalPort(int paramInt)
  {
    imscNativeMgrSetLocalPort(paramInt);
  }

  public void imscMgrSetLogLevel(int paramInt)
  {
    imscNativeMgrSetLogLevel(paramInt);
  }

  public void imscMgrSetPlatVersionStr(String paramString)
  {
    imscNativeMgrSetPlatVersionStr(paramString);
  }

  public void imscMgrSetSmsEvdoSigcomp(boolean paramBoolean)
  {
    imscNativeMgrSetSmsEvdoSigcomp(paramBoolean);
  }

  public void imscMgrSetSmsEvdoSipTimers(int paramInt)
  {
    imscNativeMgrSetSmsEvdoSipTimers(paramInt);
  }

  public Object imscParseOperationCompletedData(byte[] paramArrayOfByte)
  {
    return imscNativeParseOperationCompletedData(paramArrayOfByte);
  }

  public Object imscParseRegistrationFailedReason(byte[] paramArrayOfByte)
  {
    return imscNativeParseRegistrationFailedReason(paramArrayOfByte);
  }

  public Object imscParseRegistrationSuccessfulData(byte[] paramArrayOfByte)
  {
    return imscNativeParseRegistrationSuccessfulData(paramArrayOfByte);
  }

  public String imscParseServerName(byte[] paramArrayOfByte)
  {
    return imscNativeParseServerName(paramArrayOfByte);
  }

  public boolean imscRegAddCredentials(long paramLong, String paramString1, String paramString2, String paramString3)
  {
    return imscNativeRegAddCredentials(paramLong, paramString1, paramString2, paramString3);
  }

  public boolean imscRegClearCredentials(long paramLong)
  {
    return imscNativeRegClearCredentials(paramLong);
  }

  public long imscRegCreate()
  {
    return imscNativeRegCreate();
  }

  public long imscRegDestroy(long paramLong)
  {
    this.mDestroyRegId = paramLong;
    this.mDestroyRegOperationId = imscNativeRegDestroy(paramLong);
    if (0L != paramLong);
    synchronized (this.mSmsCache)
    {
      this.mSmsCache.remove(Long.valueOf(paramLong));
      return this.mDestroyRegOperationId;
    }
  }

  public boolean imscRegDestroyBlocking(long paramLong)
  {
    int i;
    if (0L == paramLong)
    {
      i = 1;
      return i;
    }
    this.mDestroyRegId = paramLong;
    boolean bool = true;
    this.mDestroyRegOperationId = imscNativeRegDestroy(paramLong);
    long l1 = 1000L + System.currentTimeMillis();
    while (true)
    {
      long l2;
      synchronized (this.mRegistrationSyncObj)
      {
        while (true)
        {
          l2 = l1 - System.currentTimeMillis();
          if (l2 > 0L)
            break;
          if (0L != paramLong);
          synchronized (this.mSmsCache)
          {
            this.mSmsCache.remove(Long.valueOf(paramLong));
            i = bool;
          }
        }
      }
      try
      {
        this.mRegistrationSyncObj.wait(l2);
        label115: IMSCRegistrationEvent localIMSCRegistrationEvent = this.mRegistrationEvent;
        this.mRegistrationEvent = null;
        if ((localIMSCRegistrationEvent == null) || (localIMSCRegistrationEvent.getEventType() != 10))
          continue;
        IMSCRegistrationEvent.IMSCOperationCompletedData localIMSCOperationCompletedData = localIMSCRegistrationEvent.getOperationCompletedData();
        if ((localIMSCRegistrationEvent.getRegId() != this.mDestroyRegId) || (localIMSCOperationCompletedData == null) || (localIMSCOperationCompletedData.mOperationId != this.mDestroyRegOperationId))
          continue;
        bool = localIMSCOperationCompletedData.mResult;
        continue;
        localObject2 = finally;
        monitorexit;
        throw localObject2;
        localObject3 = finally;
        monitorexit;
        throw localObject3;
      }
      catch (InterruptedException localInterruptedException)
      {
        break label115;
      }
    }
  }

  public boolean imscRegGetCredentials(long paramLong, Object paramObject)
  {
    return imscNativeRegGetCredentials(paramLong, paramObject);
  }

  public int imscRegGetInterface(long paramLong)
  {
    return imscNativeRegGetInterface(paramLong);
  }

  public long imscRegGetNumOfRegistrations()
  {
    return imscNativeRegGetNumOfRegistrations();
  }

  public String imscRegGetPublicURI(long paramLong)
  {
    return imscNativeRegGetPublicURI(paramLong);
  }

  public String imscRegGetRegistrar(long paramLong)
  {
    return imscNativeRegGetRegistrar(paramLong);
  }

  public int imscRegGetRegistrationStatus(long paramLong)
  {
    return imscNativeRegGetRegistrationStatus(paramLong);
  }

  public long[] imscRegGetRegistrations()
  {
    return imscNativeRegGetRegistrations();
  }

  public int imscRegGetSubscriptionStatus(long paramLong)
  {
    return imscNativeRegGetSubscriptionStatus(paramLong);
  }

  public long imscRegReRegister(long paramLong)
  {
    return imscNativeRegReRegister(paramLong);
  }

  public long imscRegRegister(long paramLong)
  {
    return imscNativeRegRegister(paramLong);
  }

  public boolean imscRegSetInterface(long paramLong, int paramInt)
  {
    return imscNativeRegSetInterface(paramLong, paramInt);
  }

  public boolean imscRegSetNwRegAttribute(long paramLong, boolean paramBoolean)
  {
    return imscNativeRegSetNwRegAttribute(paramLong, paramBoolean);
  }

  public boolean imscRegSetPCSCF(long paramLong, String paramString)
  {
    return imscNativeRegSetPCSCF(paramLong, paramString);
  }

  public boolean imscRegSetPCSCFPort(long paramLong, int paramInt)
  {
    return imscNativeRegSetPCSCFPort(paramLong, paramInt);
  }

  public boolean imscRegSetPublicURI(long paramLong, String paramString)
  {
    return imscNativeRegSetPublicURI(paramLong, paramString);
  }

  public boolean imscRegSetRegistrar(long paramLong, String paramString)
  {
    return imscNativeRegSetRegistrar(paramLong, paramString);
  }

  public boolean imscRegSetSecAgreeSupport(long paramLong, boolean paramBoolean)
  {
    return imscNativeRegSetSecAgreeSupport(paramLong, paramBoolean);
  }

  public boolean imscRegSetUACapabilities(long paramLong, String paramString)
  {
    return imscNativeRegSetUACapabilities(paramLong, paramString);
  }

  public long imscRegUnRegister(long paramLong)
  {
    return imscNativeRegUnRegister(paramLong);
  }

  public boolean imscRegUnRegisterBlocking(long paramLong)
  {
    int k;
    if (0L == paramLong)
    {
      k = 1;
      return k;
    }
    this.mUnRegistrationId = paramLong;
    int i = 0;
    boolean bool = true;
    this.mUnRegistrationOperationId = imscNativeRegUnRegister(paramLong);
    long l1 = 15000L + System.currentTimeMillis();
    while (true)
    {
      long l2;
      synchronized (this.mRegistrationSyncObj)
      {
        l2 = l1 - System.currentTimeMillis();
        if (l2 <= 0L)
        {
          this.mUnRegistrationId = 0L;
          this.mUnRegistrationOperationId = 0L;
          k = bool;
        }
      }
      try
      {
        this.mRegistrationSyncObj.wait(l2);
        label101: IMSCRegistrationEvent localIMSCRegistrationEvent = this.mRegistrationEvent;
        this.mRegistrationEvent = null;
        if (localIMSCRegistrationEvent == null)
          continue;
        int j = localIMSCRegistrationEvent.getEventType();
        if (j == 11)
        {
          if (localIMSCRegistrationEvent.getRegId() != this.mUnRegistrationId)
            continue;
          IMSCConfReader localIMSCConfReader = IMSCConfReader.getInstance();
          if (localIMSCConfReader == null)
            continue;
          localIMSCConfReader.doAkaAuth((int)localIMSCRegistrationEvent.getRegId(), localIMSCRegistrationEvent.getEventData());
          continue;
          localObject2 = finally;
          monitorexit;
          throw localObject2;
        }
        if ((j == 8) || (j == 9))
        {
          if (localIMSCRegistrationEvent.getRegId() != this.mUnRegistrationId)
            continue;
          i = 1;
          continue;
        }
        if (j != 10)
          continue;
        IMSCRegistrationEvent.IMSCOperationCompletedData localIMSCOperationCompletedData = localIMSCRegistrationEvent.getOperationCompletedData();
        if ((localIMSCRegistrationEvent.getRegId() != this.mUnRegistrationId) || (localIMSCOperationCompletedData == null) || (localIMSCOperationCompletedData.mOperationId != this.mUnRegistrationOperationId))
          continue;
        if (i == 0)
          Log.e("IMSCNative", "There is no UnRegistration success or fail received before operation completed.");
        this.mUnRegistrationId = 0L;
        bool = localIMSCOperationCompletedData.mResult;
      }
      catch (InterruptedException localInterruptedException)
      {
        break label101;
      }
    }
  }

  public boolean imscSetRegistrationCallback(long paramLong, IMSCRegistrationCallback paramIMSCRegistrationCallback)
  {
    int i;
    if (0L == paramLong)
      i = 0;
    while (true)
    {
      return i;
      LinkedHashMap localLinkedHashMap = this.mRegistrationCache;
      monitorenter;
      if (paramIMSCRegistrationCallback != null);
      try
      {
        this.mRegistrationCache.put(Long.valueOf(paramLong), paramIMSCRegistrationCallback);
        monitorexit;
        i = 1;
        continue;
        this.mRegistrationCache.remove(Long.valueOf(paramLong));
      }
      finally
      {
        monitorexit;
      }
    }
  }

  public boolean imscSetSmsCallback(long paramLong, IMSCSmsCallback paramIMSCSmsCallback)
  {
    boolean bool2;
    if (0L == paramLong)
      bool2 = false;
    while (true)
    {
      return bool2;
      boolean bool1 = false;
      LinkedHashMap localLinkedHashMap = this.mSmsCache;
      monitorenter;
      if (paramIMSCSmsCallback != null);
      try
      {
        this.mSmsCache.put(Long.valueOf(paramLong), paramIMSCSmsCallback);
        while (true)
        {
          monitorexit;
          bool2 = imscNativeSmsSetCallback(paramLong, bool1);
          break;
          this.mSmsCache.remove(Long.valueOf(paramLong));
          bool1 = true;
        }
      }
      finally
      {
        monitorexit;
      }
    }
    throw localObject;
  }

  public boolean imscSmsCancelMsg(long paramLong1, long paramLong2)
  {
    return imscNativeSmsCancelMsg(paramLong1, paramLong2);
  }

  public long imscSmsSendMsg(long paramLong, int paramInt1, String paramString, int paramInt2, byte[] paramArrayOfByte)
  {
    return imscNativeSmsSendMsg(paramLong, paramInt1, paramString, paramInt2, paramArrayOfByte);
  }

  public boolean imscSmsSendResponse(long paramLong1, int paramInt, long paramLong2, byte[] paramArrayOfByte)
  {
    return imscNativeSmsSendResponse(paramLong1, paramInt, paramLong2, paramArrayOfByte);
  }

  public void setAKAKeys(long paramLong, int paramInt1, byte[] paramArrayOfByte1, int paramInt2, byte[] paramArrayOfByte2, int paramInt3, byte[] paramArrayOfByte3, int paramInt4, byte[] paramArrayOfByte4, int paramInt5)
  {
    imscNativeSetAKAKeys(paramLong, paramInt1, paramArrayOfByte1, paramInt2, paramArrayOfByte2, paramInt3, paramArrayOfByte3, paramInt4, paramArrayOfByte4, paramInt5);
  }

  private static class EventHandler extends Handler
  {
    private IMSCNative mInstance;

    public EventHandler(IMSCNative paramIMSCNative, Looper paramLooper)
    {
      super();
      this.mInstance = paramIMSCNative;
    }

    public void handleMessage(Message paramMessage)
    {
      switch (paramMessage.what)
      {
      default:
        Log.e("IMSCNative", "Unknown message type " + paramMessage.what);
      case 0:
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
      }
      while (true)
      {
        return;
        IMSCNative localIMSCNative = this.mInstance;
        if (paramMessage.arg1 == 0);
        for (boolean bool = true; ; bool = false)
        {
          localIMSCNative.onMgrCallback(bool);
          break;
        }
        this.mInstance.onRegistrationCallback((IMSCRegistrationEvent)(IMSCRegistrationEvent)paramMessage.obj);
        continue;
        this.mInstance.onSmsMoCallback((IMSCSmsMoEvent)(IMSCSmsMoEvent)paramMessage.obj);
        continue;
        this.mInstance.onSmsMtCallback((IMSCSmsMtEvent)(IMSCSmsMtEvent)paramMessage.obj);
        continue;
        IMSCConfReader localIMSCConfReader = IMSCConfReader.getInstance();
        if (localIMSCConfReader == null)
          continue;
        localIMSCConfReader.doAkaAuth(paramMessage.arg1, (byte[])(byte[])paramMessage.obj);
        continue;
        this.mInstance.onMgrWakeupAlarmCallback(paramMessage.arg1, paramMessage.arg2);
      }
    }
  }

  public static abstract interface IMSCSmsCallback
  {
    public abstract void onSmsMoCallback(IMSCSmsMoEvent paramIMSCSmsMoEvent);

    public abstract void onSmsMtCallback(IMSCSmsMtEvent paramIMSCSmsMtEvent);
  }

  public static abstract interface IMSCRegistrationCallback
  {
    public abstract void onRegistrationCallback(IMSCRegistrationEvent paramIMSCRegistrationEvent);
  }

  public static abstract interface IMSCMgrCallback
  {
    public abstract void onInitialized(boolean paramBoolean);

    public abstract void onStartAlarmTimer(long paramLong);

    public abstract void onStopAlarmTimer();
  }
}

/* Location:           /home/dhacker29/jd/classes_dex2jar.jar
 * Qualified Name:     com.motorola.android.server.ims.IMSCNative
 * JD-Core Version:    0.6.0
 */