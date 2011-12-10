package com.motorola.android.server.ims;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfoOem;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.RetryManager;
import com.motorola.android.ims.IConnectionStateListener;
import com.motorola.android.ims.IIMSNetwork.Stub;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class IMSCNetwork extends IIMSNetwork.Stub
{
  public static final String ACTION_MGR_INIT = "com.motorola.ims.action.mgrInit";
  public static final String ACTION_NATIVE_TIMER = "com.motorola.ims.action.nativetimer";
  public static final String ACTION_RECONNECT_ALARM = "com.motorola.ims.action.link-reconnect";
  private static final String DEFAULT_EHRPD_LINK_RETRY_CONFIG = "max_retries=infinite,5,965";
  private static final String DEFAULT_LTE_LINK_RETRY_CONFIG = "max_retries=infinite,5,5,80,125,485,905";
  private static final int EVENT_IMSCNETWORK_BASE = 11;
  private static final int EVENT_IMS_DOREGISTER = 12;
  private static final int EVENT_IMS_UNREGISTER = 13;
  private static final int EVENT_START_CONF_READER = 14;
  private static final int EVENT_START_IMS_CONN = 16;
  private static final int EVENT_STOP_IMS_CONN = 11;
  private static final int EVENT_UICC_REFRESH = 15;
  private static final int LINK_CONN_TIMEOUT_ID_10S = 10;
  private static final int LINK_CONN_TIMEOUT_ID_15M = 3;
  private static final int LINK_CONN_TIMEOUT_ID_1S = 1;
  private static final int LINK_CONN_TIMEOUT_ID_30S = 2;
  private static final int LINK_CONN_TIMEOUT_ID_DEFAULT = 0;
  private static final int STATE_CONNECTING = 1;
  private static final int STATE_CONN_READY = 2;
  private static final int STATE_DISCONNECTING = 4;
  private static final int STATE_IDLE = 0;
  private static final int STATE_IMS_READY = 3;
  private static final String TAG = "IMSCNetwork";
  private static Context mContext;
  private static String mPublicURI_IMSI;
  private static String mPublicURI_MSISDN = "sip:+19085554321@vzims.com";
  private static long mRegId;
  private static IMSCNetwork mSingletonInstance;
  private boolean dataEnabled = true;
  private AlarmManager mAlarmManager = null;
  private RegistrationCallback mCallBack = new RegistrationCallback(null);
  private String mConfigFile;
  private ConnectivityManager mConnMgr = null;
  private PowerManager.WakeLock mDataRetryWakeLock = null;
  private IMSCConnectMgr mIMSConn = null;
  private IMSCNative.IMSCMgrCallback mImscMgrCB = new IMSCNative.IMSCMgrCallback()
  {
    public void onInitialized(boolean paramBoolean)
    {
      Log.i("IMSCNetwork", "IMSCMgrCallback.onInitialized call back - " + paramBoolean);
      IMSCNetwork.access$2202(IMSCNetwork.this, paramBoolean);
      if (paramBoolean)
      {
        IMSCNetwork.access$2302(IMSCNetwork.this, 0);
        if (IMSCNetwork.this.mAlarmManager != null)
          IMSCNetwork.this.mAlarmManager.cancel(IMSCNetwork.this.mMgrInitIntent);
        IMSCNative localIMSCNative = IMSCNative.getInstance();
        if (localIMSCNative != null)
          localIMSCNative.imscMgrSetPlatVersionStr(Build.DISPLAY);
        IMSCNetwork.this.checkAndStartDataConnection();
      }
      while (true)
      {
        return;
        IMSCNetwork.this.processImsMgrInitFailed();
      }
    }

    public void onStartAlarmTimer(long paramLong)
    {
      Log.i("IMSCNetwork", "onStartAlarmTimer, timeOut: " + paramLong);
      long l = paramLong + System.currentTimeMillis();
      IMSCNetwork.this.mAlarmManager.set(0, l, IMSCNetwork.this.mNativeTimerIntent);
    }

    public void onStopAlarmTimer()
    {
      Log.i("IMSCNetwork", "onStopAlarmTimer");
      if (IMSCNetwork.this.mAlarmManager != null)
        IMSCNetwork.this.mAlarmManager.cancel(IMSCNetwork.this.mNativeTimerIntent);
    }
  };
  private boolean mIs403 = false;
  private boolean mIs404 = false;
  private boolean mIsImsiUri = false;
  private boolean mIsMgrInit = false;
  private boolean mIsRegistering = false;
  private boolean mIsUiccRefreshInProgress = false;
  private boolean mLimitedAccess = false;
  private PendingIntent mLinkRetryTimeoutIntent;
  private PendingIntent mMgrInitIntent;
  private int mMgrInitTryTimes;
  private PendingIntent mNativeTimerIntent;
  private boolean mNeedRegForConfigUpdate = false;
  private boolean mNetworkTerminationReqReceivedBefore = false;
  private int mNetworkType = 0;
  private PowerManager mPowerManager = null;
  private boolean mReReg = false;
  private final ArrayList<Record> mRecords = new ArrayList();
  private RetryManager mRetryMgr = null;
  private ServiceHandler mServiceHandler;
  private Looper mServiceLooper;
  private int mState = 0;
  private TelephonyManager mTelephonyMgr = null;
  private PowerManager.WakeLock mWakeLock = null;
  private InetAddress mlocalAddr = null;
  private boolean simReady = false;

  static
  {
    mPublicURI_IMSI = "sip:@ims.mnc480.mcc311.3gppnetwork.org";
    mRegId = 0L;
  }

  private IMSCNetwork()
  {
    if (this.mRetryMgr != null)
    {
      Log.i("IMSCNetwork", "DEFAULT_LTE_LINK_RETRY_CONFIG=max_retries=infinite,5,5,80,125,485,905");
      this.mRetryMgr.configure("max_retries=infinite,5,5,80,125,485,905");
    }
    HandlerThread localHandlerThread = new HandlerThread("IMSCNetworkThread");
    localHandlerThread.start();
    this.mServiceLooper = localHandlerThread.getLooper();
    this.mServiceHandler = new ServiceHandler(this.mServiceLooper);
  }

  private void checkAndStartDataConnection()
  {
    Log.i("IMSCNetwork", "in checkAndStartDataConnection()");
    if (isInternalTest())
      doRegister();
    while (true)
    {
      return;
      if (!isImsModeValid())
      {
        Log.i("IMSCNetwork", "checkAndStartDataConnection, IMS mode is not valid");
        continue;
      }
      if (!this.mIsMgrInit)
      {
        Log.i("IMSCNetwork", "ImsMgrInit is not finished, so needn't start IMS PDN");
        continue;
      }
      if (this.mIs403)
      {
        Log.i("IMSCNetwork", "IMS registration 403 failed, so needn't start IMS PDN");
        continue;
      }
      Log.i("IMSCNetwork", "in checkAndStartDataConnection(), netType: " + this.mNetworkType);
      if ((this.mNetworkType != 13) && (this.mNetworkType != 14))
      {
        Log.i("IMSCNetwork", "The network is neither LTE nor EHRPD, will not call startUsingNetworkFeature().");
        continue;
      }
      startDataConnection();
    }
  }

  private void deRegister(long paramLong)
  {
    IMSCNative localIMSCNative = IMSCNative.getInstance();
    if (localIMSCNative == null);
    while (true)
    {
      return;
      if (3 == localIMSCNative.imscRegGetRegistrationStatus(paramLong))
      {
        Log.i("IMSCNetwork", "deregister: we can not do deregister while in deregistering state.");
        continue;
      }
      localIMSCNative.imscRegUnRegisterBlocking(paramLong);
      localIMSCNative.imscRegDestroyBlocking(paramLong);
      Log.i("IMSCNetwork", "deRegister, notifyNetworkDisConnected");
      notifyNetworkDisConnected();
    }
  }

  private void deRegisterAllClients()
  {
    IMSCNative localIMSCNative = IMSCNative.getInstance();
    if (localIMSCNative == null);
    while (true)
    {
      return;
      long l1 = localIMSCNative.imscRegGetNumOfRegistrations();
      Log.i("IMSCNetwork", "imscRegGetNumOfRegistrations: " + l1);
      if (l1 <= 0L)
        continue;
      long[] arrayOfLong = localIMSCNative.imscRegGetRegistrations();
      for (int i = 0; i < arrayOfLong.length; i++)
      {
        long l2 = arrayOfLong[i];
        Log.i("IMSCNetwork", "The active registration " + i + " id: " + l2);
        int j = localIMSCNative.imscRegGetRegistrationStatus(l2);
        if ((1 == j) || (2 == j))
          localIMSCNative.imscRegUnRegisterBlocking(l2);
        localIMSCNative.imscRegDestroyBlocking(l2);
      }
      Log.i("IMSCNetwork", "deRegisterAllClients and notifyNetworkDisConnected");
      notifyNetworkDisConnected();
      mRegId = 0L;
    }
  }

  private String getFeatureTags()
  {
    int i = 0;
    IMSCConfReader localIMSCConfReader = IMSCConfReader.getInstance();
    if (localIMSCConfReader != null)
      i = localIMSCConfReader.getSmsFormat();
    Log.i("IMSCNetwork", "IMSCConfReader.getSmsFormat: " + i);
    int j = 0;
    String str;
    if (i == 0)
    {
      j = 0;
      str = "";
      if (j != 0)
        break label94;
      str = "+g.3gpp2.smsip";
    }
    while (true)
    {
      return str.trim();
      if (i == 1)
      {
        j = 1;
        break;
      }
      if (i != 2)
        break;
      j = 2;
      break;
      label94: if (1 != j)
        continue;
      str = "+g.3gpp.smsip";
    }
  }

  /** @deprecated */
  public static IMSCNetwork getInstance()
  {
    monitorenter;
    try
    {
      if (mSingletonInstance == null)
        mSingletonInstance = new IMSCNetwork();
      IMSCNetwork localIMSCNetwork = mSingletonInstance;
      monitorexit;
      return localIMSCNetwork;
    }
    finally
    {
      localObject = finally;
      monitorexit;
    }
    throw localObject;
  }

  // ERROR //
  private String getLocalIpAddr()
  {
    // Byte code:
    //   0: aconst_null
    //   1: astore_1
    //   2: aconst_null
    //   3: astore_2
    //   4: new 429	java/io/BufferedReader
    //   7: dup
    //   8: new 431	java/io/FileReader
    //   11: dup
    //   12: ldc_w 433
    //   15: invokespecial 434	java/io/FileReader:<init>	(Ljava/lang/String;)V
    //   18: sipush 256
    //   21: invokespecial 437	java/io/BufferedReader:<init>	(Ljava/io/Reader;I)V
    //   24: astore_3
    //   25: aload_3
    //   26: invokevirtual 440	java/io/BufferedReader:readLine	()Ljava/lang/String;
    //   29: astore_1
    //   30: ldc 65
    //   32: new 336	java/lang/StringBuilder
    //   35: dup
    //   36: invokespecial 337	java/lang/StringBuilder:<init>	()V
    //   39: ldc_w 442
    //   42: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   45: aload_1
    //   46: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   49: ldc_w 444
    //   52: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   55: invokevirtual 349	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   58: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   61: pop
    //   62: aload_3
    //   63: ifnull +7 -> 70
    //   66: aload_3
    //   67: invokevirtual 447	java/io/BufferedReader:close	()V
    //   70: aload_1
    //   71: invokestatic 453	android/text/TextUtils:isEmpty	(Ljava/lang/CharSequence;)Z
    //   74: ifeq +50 -> 124
    //   77: aload_0
    //   78: getfield 155	com/motorola/android/server/ims/IMSCNetwork:mlocalAddr	Ljava/net/InetAddress;
    //   81: ifnull +43 -> 124
    //   84: aload_0
    //   85: getfield 155	com/motorola/android/server/ims/IMSCNetwork:mlocalAddr	Ljava/net/InetAddress;
    //   88: invokevirtual 458	java/net/InetAddress:getHostAddress	()Ljava/lang/String;
    //   91: astore_1
    //   92: ldc 65
    //   94: new 336	java/lang/StringBuilder
    //   97: dup
    //   98: invokespecial 337	java/lang/StringBuilder:<init>	()V
    //   101: ldc_w 460
    //   104: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   107: aload_1
    //   108: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   111: ldc_w 444
    //   114: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   117: invokevirtual 349	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   120: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   123: pop
    //   124: aload_1
    //   125: invokestatic 453	android/text/TextUtils:isEmpty	(Ljava/lang/CharSequence;)Z
    //   128: ifeq +16 -> 144
    //   131: ldc 65
    //   133: ldc_w 462
    //   136: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   139: pop
    //   140: ldc_w 464
    //   143: astore_1
    //   144: aload_1
    //   145: areturn
    //   146: astore 13
    //   148: goto -78 -> 70
    //   151: astore 15
    //   153: aload_2
    //   154: ifnull -84 -> 70
    //   157: aload_2
    //   158: invokevirtual 447	java/io/BufferedReader:close	()V
    //   161: goto -91 -> 70
    //   164: astore 5
    //   166: goto -96 -> 70
    //   169: astore 14
    //   171: aload_2
    //   172: ifnull -102 -> 70
    //   175: aload_2
    //   176: invokevirtual 447	java/io/BufferedReader:close	()V
    //   179: goto -109 -> 70
    //   182: astore 9
    //   184: goto -114 -> 70
    //   187: astore 10
    //   189: aload_2
    //   190: ifnull +7 -> 197
    //   193: aload_2
    //   194: invokevirtual 447	java/io/BufferedReader:close	()V
    //   197: aload 10
    //   199: athrow
    //   200: astore 11
    //   202: goto -5 -> 197
    //   205: astore 10
    //   207: aload_3
    //   208: astore_2
    //   209: goto -20 -> 189
    //   212: astore 8
    //   214: aload_3
    //   215: astore_2
    //   216: goto -45 -> 171
    //   219: astore 4
    //   221: aload_3
    //   222: astore_2
    //   223: goto -70 -> 153
    //
    // Exception table:
    //   from	to	target	type
    //   66	70	146	java/io/IOException
    //   4	25	151	java/io/FileNotFoundException
    //   157	161	164	java/io/IOException
    //   4	25	169	java/io/IOException
    //   175	179	182	java/io/IOException
    //   4	25	187	finally
    //   193	197	200	java/io/IOException
    //   25	62	205	finally
    //   25	62	212	java/io/IOException
    //   25	62	219	java/io/FileNotFoundException
  }

  private String getPcscf(String paramString)
  {
    int i = 0;
    Object localObject = null;
    try
    {
      InetAddress localInetAddress = InetAddress.getByName(paramString);
      localObject = localInetAddress;
      if (localObject != null)
      {
        Log.i("IMSCNetwork", "inetAddr != null");
        if ((localObject instanceof Inet6Address))
        {
          Log.i("IMSCNetwork", "inetAddr is IPv6");
          i = 1;
        }
      }
      if (i != 0)
      {
        str = readPcscf("/data/misc/ril/ims_ipv6_pcscf_address.txt");
        Log.i("IMSCNetwork", "getPcscf, /data/misc/ril/ims_ipv6_pcscf_address.txt:" + str);
        if (str == null)
        {
          str = SystemProperties.get("net.pcscf2");
          Log.i("IMSCNetwork", "getPcscf, net.pcscf2:" + str);
        }
        return str;
      }
    }
    catch (UnknownHostException localUnknownHostException)
    {
      while (true)
      {
        Log.i("IMSCNetwork", "UnknownHostException: " + paramString + " Exception:" + localUnknownHostException);
        continue;
        String str = readPcscf("/data/misc/ril/ims_ipv4_pcscf_address.txt");
        Log.i("IMSCNetwork", "/data/misc/ril/ims_ipv4_pcscf_address.txt:" + str);
        if (str != null)
          continue;
        str = SystemProperties.get("net.pcscf1");
        Log.i("IMSCNetwork", "getPcscf, net.pcscf1:" + str);
      }
    }
  }

  private String getPublicIdentity()
  {
    String[] arrayOfString = null;
    IMSCConfReader localIMSCConfReader = IMSCConfReader.getInstance();
    if (localIMSCConfReader != null)
      arrayOfString = localIMSCConfReader.getPublicIdentity();
    String str;
    if (arrayOfString != null)
      if (arrayOfString.length >= 2)
      {
        mPublicURI_IMSI = arrayOfString[0];
        Log.i("IMSCNetwork", "mPublicURI_IMSI is read: " + arrayOfString[0]);
        mPublicURI_MSISDN = arrayOfString[1];
        Log.i("IMSCNetwork", "mPublicURI_MSISDN is read: " + arrayOfString[1]);
        str = null;
        if (!isInternalTest())
          break label229;
        str = mPublicURI_MSISDN;
        this.mIsImsiUri = false;
        Log.i("IMSCNetwork", "For internal test, MSISDN based URI will be set: " + mPublicURI_MSISDN);
      }
    while (true)
    {
      return str;
      if (arrayOfString.length == 1)
      {
        mPublicURI_IMSI = arrayOfString[0];
        Log.i("IMSCNetwork", "mPublicURI_IMSI is read: " + arrayOfString[0]);
        break;
      }
      Log.e("IMSCNetwork", "IMSCConfReader.getPublicIdentity returns array length not enough: " + arrayOfString.length);
      break;
      Log.e("IMSCNetwork", "IMSCConfReader.getPublicIdentity returns null, the default value will be used");
      break;
      label229: if ((arrayOfString != null) && (arrayOfString.length >= 2) && (isMsisdnUriValid()))
      {
        str = mPublicURI_MSISDN;
        this.mIsImsiUri = false;
        Log.i("IMSCNetwork", "MSISDN based URI will be set: " + mPublicURI_MSISDN);
        continue;
      }
      if (!TextUtils.isEmpty(mPublicURI_IMSI))
      {
        str = mPublicURI_IMSI;
        this.mIsImsiUri = true;
        Log.i("IMSCNetwork", "IMSI based URI will be set: " + mPublicURI_IMSI);
        continue;
      }
      Log.i("IMSCNetwork", "All the URIs are empty");
    }
  }

  private String getPublicURI(long paramLong)
  {
    String str = null;
    IMSCNative localIMSCNative = IMSCNative.getInstance();
    if (localIMSCNative != null)
      str = localIMSCNative.imscRegGetPublicURI(paramLong);
    return str;
  }

  private boolean isImmediatelyReAttach()
  {
    if ((!this.mIs404) && (this.mReReg));
    for (int i = 1; ; i = 0)
      return i;
  }

  private boolean isImsModeValid()
  {
    IMSCConfReader localIMSCConfReader = IMSCConfReader.getInstance();
    int i;
    if (localIMSCConfReader == null)
    {
      Log.e("IMSCNetwork", "isImsModeValid, IMSCConfReader is not started");
      i = 0;
    }
    while (true)
    {
      return i;
      if (localIMSCConfReader.getTestMode() == 1)
      {
        Log.i("IMSCNetwork", "isImsModeValid, in test mode");
        i = 0;
        continue;
      }
      i = 1;
    }
  }

  private boolean isInternalTest()
  {
    boolean bool = false;
    String str = SystemProperties.get("net.ims.internaltest");
    if (str != null)
      bool = Boolean.valueOf(str).booleanValue();
    Log.d("IMSCNetwork", "isInternalTest =  " + bool);
    return bool;
  }

  private boolean isMsisdnUriValid()
  {
    String str1 = this.mTelephonyMgr.getLine1Number();
    Log.i("IMSCNetwork", "MSISDN is read: " + str1);
    int j;
    if (TextUtils.isEmpty(str1))
    {
      Log.i("IMSCNetwork", "MSISDN is empty");
      j = 0;
    }
    while (true)
    {
      return j;
      if (TextUtils.isEmpty(mPublicURI_MSISDN))
      {
        Log.i("IMSCNetwork", "MSISDN based URI is empty");
        j = 0;
        continue;
      }
      int i = mPublicURI_MSISDN.indexOf('@');
      Log.i("IMSCNetwork", "idx of @ in mPublicURI_MSISDN: " + i);
      if (i >= 10)
      {
        try
        {
          String str2 = mPublicURI_MSISDN.substring(i - 10, i);
          Log.i("IMSCNetwork", "The 10 digits in mPublicURI_MSISDN that will be compare: " + str2);
          if (str1.endsWith(str2))
          {
            Log.i("IMSCNetwork", "Match. The uri is valid");
            j = 1;
            continue;
          }
          Log.i("IMSCNetwork", "Not match. The uri is not valid");
          j = 0;
        }
        catch (IndexOutOfBoundsException localIndexOutOfBoundsException)
        {
          Log.i("IMSCNetwork", "mPublicURI_MSISDN.substring(), IndexOutOfBoundsException, idx: " + i);
          j = 0;
        }
        continue;
      }
      Log.i("IMSCNetwork", "idx of @ is not valid: " + i);
      j = 0;
    }
  }

  private void notifyNetworkConnected()
  {
    Log.i("IMSCNetwork", "notifyNetworkConnected, mRecords.size():" + this.mRecords.size());
    synchronized (this.mRecords)
    {
      int i = this.mRecords.size() - 1;
      while (true)
        if (i >= 0)
        {
          Record localRecord = (Record)this.mRecords.get(i);
          boolean bool = localRecord.connected;
          if (!bool);
          try
          {
            localRecord.callback.onConnectionStateChanged(2, this.mLimitedAccess);
            localRecord.connected = true;
            i--;
          }
          catch (RemoteException localRemoteException)
          {
            while (true)
              remove(localRecord.binder);
          }
        }
    }
    monitorexit;
  }

  private void notifyNetworkDisConnected()
  {
    Log.i("IMSCNetwork", "notifyNetworkDisConnected, mRecords.size():" + this.mRecords.size());
    synchronized (this.mRecords)
    {
      int i = this.mRecords.size() - 1;
      while (true)
        if (i >= 0)
        {
          Record localRecord = (Record)this.mRecords.get(i);
          boolean bool = localRecord.connected;
          if (bool == true);
          try
          {
            localRecord.callback.onConnectionStateChanged(1, false);
            localRecord.connected = false;
            i += -1;
          }
          catch (RemoteException localRemoteException)
          {
            while (true)
              remove(localRecord.binder);
          }
        }
    }
    monitorexit;
  }

  private void onDeRegister()
  {
    monitorenter;
    try
    {
      if (this.mState != 3)
      {
        Log.i("IMSCNetwork", "deRegister: we can not do deRegister while not in registered state.");
        monitorexit;
      }
      else
      {
        monitorexit;
        deRegister(mRegId);
        mRegId = 0L;
      }
    }
    finally
    {
      monitorexit;
    }
  }

  // ERROR //
  private boolean onDoRegister()
  {
    // Byte code:
    //   0: invokestatic 401	com/motorola/android/server/ims/IMSCConfReader:getInstance	()Lcom/motorola/android/server/ims/IMSCConfReader;
    //   3: astore_1
    //   4: aload_1
    //   5: ifnonnull +22 -> 27
    //   8: ldc 65
    //   10: ldc_w 642
    //   13: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   16: pop
    //   17: aload_0
    //   18: invokespecial 645	com/motorola/android/server/ims/IMSCNetwork:startConfReader	()V
    //   21: iconst_0
    //   22: istore 15
    //   24: iload 15
    //   26: ireturn
    //   27: aload_1
    //   28: invokevirtual 648	com/motorola/android/server/ims/IMSCConfReader:getIMSRegistrationDisable	()Z
    //   31: ifeq +18 -> 49
    //   34: ldc 65
    //   36: ldc_w 650
    //   39: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   42: pop
    //   43: iconst_0
    //   44: istore 15
    //   46: goto -22 -> 24
    //   49: aload_0
    //   50: monitorenter
    //   51: aload_0
    //   52: getfield 157	com/motorola/android/server/ims/IMSCNetwork:mState	I
    //   55: iconst_3
    //   56: if_icmpne +25 -> 81
    //   59: ldc 65
    //   61: ldc_w 652
    //   64: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   67: pop
    //   68: iconst_1
    //   69: istore 15
    //   71: aload_0
    //   72: monitorexit
    //   73: goto -49 -> 24
    //   76: astore_2
    //   77: aload_0
    //   78: monitorexit
    //   79: aload_2
    //   80: athrow
    //   81: aload_0
    //   82: monitorexit
    //   83: aload_0
    //   84: getfield 161	com/motorola/android/server/ims/IMSCNetwork:mIsMgrInit	Z
    //   87: ifne +18 -> 105
    //   90: ldc 65
    //   92: ldc_w 654
    //   95: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   98: pop
    //   99: iconst_0
    //   100: istore 15
    //   102: goto -78 -> 24
    //   105: invokestatic 361	com/motorola/android/server/ims/IMSCNative:getInstance	()Lcom/motorola/android/server/ims/IMSCNative;
    //   108: astore_3
    //   109: aload_3
    //   110: ifnonnull +9 -> 119
    //   113: iconst_0
    //   114: istore 15
    //   116: goto -92 -> 24
    //   119: getstatic 136	com/motorola/android/server/ims/IMSCNetwork:mRegId	J
    //   122: ldc2_w 133
    //   125: lcmp
    //   126: ifeq +39 -> 165
    //   129: aload_3
    //   130: getstatic 136	com/motorola/android/server/ims/IMSCNetwork:mRegId	J
    //   133: invokevirtual 365	com/motorola/android/server/ims/IMSCNative:imscRegGetRegistrationStatus	(J)I
    //   136: istore 38
    //   138: iconst_1
    //   139: iload 38
    //   141: if_icmpeq +9 -> 150
    //   144: iconst_2
    //   145: iload 38
    //   147: if_icmpne +18 -> 165
    //   150: ldc 65
    //   152: ldc_w 656
    //   155: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   158: pop
    //   159: iconst_0
    //   160: istore 15
    //   162: goto -138 -> 24
    //   165: aload_0
    //   166: invokespecial 328	com/motorola/android/server/ims/IMSCNetwork:isImsModeValid	()Z
    //   169: ifne +18 -> 187
    //   172: ldc 65
    //   174: ldc_w 658
    //   177: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   180: pop
    //   181: iconst_0
    //   182: istore 15
    //   184: goto -160 -> 24
    //   187: getstatic 136	com/motorola/android/server/ims/IMSCNetwork:mRegId	J
    //   190: ldc2_w 133
    //   193: lcmp
    //   194: ifne +23 -> 217
    //   197: aload_0
    //   198: invokevirtual 661	com/motorola/android/server/ims/IMSCNetwork:createRegistration	()V
    //   201: getstatic 136	com/motorola/android/server/ims/IMSCNetwork:mRegId	J
    //   204: ldc2_w 133
    //   207: lcmp
    //   208: ifne +18 -> 226
    //   211: iconst_0
    //   212: istore 15
    //   214: goto -190 -> 24
    //   217: ldc 65
    //   219: ldc_w 663
    //   222: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   225: pop
    //   226: aload_3
    //   227: getstatic 136	com/motorola/android/server/ims/IMSCNetwork:mRegId	J
    //   230: iconst_1
    //   231: invokevirtual 667	com/motorola/android/server/ims/IMSCNative:imscRegSetInterface	(JI)Z
    //   234: pop
    //   235: aload_0
    //   236: invokespecial 669	com/motorola/android/server/ims/IMSCNetwork:getFeatureTags	()Ljava/lang/String;
    //   239: astore 6
    //   241: aload 6
    //   243: invokestatic 453	android/text/TextUtils:isEmpty	(Ljava/lang/CharSequence;)Z
    //   246: ifne +13 -> 259
    //   249: aload_3
    //   250: getstatic 136	com/motorola/android/server/ims/IMSCNetwork:mRegId	J
    //   253: aload 6
    //   255: invokevirtual 673	com/motorola/android/server/ims/IMSCNative:imscRegSetUACapabilities	(JLjava/lang/String;)Z
    //   258: pop
    //   259: aload_0
    //   260: invokespecial 675	com/motorola/android/server/ims/IMSCNetwork:getLocalIpAddr	()Ljava/lang/String;
    //   263: astore 7
    //   265: ldc 65
    //   267: new 336	java/lang/StringBuilder
    //   270: dup
    //   271: invokespecial 337	java/lang/StringBuilder:<init>	()V
    //   274: ldc_w 677
    //   277: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   280: aload 7
    //   282: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   285: ldc_w 444
    //   288: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   291: invokevirtual 349	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   294: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   297: pop
    //   298: aconst_null
    //   299: astore 9
    //   301: aload 7
    //   303: invokestatic 472	java/net/InetAddress:getByName	(Ljava/lang/String;)Ljava/net/InetAddress;
    //   306: astore 35
    //   308: aload 35
    //   310: astore 9
    //   312: aload 9
    //   314: ifnull +155 -> 469
    //   317: aload 9
    //   319: invokevirtual 680	java/net/InetAddress:isLinkLocalAddress	()Z
    //   322: ifeq +147 -> 469
    //   325: ldc 65
    //   327: ldc_w 682
    //   330: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   333: pop
    //   334: new 684	android/content/Intent
    //   337: dup
    //   338: ldc_w 686
    //   341: invokespecial 687	android/content/Intent:<init>	(Ljava/lang/String;)V
    //   344: astore 30
    //   346: aload 30
    //   348: ldc_w 689
    //   351: iconst_5
    //   352: invokevirtual 693	android/content/Intent:putExtra	(Ljava/lang/String;I)Landroid/content/Intent;
    //   355: pop
    //   356: aload 30
    //   358: ldc_w 695
    //   361: iconst_0
    //   362: invokevirtual 698	android/content/Intent:putExtra	(Ljava/lang/String;Z)Landroid/content/Intent;
    //   365: pop
    //   366: getstatic 277	com/motorola/android/server/ims/IMSCNetwork:mContext	Landroid/content/Context;
    //   369: aload 30
    //   371: invokevirtual 704	android/content/Context:sendBroadcast	(Landroid/content/Intent;)V
    //   374: aload_0
    //   375: monitorenter
    //   376: aload_0
    //   377: getfield 157	com/motorola/android/server/ims/IMSCNetwork:mState	I
    //   380: ifeq +31 -> 411
    //   383: aload_0
    //   384: getfield 157	com/motorola/android/server/ims/IMSCNetwork:mState	I
    //   387: iconst_4
    //   388: if_icmpeq +23 -> 411
    //   391: aload_0
    //   392: getfield 236	com/motorola/android/server/ims/IMSCNetwork:mServiceHandler	Lcom/motorola/android/server/ims/IMSCNetwork$ServiceHandler;
    //   395: aload_0
    //   396: getfield 236	com/motorola/android/server/ims/IMSCNetwork:mServiceHandler	Lcom/motorola/android/server/ims/IMSCNetwork$ServiceHandler;
    //   399: bipush 11
    //   401: invokevirtual 708	com/motorola/android/server/ims/IMSCNetwork$ServiceHandler:obtainMessage	(I)Landroid/os/Message;
    //   404: ldc2_w 709
    //   407: invokevirtual 714	com/motorola/android/server/ims/IMSCNetwork$ServiceHandler:sendMessageDelayed	(Landroid/os/Message;J)Z
    //   410: pop
    //   411: aload_0
    //   412: monitorexit
    //   413: iconst_0
    //   414: istore 15
    //   416: goto -392 -> 24
    //   419: astore 10
    //   421: ldc 65
    //   423: new 336	java/lang/StringBuilder
    //   426: dup
    //   427: invokespecial 337	java/lang/StringBuilder:<init>	()V
    //   430: ldc_w 496
    //   433: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   436: aload 7
    //   438: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   441: ldc_w 498
    //   444: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   447: aload 10
    //   449: invokevirtual 501	java/lang/StringBuilder:append	(Ljava/lang/Object;)Ljava/lang/StringBuilder;
    //   452: invokevirtual 349	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   455: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   458: pop
    //   459: goto -147 -> 312
    //   462: astore 33
    //   464: aload_0
    //   465: monitorexit
    //   466: aload 33
    //   468: athrow
    //   469: aload 7
    //   471: invokestatic 453	android/text/TextUtils:isEmpty	(Ljava/lang/CharSequence;)Z
    //   474: ifne +10 -> 484
    //   477: aload_3
    //   478: aload 7
    //   480: invokevirtual 717	com/motorola/android/server/ims/IMSCNative:imscMgrSetLocalIpAddr	(Ljava/lang/String;)Z
    //   483: pop
    //   484: aload_0
    //   485: getfield 175	com/motorola/android/server/ims/IMSCNetwork:mIsUiccRefreshInProgress	Z
    //   488: ifeq +9 -> 497
    //   491: iconst_0
    //   492: istore 15
    //   494: goto -470 -> 24
    //   497: aload_0
    //   498: aload 7
    //   500: invokespecial 719	com/motorola/android/server/ims/IMSCNetwork:getPcscf	(Ljava/lang/String;)Ljava/lang/String;
    //   503: astore 12
    //   505: ldc 65
    //   507: new 336	java/lang/StringBuilder
    //   510: dup
    //   511: invokespecial 337	java/lang/StringBuilder:<init>	()V
    //   514: ldc_w 721
    //   517: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   520: getstatic 136	com/motorola/android/server/ims/IMSCNetwork:mRegId	J
    //   523: invokevirtual 385	java/lang/StringBuilder:append	(J)Ljava/lang/StringBuilder;
    //   526: ldc_w 723
    //   529: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   532: aload 12
    //   534: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   537: invokevirtual 349	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   540: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   543: pop
    //   544: aload 12
    //   546: invokestatic 453	android/text/TextUtils:isEmpty	(Ljava/lang/CharSequence;)Z
    //   549: ifne +178 -> 727
    //   552: aload_3
    //   553: getstatic 136	com/motorola/android/server/ims/IMSCNetwork:mRegId	J
    //   556: aload 12
    //   558: invokevirtual 726	com/motorola/android/server/ims/IMSCNative:imscRegSetPCSCF	(JLjava/lang/String;)Z
    //   561: pop
    //   562: aload 12
    //   564: ldc_w 728
    //   567: invokevirtual 732	java/lang/String:split	(Ljava/lang/String;)[Ljava/lang/String;
    //   570: astore 18
    //   572: aconst_null
    //   573: astore 19
    //   575: aload 18
    //   577: arraylength
    //   578: istore 20
    //   580: iconst_0
    //   581: istore 21
    //   583: iload 21
    //   585: iload 20
    //   587: if_icmpge +140 -> 727
    //   590: aload 18
    //   592: iload 21
    //   594: aaload
    //   595: invokestatic 472	java/net/InetAddress:getByName	(Ljava/lang/String;)Ljava/net/InetAddress;
    //   598: astore 27
    //   600: aload 27
    //   602: astore 19
    //   604: aload 19
    //   606: ifnull +109 -> 715
    //   609: aload_0
    //   610: getfield 147	com/motorola/android/server/ims/IMSCNetwork:mConnMgr	Landroid/net/ConnectivityManager;
    //   613: bipush 32
    //   615: aload 19
    //   617: iconst_0
    //   618: invokevirtual 738	android/net/ConnectivityManager:requestRouteToHostAddress	(ILjava/net/InetAddress;I)Z
    //   621: ifeq +82 -> 703
    //   624: ldc 65
    //   626: new 336	java/lang/StringBuilder
    //   629: dup
    //   630: invokespecial 337	java/lang/StringBuilder:<init>	()V
    //   633: ldc_w 740
    //   636: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   639: aload 18
    //   641: iload 21
    //   643: aaload
    //   644: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   647: invokevirtual 349	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   650: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   653: pop
    //   654: iinc 21 1
    //   657: goto -74 -> 583
    //   660: astore 22
    //   662: ldc 65
    //   664: new 336	java/lang/StringBuilder
    //   667: dup
    //   668: invokespecial 337	java/lang/StringBuilder:<init>	()V
    //   671: ldc_w 496
    //   674: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   677: aload 12
    //   679: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   682: ldc_w 498
    //   685: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   688: aload 22
    //   690: invokevirtual 501	java/lang/StringBuilder:append	(Ljava/lang/Object;)Ljava/lang/StringBuilder;
    //   693: invokevirtual 349	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   696: invokestatic 524	android/util/Log:e	(Ljava/lang/String;Ljava/lang/String;)I
    //   699: pop
    //   700: goto -96 -> 604
    //   703: ldc 65
    //   705: ldc_w 742
    //   708: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   711: pop
    //   712: goto -58 -> 654
    //   715: ldc 65
    //   717: ldc_w 744
    //   720: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   723: pop
    //   724: goto -70 -> 654
    //   727: aload_0
    //   728: invokespecial 746	com/motorola/android/server/ims/IMSCNetwork:getPublicIdentity	()Ljava/lang/String;
    //   731: astore 14
    //   733: aload 14
    //   735: invokestatic 453	android/text/TextUtils:isEmpty	(Ljava/lang/CharSequence;)Z
    //   738: ifne +13 -> 751
    //   741: aload_3
    //   742: getstatic 136	com/motorola/android/server/ims/IMSCNetwork:mRegId	J
    //   745: aload 14
    //   747: invokevirtual 749	com/motorola/android/server/ims/IMSCNative:imscRegSetPublicURI	(JLjava/lang/String;)Z
    //   750: pop
    //   751: aload_0
    //   752: iconst_1
    //   753: putfield 177	com/motorola/android/server/ims/IMSCNetwork:mIsRegistering	Z
    //   756: aload_0
    //   757: getstatic 136	com/motorola/android/server/ims/IMSCNetwork:mRegId	J
    //   760: invokespecial 267	com/motorola/android/server/ims/IMSCNetwork:register	(J)V
    //   763: iconst_1
    //   764: istore 15
    //   766: goto -742 -> 24
    //
    // Exception table:
    //   from	to	target	type
    //   51	79	76	finally
    //   81	83	76	finally
    //   301	308	419	java/net/UnknownHostException
    //   376	413	462	finally
    //   464	466	462	finally
    //   590	600	660	java/net/UnknownHostException
  }

  private void onStartConfReader()
  {
    IMSCConfReader.start(mContext, this.mServiceHandler);
  }

  private void processImsMgrInitFailed()
  {
    this.mMgrInitTryTimes = (1 + this.mMgrInitTryTimes);
    if ((this.mMgrInitTryTimes <= 10) && (this.mAlarmManager != null))
    {
      int i = 30 * (1 << this.mMgrInitTryTimes - 1);
      if (i > 600)
        i = 600;
      Log.e("IMSCNetwork", "processImsMgrInitFailed, try again after " + i + " seconds.");
      long l = System.currentTimeMillis() + i * 1000;
      this.mAlarmManager.set(0, l, this.mMgrInitIntent);
    }
    IMSCNative localIMSCNative = IMSCNative.getInstance();
    if (localIMSCNative != null)
      localIMSCNative.imscMgrDeInit();
  }

  // ERROR //
  private String readPcscf(String paramString)
  {
    // Byte code:
    //   0: aconst_null
    //   1: astore_2
    //   2: aconst_null
    //   3: astore_3
    //   4: new 429	java/io/BufferedReader
    //   7: dup
    //   8: new 431	java/io/FileReader
    //   11: dup
    //   12: aload_1
    //   13: invokespecial 434	java/io/FileReader:<init>	(Ljava/lang/String;)V
    //   16: sipush 1024
    //   19: invokespecial 437	java/io/BufferedReader:<init>	(Ljava/io/Reader;I)V
    //   22: astore 4
    //   24: aload 4
    //   26: invokevirtual 440	java/io/BufferedReader:readLine	()Ljava/lang/String;
    //   29: astore 11
    //   31: aload 11
    //   33: astore_2
    //   34: aload 4
    //   36: ifnull +8 -> 44
    //   39: aload 4
    //   41: invokevirtual 447	java/io/BufferedReader:close	()V
    //   44: aload_2
    //   45: areturn
    //   46: astore 12
    //   48: goto -4 -> 44
    //   51: astore 13
    //   53: aload 13
    //   55: astore 6
    //   57: ldc 65
    //   59: new 336	java/lang/StringBuilder
    //   62: dup
    //   63: invokespecial 337	java/lang/StringBuilder:<init>	()V
    //   66: ldc_w 774
    //   69: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   72: aload_1
    //   73: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   76: ldc_w 776
    //   79: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   82: aload 6
    //   84: invokevirtual 501	java/lang/StringBuilder:append	(Ljava/lang/Object;)Ljava/lang/StringBuilder;
    //   87: invokevirtual 349	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   90: invokestatic 524	android/util/Log:e	(Ljava/lang/String;Ljava/lang/String;)I
    //   93: pop
    //   94: aload_3
    //   95: ifnull -51 -> 44
    //   98: aload_3
    //   99: invokevirtual 447	java/io/BufferedReader:close	()V
    //   102: goto -58 -> 44
    //   105: astore 10
    //   107: goto -63 -> 44
    //   110: astore 7
    //   112: aload_3
    //   113: ifnull +7 -> 120
    //   116: aload_3
    //   117: invokevirtual 447	java/io/BufferedReader:close	()V
    //   120: aload 7
    //   122: athrow
    //   123: astore 8
    //   125: goto -5 -> 120
    //   128: astore 7
    //   130: aload 4
    //   132: astore_3
    //   133: goto -21 -> 112
    //   136: astore 5
    //   138: aload 5
    //   140: astore 6
    //   142: aload 4
    //   144: astore_3
    //   145: goto -88 -> 57
    //
    // Exception table:
    //   from	to	target	type
    //   39	44	46	java/io/IOException
    //   4	24	51	java/lang/Exception
    //   98	102	105	java/io/IOException
    //   4	24	110	finally
    //   57	94	110	finally
    //   116	120	123	java/io/IOException
    //   24	31	128	finally
    //   24	31	136	java/lang/Exception
  }

  private void register(long paramLong)
  {
    IMSCNative localIMSCNative = IMSCNative.getInstance();
    if (localIMSCNative == null);
    while (true)
    {
      return;
      localIMSCNative.imscRegRegister(paramLong);
    }
  }

  private void remove(IBinder paramIBinder)
  {
    Log.i("IMSCNetwork", "remove, remove Binder");
    while (true)
    {
      int j;
      synchronized (this.mRecords)
      {
        int i = this.mRecords.size();
        Log.i("IMSCNetwork", "remove, recordCount:" + i);
        j = 0;
        if (j >= i)
          continue;
        if (((Record)this.mRecords.get(j)).binder != paramIBinder)
          break label109;
        this.mRecords.remove(j);
      }
      return;
      label109: j++;
    }
  }

  private void restoreAllClients()
  {
    checkAndStartDataConnection();
  }

  private void retryImsConnAfterDelay(int paramInt)
  {
    int i;
    if (paramInt == 1)
    {
      i = 1;
      Log.i("IMSCNetwork", "retryImsConnAfterDelay(), delay time = " + i + "seconds");
      if (i >= 15)
        break label135;
      this.mServiceHandler.sendMessageDelayed(this.mServiceHandler.obtainMessage(16), i * 1000);
      this.mDataRetryWakeLock.acquire(i * 1000);
    }
    while (true)
    {
      return;
      if (paramInt == 10)
      {
        i = 10;
        break;
      }
      if (paramInt == 2)
      {
        i = 30;
        break;
      }
      if (paramInt == 3)
      {
        i = 900;
        break;
      }
      i = this.mRetryMgr.getRetryTimer();
      this.mRetryMgr.increaseRetryCount();
      break;
      label135: if (this.mAlarmManager == null)
        continue;
      this.mAlarmManager.set(2, SystemClock.elapsedRealtime() + i * 1000, this.mLinkRetryTimeoutIntent);
    }
  }

  private void startConfReader()
  {
    this.mServiceHandler.sendMessage(this.mServiceHandler.obtainMessage(14));
  }

  // ERROR //
  private void startDataConnection()
  {
    // Byte code:
    //   0: aload_0
    //   1: monitorenter
    //   2: ldc 65
    //   4: new 336	java/lang/StringBuilder
    //   7: dup
    //   8: invokespecial 337	java/lang/StringBuilder:<init>	()V
    //   11: ldc_w 817
    //   14: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   17: aload_0
    //   18: getfield 143	com/motorola/android/server/ims/IMSCNetwork:dataEnabled	Z
    //   21: invokevirtual 564	java/lang/StringBuilder:append	(Z)Ljava/lang/StringBuilder;
    //   24: ldc_w 819
    //   27: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   30: aload_0
    //   31: getfield 157	com/motorola/android/server/ims/IMSCNetwork:mState	I
    //   34: invokevirtual 346	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
    //   37: invokevirtual 349	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   40: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   43: pop
    //   44: aload_0
    //   45: getfield 143	com/motorola/android/server/ims/IMSCNetwork:dataEnabled	Z
    //   48: ifeq +10 -> 58
    //   51: aload_0
    //   52: getfield 157	com/motorola/android/server/ims/IMSCNetwork:mState	I
    //   55: ifeq +8 -> 63
    //   58: aload_0
    //   59: monitorexit
    //   60: goto +164 -> 224
    //   63: aload_0
    //   64: getfield 147	com/motorola/android/server/ims/IMSCNetwork:mConnMgr	Landroid/net/ConnectivityManager;
    //   67: iconst_0
    //   68: ldc_w 821
    //   71: invokevirtual 825	android/net/ConnectivityManager:startUsingNetworkFeature	(ILjava/lang/String;)I
    //   74: istore_3
    //   75: iload_3
    //   76: tableswitch	default:+24 -> 100, 0:+72->148, 1:+122->198
    //   101: lstore_2
    //   102: new 336	java/lang/StringBuilder
    //   105: dup
    //   106: invokespecial 337	java/lang/StringBuilder:<init>	()V
    //   109: ldc_w 827
    //   112: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   115: iload_3
    //   116: invokevirtual 346	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
    //   119: invokevirtual 349	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   122: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   125: pop
    //   126: aload_0
    //   127: getfield 179	com/motorola/android/server/ims/IMSCNetwork:mNetworkTerminationReqReceivedBefore	Z
    //   130: ifeq +85 -> 215
    //   133: aload_0
    //   134: iconst_3
    //   135: invokespecial 829	com/motorola/android/server/ims/IMSCNetwork:retryImsConnAfterDelay	(I)V
    //   138: aload_0
    //   139: monitorexit
    //   140: goto +84 -> 224
    //   143: astore_1
    //   144: aload_0
    //   145: monitorexit
    //   146: aload_1
    //   147: athrow
    //   148: ldc 65
    //   150: ldc_w 831
    //   153: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   156: pop
    //   157: aload_0
    //   158: getfield 157	com/motorola/android/server/ims/IMSCNetwork:mState	I
    //   161: iconst_3
    //   162: if_icmpne +15 -> 177
    //   165: ldc 65
    //   167: ldc_w 833
    //   170: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   173: pop
    //   174: goto -36 -> 138
    //   177: aload_0
    //   178: iconst_2
    //   179: putfield 157	com/motorola/android/server/ims/IMSCNetwork:mState	I
    //   182: ldc 65
    //   184: ldc_w 835
    //   187: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   190: pop
    //   191: aload_0
    //   192: invokevirtual 325	com/motorola/android/server/ims/IMSCNetwork:doRegister	()V
    //   195: goto -57 -> 138
    //   198: aload_0
    //   199: iconst_1
    //   200: putfield 157	com/motorola/android/server/ims/IMSCNetwork:mState	I
    //   203: ldc 65
    //   205: ldc_w 837
    //   208: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   211: pop
    //   212: goto -74 -> 138
    //   215: aload_0
    //   216: bipush 10
    //   218: invokespecial 829	com/motorola/android/server/ims/IMSCNetwork:retryImsConnAfterDelay	(I)V
    //   221: goto -83 -> 138
    //   224: return
    //
    // Exception table:
    //   from	to	target	type
    //   2	146	143	finally
    //   148	221	143	finally
  }

  private void stopConfReader()
  {
    IMSCConfReader localIMSCConfReader = IMSCConfReader.getInstance();
    if (localIMSCConfReader != null)
      localIMSCConfReader.stop();
  }

  // ERROR //
  private void stopDataConnection()
  {
    // Byte code:
    //   0: aload_0
    //   1: monitorenter
    //   2: ldc 65
    //   4: new 336	java/lang/StringBuilder
    //   7: dup
    //   8: invokespecial 337	java/lang/StringBuilder:<init>	()V
    //   11: ldc_w 843
    //   14: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   17: aload_0
    //   18: getfield 157	com/motorola/android/server/ims/IMSCNetwork:mState	I
    //   21: invokevirtual 346	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
    //   24: invokevirtual 349	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   27: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   30: pop
    //   31: aload_0
    //   32: getfield 157	com/motorola/android/server/ims/IMSCNetwork:mState	I
    //   35: ifne +8 -> 43
    //   38: aload_0
    //   39: monitorexit
    //   40: goto +41 -> 81
    //   43: aload_0
    //   44: getfield 147	com/motorola/android/server/ims/IMSCNetwork:mConnMgr	Landroid/net/ConnectivityManager;
    //   47: iconst_0
    //   48: ldc_w 821
    //   51: invokevirtual 846	android/net/ConnectivityManager:stopUsingNetworkFeature	(ILjava/lang/String;)I
    //   54: iconst_1
    //   55: if_icmpne +18 -> 73
    //   58: aload_0
    //   59: iconst_4
    //   60: putfield 157	com/motorola/android/server/ims/IMSCNetwork:mState	I
    //   63: aload_0
    //   64: monitorexit
    //   65: goto +16 -> 81
    //   68: astore_1
    //   69: aload_0
    //   70: monitorexit
    //   71: aload_1
    //   72: athrow
    //   73: aload_0
    //   74: iconst_0
    //   75: putfield 157	com/motorola/android/server/ims/IMSCNetwork:mState	I
    //   78: goto -15 -> 63
    //   81: return
    //
    // Exception table:
    //   from	to	target	type
    //   2	71	68	finally
    //   73	78	68	finally
  }

  public void createRegistration()
  {
    IMSCNative localIMSCNative = IMSCNative.getInstance();
    if (localIMSCNative != null)
    {
      mRegId = localIMSCNative.imscRegCreate();
      if (0L != mRegId)
        break label35;
      Log.i("IMSCNetwork", "createRegistration, mRegId is invalid");
    }
    while (true)
    {
      return;
      label35: localIMSCNative.imscSetRegistrationCallback(mRegId, this.mCallBack);
    }
  }

  public void deRegister()
  {
    this.mServiceHandler.sendMessage(this.mServiceHandler.obtainMessage(13));
  }

  public void doRegister()
  {
    this.mServiceHandler.removeMessages(12);
    this.mServiceHandler.sendMessage(this.mServiceHandler.obtainMessage(12));
  }

  public long getImsRegId()
  {
    return mRegId;
  }

  public boolean isImsRegistered()
  {
    monitorenter;
    while (true)
    {
      try
      {
        if (this.mState == 3)
        {
          i = 1;
          monitorexit;
          return i;
        }
      }
      finally
      {
        localObject = finally;
        monitorexit;
        throw localObject;
      }
      int i = 0;
    }
  }

  public boolean isLimitedAccessMode()
  {
    return this.mLimitedAccess;
  }

  public void onAirplaneModeChanged(boolean paramBoolean)
  {
    Log.i("IMSCNetwork", "onAirplaneModeChanged: " + paramBoolean);
    if (paramBoolean == true)
      deRegisterAllClients();
    while (true)
    {
      return;
      restoreAllClients();
    }
  }

  public void onConnectivityChanged(Intent paramIntent)
  {
    NetworkInfo localNetworkInfo = (NetworkInfo)paramIntent.getParcelableExtra("networkInfo");
    String str1 = paramIntent.getStringExtra("reason");
    StringBuilder localStringBuilder1 = new StringBuilder().append("onConnectivityChanged(), info = ").append(localNetworkInfo).append(" reason = ");
    String str2;
    if (str1 == null)
    {
      str2 = "[none]";
      Log.i("IMSCNetwork", str2);
      if (localNetworkInfo != null)
        break label89;
      Log.i("IMSCNetwork", "onConnectivityChanged(): NetworkInfo is null");
    }
    while (true)
    {
      return;
      str2 = str1;
      break;
      label89: if (isInternalTest())
      {
        Log.i("IMSCNetwork", "onConnectivityChanged(): internal test");
        continue;
      }
      if (localNetworkInfo.getType() != 32)
      {
        Log.i("IMSCNetwork", "onConnectivityChanged(): Type: " + localNetworkInfo.getType() + " is not TYPE_MOBILE_IMS");
        continue;
      }
      if (localNetworkInfo.isConnected())
      {
        Log.i("IMSCNetwork", "onConnectivityChanged(), isConnected.");
        NetworkInfoOem localNetworkInfoOem = this.mConnMgr.getNetworkInfoOem("enableIMS");
        InetAddress[] arrayOfInetAddress;
        if (localNetworkInfoOem != null)
        {
          arrayOfInetAddress = localNetworkInfoOem.getAddress();
          if ((arrayOfInetAddress == null) || (arrayOfInetAddress.length == 0))
          {
            Log.i("IMSCNetwork", "onConnectivityChanged(): addr is empty");
            label215: monitorenter;
          }
        }
        try
        {
          switch (this.mState)
          {
          default:
          case 1:
          case 2:
          }
          while (true)
          {
            monitorexit;
            this.mNetworkTerminationReqReceivedBefore = false;
            break;
            this.mlocalAddr = arrayOfInetAddress[0];
            StringBuilder localStringBuilder2 = new StringBuilder().append("onConnectivityChanged(): mlocalAddr = ");
            if (this.mlocalAddr == null);
            for (String str3 = "null"; ; str3 = this.mlocalAddr.getHostAddress())
            {
              Log.i("IMSCNetwork", str3);
              break;
            }
            Log.i("IMSCNetwork", "onConnectivityChanged(): infoOem is null");
            break label215;
            this.mState = 2;
            Log.i("IMSCNetwork", "onConnectivityChanged: STATE_CONN_READY");
            doRegister();
          }
        }
        finally
        {
          monitorexit;
        }
      }
      if (localNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.FAILED)
      {
        Log.i("IMSCNetwork", "onConnectivityChanged(), set connection failed.");
        monitorenter;
        try
        {
          this.mState = 0;
          monitorexit;
          Log.i("IMSCNetwork", "onConnectivityChanged: STATE_IDLE");
          this.mServiceHandler.removeMessages(11);
          if (!this.dataEnabled)
          {
            Log.i("IMSCNetwork", "onConnectivityChanged(), the dataEnabled is false.");
            continue;
          }
        }
        finally
        {
          monitorexit;
        }
        if (((str1 != null) && ((str1.equals("LCP configuration")) || (str1.equals("AKA authentication")) || (str1.equals("VSNCP configuration")) || (str1.equals("no IPv6 address")))) || (this.mNetworkTerminationReqReceivedBefore == true))
        {
          retryImsConnAfterDelay(3);
          continue;
        }
        retryImsConnAfterDelay(0);
        continue;
      }
      if ((localNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.DISCONNECTED) || (localNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.IDLE))
      {
        Log.i("IMSCNetwork", "onConnectivityChanged(), detailedState is IDLE or DISCONNECTED");
        monitorenter;
        try
        {
          this.mState = 0;
          monitorexit;
          this.mServiceHandler.removeMessages(16);
          this.mAlarmManager.cancel(this.mLinkRetryTimeoutIntent);
          this.mServiceHandler.removeMessages(11);
          this.mRetryMgr.resetRetryCount();
          if (this.mIsMgrInit == true)
          {
            if (this.mIMSConn == null)
              this.mIMSConn = new IMSCConnectMgr(mContext);
            this.mIMSConn.notifyConnectionStatus(1, 2);
          }
          if ((str1 != null) && (str1.equals("imsPdnNwInitiated")) && (!this.mNetworkTerminationReqReceivedBefore))
            this.mNetworkTerminationReqReceivedBefore = true;
          if (!this.dataEnabled)
          {
            Log.i("IMSCNetwork", "onConnectivityChanged(), the dataEnabled is false.");
            continue;
          }
        }
        finally
        {
          monitorexit;
        }
        if (isImmediatelyReAttach())
        {
          Log.i("IMSCNetwork", "onConnectivityChanged(), isImmediatelyReAttach.");
          checkAndStartDataConnection();
          continue;
        }
        if ((this.mNetworkType != 13) && (this.mNetworkType != 14))
          continue;
        retryImsConnAfterDelay(10);
        continue;
      }
      if (localNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.SUSPENDED)
        continue;
      Log.i("IMSCNetwork", "onConnectivityChanged(), other than Fail or Disconnected");
      monitorenter;
      try
      {
        this.mState = 0;
        monitorexit;
        Log.i("IMSCNetwork", "onConnectivityChanged: STATE_IDLE");
        continue;
      }
      finally
      {
        monitorexit;
      }
    }
    throw localObject2;
  }

  public void onDestroy()
  {
    IMSCConfReader localIMSCConfReader = IMSCConfReader.getInstance();
    if (localIMSCConfReader != null)
      localIMSCConfReader.stop();
  }

  public void onImsConfigReady(String paramString)
  {
    Log.i("IMSCNetwork", "onImsConfigReady: " + paramString);
    if (!this.mIsMgrInit)
    {
      this.mConfigFile = paramString;
      this.mMgrInitTryTimes = 0;
      if (this.mAlarmManager != null)
        this.mAlarmManager.cancel(this.mMgrInitIntent);
      startImsMgrInit();
    }
    while (true)
    {
      return;
      onImsConfigUpdated();
    }
  }

  // ERROR //
  public void onImsConfigUpdated()
  {
    // Byte code:
    //   0: aload_0
    //   1: invokevirtual 1002	com/motorola/android/server/ims/IMSCNetwork:isImsRegistered	()Z
    //   4: istore_1
    //   5: ldc 65
    //   7: new 336	java/lang/StringBuilder
    //   10: dup
    //   11: invokespecial 337	java/lang/StringBuilder:<init>	()V
    //   14: ldc_w 1004
    //   17: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   20: iload_1
    //   21: invokevirtual 564	java/lang/StringBuilder:append	(Z)Ljava/lang/StringBuilder;
    //   24: invokevirtual 349	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   27: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   30: pop
    //   31: iload_1
    //   32: ifeq +7 -> 39
    //   35: aload_0
    //   36: invokespecial 867	com/motorola/android/server/ims/IMSCNetwork:deRegisterAllClients	()V
    //   39: invokestatic 361	com/motorola/android/server/ims/IMSCNative:getInstance	()Lcom/motorola/android/server/ims/IMSCNative;
    //   42: astore_3
    //   43: aload_3
    //   44: ifnull +8 -> 52
    //   47: aload_3
    //   48: invokevirtual 1007	com/motorola/android/server/ims/IMSCNative:imscMgrReloadConfig	()Z
    //   51: pop
    //   52: aload_0
    //   53: iconst_0
    //   54: putfield 175	com/motorola/android/server/ims/IMSCNetwork:mIsUiccRefreshInProgress	Z
    //   57: aload_0
    //   58: monitorenter
    //   59: aload_0
    //   60: invokespecial 328	com/motorola/android/server/ims/IMSCNetwork:isImsModeValid	()Z
    //   63: ifeq +165 -> 228
    //   66: ldc 65
    //   68: new 336	java/lang/StringBuilder
    //   71: dup
    //   72: invokespecial 337	java/lang/StringBuilder:<init>	()V
    //   75: ldc_w 1009
    //   78: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   81: aload_0
    //   82: getfield 157	com/motorola/android/server/ims/IMSCNetwork:mState	I
    //   85: invokevirtual 346	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
    //   88: invokevirtual 349	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   91: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   94: pop
    //   95: aload_0
    //   96: getfield 157	com/motorola/android/server/ims/IMSCNetwork:mState	I
    //   99: ifne +102 -> 201
    //   102: aload_0
    //   103: invokespecial 322	com/motorola/android/server/ims/IMSCNetwork:isInternalTest	()Z
    //   106: ifeq +28 -> 134
    //   109: aload_0
    //   110: getfield 236	com/motorola/android/server/ims/IMSCNetwork:mServiceHandler	Lcom/motorola/android/server/ims/IMSCNetwork$ServiceHandler;
    //   113: aload_0
    //   114: getfield 236	com/motorola/android/server/ims/IMSCNetwork:mServiceHandler	Lcom/motorola/android/server/ims/IMSCNetwork$ServiceHandler;
    //   117: bipush 12
    //   119: invokevirtual 708	com/motorola/android/server/ims/IMSCNetwork$ServiceHandler:obtainMessage	(I)Landroid/os/Message;
    //   122: ldc2_w 1010
    //   125: invokevirtual 714	com/motorola/android/server/ims/IMSCNetwork$ServiceHandler:sendMessageDelayed	(Landroid/os/Message;J)Z
    //   128: pop
    //   129: aload_0
    //   130: monitorexit
    //   131: goto +148 -> 279
    //   134: aload_0
    //   135: getfield 151	com/motorola/android/server/ims/IMSCNetwork:mTelephonyMgr	Landroid/telephony/TelephonyManager;
    //   138: invokevirtual 1014	android/telephony/TelephonyManager:getNetworkType	()I
    //   141: istore 7
    //   143: ldc 65
    //   145: new 336	java/lang/StringBuilder
    //   148: dup
    //   149: invokespecial 337	java/lang/StringBuilder:<init>	()V
    //   152: ldc_w 1016
    //   155: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   158: iload 7
    //   160: invokevirtual 346	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
    //   163: invokevirtual 349	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   166: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   169: pop
    //   170: iload 7
    //   172: bipush 13
    //   174: if_icmpeq +10 -> 184
    //   177: iload 7
    //   179: bipush 14
    //   181: if_icmpne +8 -> 189
    //   184: aload_0
    //   185: iconst_1
    //   186: invokespecial 829	com/motorola/android/server/ims/IMSCNetwork:retryImsConnAfterDelay	(I)V
    //   189: aload_0
    //   190: monitorexit
    //   191: goto +88 -> 279
    //   194: astore 4
    //   196: aload_0
    //   197: monitorexit
    //   198: aload 4
    //   200: athrow
    //   201: iload_1
    //   202: ifeq +11 -> 213
    //   205: aload_0
    //   206: iconst_1
    //   207: putfield 173	com/motorola/android/server/ims/IMSCNetwork:mNeedRegForConfigUpdate	Z
    //   210: goto -21 -> 189
    //   213: aload_0
    //   214: getfield 157	com/motorola/android/server/ims/IMSCNetwork:mState	I
    //   217: iconst_2
    //   218: if_icmpne -29 -> 189
    //   221: aload_0
    //   222: invokevirtual 325	com/motorola/android/server/ims/IMSCNetwork:doRegister	()V
    //   225: goto -36 -> 189
    //   228: ldc 65
    //   230: new 336	java/lang/StringBuilder
    //   233: dup
    //   234: invokespecial 337	java/lang/StringBuilder:<init>	()V
    //   237: ldc_w 1018
    //   240: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   243: aload_0
    //   244: getfield 157	com/motorola/android/server/ims/IMSCNetwork:mState	I
    //   247: invokevirtual 346	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
    //   250: invokevirtual 349	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   253: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   256: pop
    //   257: aload_0
    //   258: getfield 157	com/motorola/android/server/ims/IMSCNetwork:mState	I
    //   261: ifeq -72 -> 189
    //   264: aload_0
    //   265: getfield 157	com/motorola/android/server/ims/IMSCNetwork:mState	I
    //   268: iconst_4
    //   269: if_icmpeq -80 -> 189
    //   272: aload_0
    //   273: invokespecial 299	com/motorola/android/server/ims/IMSCNetwork:stopDataConnection	()V
    //   276: goto -87 -> 189
    //   279: return
    //
    // Exception table:
    //   from	to	target	type
    //   59	198	194	finally
    //   205	276	194	finally
  }

  public void onMobileDataEnabledChanged(Intent paramIntent)
  {
    this.dataEnabled = paramIntent.getBooleanExtra("mobileDataEnabled", true);
    Log.i("IMSCNetwork", "onMobileDataEnabledChanged, dataEnabled: " + this.dataEnabled);
    if (this.dataEnabled)
    {
      monitorenter;
      try
      {
        if (this.mState == 0)
          checkAndStartDataConnection();
        monitorexit;
        return;
      }
      finally
      {
        localObject = finally;
        monitorexit;
        throw localObject;
      }
    }
    else
    {
      this.mServiceHandler.removeMessages(16);
      if (this.mAlarmManager != null)
        this.mAlarmManager.cancel(this.mLinkRetryTimeoutIntent);
    }
  }

  public void onNativeTimer()
  {
    Log.i("IMSCNetwork", "onNativeTimer");
    PowerManager.WakeLock localWakeLock = this.mPowerManager.newWakeLock(1, "IMS_Lock");
    IMSCNative localIMSCNative;
    if (localWakeLock != null)
    {
      localWakeLock.acquire(1000L);
      localIMSCNative = IMSCNative.getInstance();
      if (localIMSCNative != null)
        break label41;
    }
    while (true)
    {
      return;
      label41: localIMSCNative.imscAlarmTimeout();
      this.mWakeLock = localWakeLock;
    }
  }

  // ERROR //
  public void onNetworkStateChanged()
  {
    // Byte code:
    //   0: aload_0
    //   1: getfield 151	com/motorola/android/server/ims/IMSCNetwork:mTelephonyMgr	Landroid/telephony/TelephonyManager;
    //   4: invokevirtual 1014	android/telephony/TelephonyManager:getNetworkType	()I
    //   7: istore_1
    //   8: ldc 65
    //   10: new 336	java/lang/StringBuilder
    //   13: dup
    //   14: invokespecial 337	java/lang/StringBuilder:<init>	()V
    //   17: ldc_w 1043
    //   20: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   23: iload_1
    //   24: invokevirtual 346	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
    //   27: ldc_w 1045
    //   30: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   33: aload_0
    //   34: getfield 159	com/motorola/android/server/ims/IMSCNetwork:mNetworkType	I
    //   37: invokevirtual 346	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
    //   40: invokevirtual 349	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   43: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   46: pop
    //   47: iload_1
    //   48: aload_0
    //   49: getfield 159	com/motorola/android/server/ims/IMSCNetwork:mNetworkType	I
    //   52: if_icmpne +4 -> 56
    //   55: return
    //   56: aload_0
    //   57: iload_1
    //   58: putfield 159	com/motorola/android/server/ims/IMSCNetwork:mNetworkType	I
    //   61: aload_0
    //   62: monitorenter
    //   63: aload_0
    //   64: getfield 157	com/motorola/android/server/ims/IMSCNetwork:mState	I
    //   67: ifeq +42 -> 109
    //   70: ldc 65
    //   72: new 336	java/lang/StringBuilder
    //   75: dup
    //   76: invokespecial 337	java/lang/StringBuilder:<init>	()V
    //   79: ldc_w 1047
    //   82: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   85: aload_0
    //   86: getfield 157	com/motorola/android/server/ims/IMSCNetwork:mState	I
    //   89: invokevirtual 346	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
    //   92: invokevirtual 349	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   95: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   98: pop
    //   99: aload_0
    //   100: monitorexit
    //   101: goto -46 -> 55
    //   104: astore_3
    //   105: aload_0
    //   106: monitorexit
    //   107: aload_3
    //   108: athrow
    //   109: aload_0
    //   110: monitorexit
    //   111: iload_1
    //   112: bipush 13
    //   114: if_icmpne +80 -> 194
    //   117: aload_0
    //   118: getfield 141	com/motorola/android/server/ims/IMSCNetwork:mRetryMgr	Lcom/android/internal/telephony/RetryManager;
    //   121: ifnull +21 -> 142
    //   124: ldc 65
    //   126: ldc 205
    //   128: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   131: pop
    //   132: aload_0
    //   133: getfield 141	com/motorola/android/server/ims/IMSCNetwork:mRetryMgr	Lcom/android/internal/telephony/RetryManager;
    //   136: ldc 32
    //   138: invokevirtual 215	com/android/internal/telephony/RetryManager:configure	(Ljava/lang/String;)Z
    //   141: pop
    //   142: aload_0
    //   143: getfield 167	com/motorola/android/server/ims/IMSCNetwork:mIs403	Z
    //   146: iconst_1
    //   147: if_icmpne +8 -> 155
    //   150: aload_0
    //   151: iconst_0
    //   152: putfield 167	com/motorola/android/server/ims/IMSCNetwork:mIs403	Z
    //   155: aload_0
    //   156: getfield 157	com/motorola/android/server/ims/IMSCNetwork:mState	I
    //   159: ifeq +82 -> 241
    //   162: ldc 65
    //   164: new 336	java/lang/StringBuilder
    //   167: dup
    //   168: invokespecial 337	java/lang/StringBuilder:<init>	()V
    //   171: ldc_w 1047
    //   174: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   177: aload_0
    //   178: getfield 157	com/motorola/android/server/ims/IMSCNetwork:mState	I
    //   181: invokevirtual 346	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
    //   184: invokevirtual 349	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   187: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   190: pop
    //   191: goto -136 -> 55
    //   194: iload_1
    //   195: bipush 14
    //   197: if_icmpne +32 -> 229
    //   200: aload_0
    //   201: getfield 141	com/motorola/android/server/ims/IMSCNetwork:mRetryMgr	Lcom/android/internal/telephony/RetryManager;
    //   204: ifnull -49 -> 155
    //   207: ldc 65
    //   209: ldc_w 1049
    //   212: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   215: pop
    //   216: aload_0
    //   217: getfield 141	com/motorola/android/server/ims/IMSCNetwork:mRetryMgr	Lcom/android/internal/telephony/RetryManager;
    //   220: ldc 29
    //   222: invokevirtual 215	com/android/internal/telephony/RetryManager:configure	(Ljava/lang/String;)Z
    //   225: pop
    //   226: goto -71 -> 155
    //   229: ldc 65
    //   231: ldc_w 1051
    //   234: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   237: pop
    //   238: goto -183 -> 55
    //   241: aload_0
    //   242: getfield 145	com/motorola/android/server/ims/IMSCNetwork:simReady	Z
    //   245: ifne +15 -> 260
    //   248: ldc 65
    //   250: ldc_w 1053
    //   253: invokestatic 524	android/util/Log:e	(Ljava/lang/String;Ljava/lang/String;)I
    //   256: pop
    //   257: goto -202 -> 55
    //   260: invokestatic 401	com/motorola/android/server/ims/IMSCConfReader:getInstance	()Lcom/motorola/android/server/ims/IMSCConfReader;
    //   263: ifnonnull +19 -> 282
    //   266: ldc 65
    //   268: ldc_w 1055
    //   271: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   274: pop
    //   275: aload_0
    //   276: invokespecial 645	com/motorola/android/server/ims/IMSCNetwork:startConfReader	()V
    //   279: goto -224 -> 55
    //   282: aload_0
    //   283: invokespecial 328	com/motorola/android/server/ims/IMSCNetwork:isImsModeValid	()Z
    //   286: ifne +15 -> 301
    //   289: ldc 65
    //   291: ldc_w 1057
    //   294: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   297: pop
    //   298: goto -243 -> 55
    //   301: aload_0
    //   302: getfield 236	com/motorola/android/server/ims/IMSCNetwork:mServiceHandler	Lcom/motorola/android/server/ims/IMSCNetwork$ServiceHandler;
    //   305: bipush 16
    //   307: invokevirtual 858	com/motorola/android/server/ims/IMSCNetwork$ServiceHandler:removeMessages	(I)V
    //   310: aload_0
    //   311: getfield 149	com/motorola/android/server/ims/IMSCNetwork:mAlarmManager	Landroid/app/AlarmManager;
    //   314: aload_0
    //   315: getfield 811	com/motorola/android/server/ims/IMSCNetwork:mLinkRetryTimeoutIntent	Landroid/app/PendingIntent;
    //   318: invokevirtual 965	android/app/AlarmManager:cancel	(Landroid/app/PendingIntent;)V
    //   321: aload_0
    //   322: getfield 141	com/motorola/android/server/ims/IMSCNetwork:mRetryMgr	Lcom/android/internal/telephony/RetryManager;
    //   325: invokevirtual 968	com/android/internal/telephony/RetryManager:resetRetryCount	()V
    //   328: ldc 65
    //   330: ldc_w 1059
    //   333: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   336: pop
    //   337: aload_0
    //   338: invokespecial 273	com/motorola/android/server/ims/IMSCNetwork:checkAndStartDataConnection	()V
    //   341: goto -286 -> 55
    //
    // Exception table:
    //   from	to	target	type
    //   63	107	104	finally
    //   109	111	104	finally
  }

  public void onReconnectTimer()
  {
    checkAndStartDataConnection();
  }

  public void onShutdown()
  {
    Log.i("IMSCNetwork", "onShutdown");
    deRegisterAllClients();
  }

  public void onSimNotReady()
  {
    Log.i("IMSCNetwork", "SIM is not ready");
    if (this.simReady)
    {
      this.simReady = false;
      if (IMSCConfReader.getInstance() != null)
        stopConfReader();
    }
  }

  public void onSimReady()
  {
    Log.i("IMSCNetwork", "SIM is ready");
    if (this.simReady);
    while (true)
    {
      return;
      if (!this.simReady)
      {
        this.simReady = true;
        if (IMSCConfReader.getInstance() != null)
          continue;
        Log.i("IMSCNetwork", "onSimReady, IMSCConfReader is not started yet");
        startConfReader();
        continue;
      }
    }
  }

  public void onUiccRefresh(Intent paramIntent)
  {
    int[] arrayOfInt = paramIntent.getIntArrayExtra("result");
    int i = 0;
    switch (arrayOfInt[0])
    {
    default:
      Log.e("IMSCNetwork", "UICC_REFRESH with unknown operation");
    case 0:
    case 1:
    case 2:
    }
    while (true)
    {
      if (i != 0)
      {
        this.mServiceHandler.removeMessages(2);
        this.mServiceHandler.removeMessages(15);
        this.mServiceHandler.sendMessageDelayed(this.mServiceHandler.obtainMessage(15), 30000L);
        this.mIsUiccRefreshInProgress = true;
      }
      return;
      Log.d("IMSCNetwork", "SIM_REFRESH_FILE_UPDATED");
      int j = arrayOfInt[1];
      Log.d("IMSCNetwork", "SIM file updated: " + j);
      if ((j != 28418) && (j != 28420) && (j != 28419) && (j != 28425))
        continue;
      i = 1;
      continue;
      Log.d("IMSCNetwork", "SIM_REFRESH_INIT");
      i = 1;
      continue;
      Log.d("IMSCNetwork", "SIM_REFRESH_RESET");
      i = 1;
    }
  }

  public void refresh(long paramLong)
  {
    IMSCNative localIMSCNative = IMSCNative.getInstance();
    if (localIMSCNative == null);
    while (true)
    {
      return;
      localIMSCNative.imscRegReRegister(paramLong);
    }
  }

  public void setContext(Context paramContext)
  {
    mContext = paramContext;
    if (this.mConnMgr == null)
      this.mConnMgr = ((ConnectivityManager)mContext.getSystemService("connectivity"));
    if (this.mTelephonyMgr == null)
      this.mTelephonyMgr = ((TelephonyManager)mContext.getSystemService("phone"));
    if (this.mAlarmManager == null)
    {
      this.mAlarmManager = ((AlarmManager)mContext.getSystemService("alarm"));
      Intent localIntent1 = new Intent("com.motorola.ims.action.link-reconnect");
      this.mLinkRetryTimeoutIntent = PendingIntent.getBroadcast(mContext, 0, localIntent1, 0);
      Intent localIntent2 = new Intent("com.motorola.ims.action.mgrInit", null);
      this.mMgrInitIntent = PendingIntent.getBroadcast(mContext, 0, localIntent2, 0);
      Intent localIntent3 = new Intent("com.motorola.ims.action.nativetimer", null);
      this.mNativeTimerIntent = PendingIntent.getBroadcast(mContext, 0, localIntent3, 0);
    }
    if (this.mPowerManager == null)
      this.mPowerManager = ((PowerManager)mContext.getSystemService("power"));
    if (this.mDataRetryWakeLock == null)
    {
      this.mDataRetryWakeLock = this.mPowerManager.newWakeLock(1, "IMS_Data_Retry");
      this.mDataRetryWakeLock.setReferenceCounted(false);
    }
  }

  // ERROR //
  public void setListener(IConnectionStateListener paramIConnectionStateListener, boolean paramBoolean)
  {
    // Byte code:
    //   0: ldc 65
    //   2: new 336	java/lang/StringBuilder
    //   5: dup
    //   6: invokespecial 337	java/lang/StringBuilder:<init>	()V
    //   9: ldc_w 1126
    //   12: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   15: iload_2
    //   16: invokevirtual 564	java/lang/StringBuilder:append	(Z)Ljava/lang/StringBuilder;
    //   19: invokevirtual 349	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   22: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   25: pop
    //   26: iconst_1
    //   27: iload_2
    //   28: if_icmpne +168 -> 196
    //   31: aload_0
    //   32: getfield 190	com/motorola/android/server/ims/IMSCNetwork:mRecords	Ljava/util/ArrayList;
    //   35: astore 5
    //   37: aload 5
    //   39: monitorenter
    //   40: aconst_null
    //   41: astore 6
    //   43: aload_1
    //   44: invokeinterface 1130 1 0
    //   49: astore 8
    //   51: aload_0
    //   52: getfield 190	com/motorola/android/server/ims/IMSCNetwork:mRecords	Ljava/util/ArrayList;
    //   55: invokevirtual 610	java/util/ArrayList:size	()I
    //   58: istore 9
    //   60: ldc 65
    //   62: new 336	java/lang/StringBuilder
    //   65: dup
    //   66: invokespecial 337	java/lang/StringBuilder:<init>	()V
    //   69: ldc_w 1132
    //   72: invokevirtual 343	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   75: iload 9
    //   77: invokevirtual 346	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
    //   80: invokevirtual 349	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   83: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   86: pop
    //   87: iconst_0
    //   88: istore 11
    //   90: iload 11
    //   92: iload 9
    //   94: if_icmpge +138 -> 232
    //   97: aload_0
    //   98: getfield 190	com/motorola/android/server/ims/IMSCNetwork:mRecords	Ljava/util/ArrayList;
    //   101: iload 11
    //   103: invokevirtual 613	java/util/ArrayList:get	(I)Ljava/lang/Object;
    //   106: checkcast 14	com/motorola/android/server/ims/IMSCNetwork$Record
    //   109: astore 6
    //   111: aload 6
    //   113: getfield 630	com/motorola/android/server/ims/IMSCNetwork$Record:binder	Landroid/os/IBinder;
    //   116: astore 18
    //   118: aload 8
    //   120: aload 18
    //   122: if_acmpne +118 -> 240
    //   125: aload 6
    //   127: astore 12
    //   129: iload 11
    //   131: iload 9
    //   133: if_icmpne +93 -> 226
    //   136: ldc 65
    //   138: ldc_w 1134
    //   141: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   144: pop
    //   145: new 14	com/motorola/android/server/ims/IMSCNetwork$Record
    //   148: dup
    //   149: aconst_null
    //   150: invokespecial 1137	com/motorola/android/server/ims/IMSCNetwork$Record:<init>	(Lcom/motorola/android/server/ims/IMSCNetwork$1;)V
    //   153: astore 16
    //   155: aload 16
    //   157: aload 8
    //   159: putfield 630	com/motorola/android/server/ims/IMSCNetwork$Record:binder	Landroid/os/IBinder;
    //   162: aload 16
    //   164: aload_1
    //   165: putfield 620	com/motorola/android/server/ims/IMSCNetwork$Record:callback	Lcom/motorola/android/ims/IConnectionStateListener;
    //   168: aload 16
    //   170: iconst_0
    //   171: putfield 616	com/motorola/android/server/ims/IMSCNetwork$Record:connected	Z
    //   174: aload_0
    //   175: getfield 190	com/motorola/android/server/ims/IMSCNetwork:mRecords	Ljava/util/ArrayList;
    //   178: aload 16
    //   180: invokevirtual 1140	java/util/ArrayList:add	(Ljava/lang/Object;)Z
    //   183: pop
    //   184: aload 5
    //   186: monitorexit
    //   187: goto +52 -> 239
    //   190: aload 5
    //   192: monitorexit
    //   193: aload 7
    //   195: athrow
    //   196: ldc 65
    //   198: ldc_w 1142
    //   201: invokestatic 211	android/util/Log:i	(Ljava/lang/String;Ljava/lang/String;)I
    //   204: pop
    //   205: aload_0
    //   206: aload_1
    //   207: invokeinterface 1130 1 0
    //   212: invokespecial 634	com/motorola/android/server/ims/IMSCNetwork:remove	(Landroid/os/IBinder;)V
    //   215: goto +24 -> 239
    //   218: astore 7
    //   220: aload 12
    //   222: pop
    //   223: goto -33 -> 190
    //   226: aload 12
    //   228: pop
    //   229: goto -45 -> 184
    //   232: aload 6
    //   234: astore 12
    //   236: goto -107 -> 129
    //   239: return
    //   240: iinc 11 1
    //   243: goto -153 -> 90
    //   246: astore 7
    //   248: goto -58 -> 190
    //
    // Exception table:
    //   from	to	target	type
    //   136	155	218	finally
    //   43	118	246	finally
    //   155	193	246	finally
  }

  public void startImsMgrInit()
  {
    Log.i("IMSCNetwork", "startImsMgrInit(), configFile: " + this.mConfigFile);
    IMSCNative localIMSCNative = IMSCNative.getInstance();
    if ((localIMSCNative != null) && (!localIMSCNative.imscMgrInit(this.mConfigFile, this.mImscMgrCB)))
      processImsMgrInitFailed();
  }

  private class RegistrationCallback
    implements IMSCNative.IMSCRegistrationCallback
  {
    private RegistrationCallback()
    {
    }

    private void broadcastRegStatus(IMSCRegistrationEvent paramIMSCRegistrationEvent)
    {
      int i = paramIMSCRegistrationEvent.getEventType();
      switch (i)
      {
      case 4:
      case 7:
      default:
      case 1:
      case 5:
      case 2:
      case 6:
      case 3:
      case 8:
      }
      while (true)
      {
        return;
        int j = 0;
        IMSCNetwork.access$1902(IMSCNetwork.this, false);
        Intent localIntent;
        while (true)
        {
          Log.i("IMSCNetwork", "broadcastRegStatus, statusCode: " + j + ", mReReg: " + IMSCNetwork.this.mReReg);
          localIntent = new Intent("com.motorola.ims.action.registration_status");
          localIntent.putExtra("Code", j);
          localIntent.putExtra("reReg", IMSCNetwork.this.mReReg);
          if (j != 3)
            break label265;
          IMSCNetwork.mContext.sendStickyBroadcast(localIntent);
          break;
          j = 0;
          IMSCNetwork.access$1902(IMSCNetwork.this, true);
          continue;
          if (i == 2)
            IMSCNetwork.access$1902(IMSCNetwork.this, false);
          while (true)
          {
            if (IMSCNetwork.this.mIs404 != true)
              break label217;
            j = 4;
            break;
            IMSCNetwork.access$1902(IMSCNetwork.this, true);
          }
          label217: if ((IMSCNetwork.this.mIs403 == true) && (!IMSCNetwork.this.mReReg))
          {
            j = 3;
            continue;
          }
          j = 2;
          continue;
          IMSCNetwork.access$1902(IMSCNetwork.this, false);
          j = 1;
        }
        label265: IMSCNetwork.mContext.sendBroadcast(localIntent);
      }
    }

    private void processFailureCases(IMSCRegistrationEvent paramIMSCRegistrationEvent)
    {
      Log.i("IMSCNetwork", "Enter processFailureCases");
      int i = 0;
      IMSCRegistrationEvent.IMSCRegistrationFailedReason localIMSCRegistrationFailedReason = paramIMSCRegistrationEvent.getRegistrationFailedReason();
      if (localIMSCRegistrationFailedReason != null)
        i = localIMSCRegistrationFailedReason.mExtensionCode;
      Log.i("IMSCNetwork", "processFailureCases, failedReason:" + i);
      if ((i == 0) || (i == 2) || (i == 1))
      {
        if (i != 1)
          break label251;
        IMSCNetwork.access$1402(IMSCNetwork.this, true);
        if (i != 2)
          break label263;
        IMSCNetwork.access$1502(IMSCNetwork.this, true);
        label94: Log.i("IMSCNetwork", "processFailureCases, mIs403:" + IMSCNetwork.this.mIs403 + ", mIs404:" + IMSCNetwork.this.mIs404 + ", mIsImsiUri:" + IMSCNetwork.this.mIsImsiUri);
        if ((IMSCNetwork.this.mIs404 != true) || (IMSCNetwork.this.mIsImsiUri))
          break label275;
        IMSCNative localIMSCNative = IMSCNative.getInstance();
        if ((localIMSCNative != null) && (IMSCNetwork.mPublicURI_IMSI != null))
        {
          localIMSCNative.imscRegSetPublicURI(IMSCNetwork.mRegId, IMSCNetwork.mPublicURI_IMSI);
          IMSCNetwork.access$902(IMSCNetwork.this, true);
          Log.i("IMSCNetwork", "processFailureCases, IMSI based URI: " + IMSCNetwork.mPublicURI_IMSI);
          IMSCNetwork.this.register(IMSCNetwork.mRegId);
        }
      }
      while (true)
      {
        return;
        label251: IMSCNetwork.access$1402(IMSCNetwork.this, false);
        break;
        label263: IMSCNetwork.access$1502(IMSCNetwork.this, false);
        break label94;
        label275: broadcastRegStatus(paramIMSCRegistrationEvent);
        monitorenter;
        try
        {
          if ((IMSCNetwork.this.mState != 0) && (IMSCNetwork.this.mState != 4))
            IMSCNetwork.this.mServiceHandler.sendMessageDelayed(IMSCNetwork.this.mServiceHandler.obtainMessage(11), 300L);
          monitorexit;
          continue;
        }
        finally
        {
          localObject = finally;
          monitorexit;
        }
      }
      throw localObject;
    }

    public void onRegistrationCallback(IMSCRegistrationEvent paramIMSCRegistrationEvent)
    {
      long l = paramIMSCRegistrationEvent.getRegId();
      int i = paramIMSCRegistrationEvent.getEventType();
      Log.i("IMSCNetwork", "onRegistrationCallback, regId:" + l);
      Log.i("IMSCNetwork", "onRegistrationCallback, evType:" + i);
      switch (i)
      {
      case 4:
      case 7:
      case 9:
      default:
      case 1:
      case 5:
      case 2:
      case 6:
      case 3:
      case 8:
      case 10:
      }
      while (true)
      {
        return;
        monitorenter;
        while (true)
        {
          try
          {
            IMSCNetwork.access$802(IMSCNetwork.this, 3);
            monitorexit;
            IMSCNetwork.access$002(IMSCNetwork.this, false);
            Log.i("IMSCNetwork", "onRegistrationCallback: STATE_IMS_READY");
            if (IMSCNetwork.this.mIsImsiUri)
            {
              IMSCNetwork.access$1002(IMSCNetwork.this, true);
              Log.i("IMSCNetwork", "onRegistrationCallback, notifyNetworkConnected");
              IMSCNetwork.this.notifyNetworkConnected();
              broadcastRegStatus(paramIMSCRegistrationEvent);
              break;
            }
          }
          finally
          {
            monitorexit;
          }
          IMSCNetwork.access$1002(IMSCNetwork.this, false);
        }
        IMSCNetwork.access$002(IMSCNetwork.this, false);
        processFailureCases(paramIMSCRegistrationEvent);
        monitorenter;
        try
        {
          if (IMSCNetwork.this.mState == 3)
            IMSCNetwork.access$802(IMSCNetwork.this, 2);
          monitorexit;
          Log.i("IMSCNetwork", "onRegistrationCallback: Registration failed");
          IMSCNetwork.this.notifyNetworkDisConnected();
          continue;
        }
        finally
        {
          monitorexit;
        }
        IMSCNetwork.access$002(IMSCNetwork.this, false);
        monitorenter;
        try
        {
          if (IMSCNetwork.this.mState == 3)
            IMSCNetwork.access$802(IMSCNetwork.this, 2);
          monitorexit;
          Log.i("IMSCNetwork", "onRegistrationCallback: Reg removed or Un-Reg Success");
          IMSCNetwork.this.notifyNetworkDisConnected();
          broadcastRegStatus(paramIMSCRegistrationEvent);
          continue;
        }
        finally
        {
          monitorexit;
        }
        IMSCNative localIMSCNative = IMSCNative.getInstance();
        if (localIMSCNative == null)
          continue;
        IMSCRegistrationEvent.IMSCOperationCompletedData localIMSCOperationCompletedData = paramIMSCRegistrationEvent.getOperationCompletedData();
        if ((!IMSCNetwork.this.mNeedRegForConfigUpdate) || (localIMSCOperationCompletedData == null) || (localIMSCOperationCompletedData.mOperationId != localIMSCNative.getDestroyRegOperationId()))
          continue;
        IMSCNetwork.this.doRegister();
        IMSCNetwork.access$1302(IMSCNetwork.this, false);
      }
    }
  }

  private final class ServiceHandler extends Handler
  {
    public ServiceHandler(Looper arg2)
    {
      super();
    }

    public void handleMessage(Message paramMessage)
    {
      switch (paramMessage.what)
      {
      case 3:
      case 4:
      case 5:
      case 6:
      case 7:
      case 8:
      case 9:
      case 10:
      default:
      case 1:
      case 2:
      case 16:
      case 11:
      case 12:
      case 13:
      case 14:
      case 15:
      }
      while (true)
      {
        return;
        String str = (String)paramMessage.obj;
        Log.i("IMSCNetwork", "Handler: CONFIG_READY, config file: " + str);
        IMSCNetwork.this.onImsConfigReady(str);
        continue;
        Log.i("IMSCNetwork", "Handler: CONFIG_UPDATED");
        if (IMSCNetwork.this.mIsRegistering)
        {
          IMSCNetwork.this.mServiceHandler.sendMessageDelayed(IMSCNetwork.this.mServiceHandler.obtainMessage(2), 5000L);
          continue;
        }
        IMSCNetwork.this.onImsConfigUpdated();
        continue;
        IMSCNetwork.this.checkAndStartDataConnection();
        continue;
        IMSCNetwork.this.stopDataConnection();
        continue;
        IMSCNetwork.this.onDoRegister();
        continue;
        IMSCNetwork.this.onDeRegister();
        continue;
        IMSCNetwork.this.onStartConfReader();
        continue;
        IMSCConfReader localIMSCConfReader = IMSCConfReader.getInstance();
        if (localIMSCConfReader == null)
          continue;
        localIMSCConfReader.onUiccRefresh();
      }
    }
  }

  private static class Record
  {
    IBinder binder;
    IConnectionStateListener callback;
    boolean connected;
  }
}

/* Location:           /home/dhacker29/jd/classes_dex2jar.jar
 * Qualified Name:     com.motorola.android.server.ims.IMSCNetwork
 * JD-Core Version:    0.6.0
 */