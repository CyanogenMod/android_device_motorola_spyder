package com.motorola.android.server.ims;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.motorola.android.telephony.IPhoneNVInfo;
import com.motorola.android.telephony.IPhoneNVInfo.Stub;
import com.motorola.android.telephony.IPhoneNVInfoObserver.Stub;
import com.motorola.android.telephony.ISIMAuthRes;
import com.motorola.android.telephony.ISIMInterfaceManager;
import java.util.EnumMap;

final class IMSCConfReader
{
  public static final int CONFIG_READY = 1;
  public static final int CONFIG_UPDATED = 2;
  private static final String DEFAULT_HOME_DOMAIN = "vzims.com";
  private static final boolean DEFAULT_IMS_SATUS = true;
  private static final int DEFAULT_PCSCF_PORT = 5060;
  private static final String DEFAULT_PHONE_CONTEXT_URI = "vzims.com";
  private static final int DEFAULT_SIG_COMP = 0;
  private static final int DEFAULT_SMS_FORMAT = 0;
  private static final int DEFAULT_T1_TIMER = 3;
  private static final int DEFAULT_T2_TIMER = 16;
  private static final int DEFAULT_TEST_MODE = 0;
  private static final int DEFAULT_TF_TIMER = 30;
  private static final int ISIM_PARAMETER_READY = 6;
  private static final int ISIM_PARAMETER_READ_RETRY = 5;
  private static final int ISIM_RETRY_COUNT_MAX = 4;
  private static final String TAG = "IMSCConfReader";
  private static final int UICC_REFRESH_COUNT_MAX = 4;
  private static final int UICC_REFRESH_READY = 10;
  private static final int UICC_REFRESH_RETRY = 9;
  private static final int UPDATE_DB = 4;
  private static final int VALUE_CHANGED = 3;
  private static final String fileConf = "/data/imsc.conf";
  private static int iSimRetryCount;
  private static IMSCConfReader mInstance = null;
  private int iUiccRefreshCount = 0;
  private ISIMInterfaceManager isimhandle = null;
  private Context mContext = null;
  private Handler mHandler = new Handler()
  {
    public void handleMessage(Message paramMessage)
    {
      switch (paramMessage.what)
      {
      case 7:
      case 8:
      default:
      case 3:
      case 4:
      case 6:
      case 5:
      case 10:
      case 9:
      }
      while (true)
      {
        super.handleMessage(paramMessage);
        return;
        switch (paramMessage.arg1)
        {
        case 8009:
        case 8010:
        case 8012:
        case 8019:
        case 8020:
        default:
          Log.d("IMSCConfReader", "NV item update, unknown NV item: " + Integer.toString(paramMessage.arg1));
          break;
        case 8008:
          IMSCConfReader.this.readT1Timer();
          break;
        case 8016:
          IMSCConfReader.this.readT2Timer();
          break;
        case 8015:
          IMSCConfReader.this.readTfTimer();
          break;
        case 8013:
          IMSCConfReader.this.readPhoneContext();
          break;
        case 8011:
          IMSCConfReader.this.readPCscfPort();
          break;
        case 8014:
          IMSCConfReader.this.readSigComp();
          break;
        case 8021:
          IMSCConfReader.this.readTestMode();
          break;
        case 8017:
          IMSCConfReader.this.readSmsFormat();
          break;
        case 8018:
          IMSCConfReader.this.readSmsOverImsStatus();
          continue;
          if (!IMSCConfReader.this.updateConfigFile())
            continue;
          IMSCConfReader.this.mServerHandler.sendMessage(IMSCConfReader.this.mServerHandler.obtainMessage(2, 0, 0, null));
          continue;
          if (!IMSCConfReader.this.updateConfigFile())
            continue;
          IMSCConfReader.this.mServerHandler.sendMessage(IMSCConfReader.this.mServerHandler.obtainMessage(1, 0, 0, "/data/imsc.conf"));
          continue;
          IMSCConfReader.this.onReadISIMParameters();
          continue;
          IMSCConfReader.this.mHandler.removeMessages(4);
          IMSCConfReader.this.mHandler.sendMessageDelayed(IMSCConfReader.this.mHandler.obtainMessage(4), 500L);
          continue;
          IMSCConfReader.this.onRetryUiccRefresh();
        }
      }
    }
  };
  String mIMSI = null;
  boolean mIMSRegDisabled = false;
  boolean mImsStatus = true;
  private IPhoneNVInfo mNvInfo = null;
  private final IPhoneNVInfoObserver.Stub mObserver = new IPhoneNVInfoObserver.Stub()
  {
    public void valueChanged(int paramInt)
    {
      Log.d("IMSCConfReader", "Drop into IPhoneNVInfoObserver, nv = " + Integer.toString(paramInt));
      IMSCConfReader.this.mHandler.sendMessage(IMSCConfReader.this.mHandler.obtainMessage(3, paramInt, 0));
      IMSCConfReader.this.mHandler.removeMessages(4);
      IMSCConfReader.this.mHandler.sendMessageDelayed(IMSCConfReader.this.mHandler.obtainMessage(4), 500L);
    }
  };
  String[] mPublicId = null;
  private Handler mServerHandler = null;
  int mSmsFormat = 0;
  int mTestMode = 0;
  private EnumMap<IMSCDbKeys, String> paras;
  private TelephonyManager telManager = null;

  static
  {
    iSimRetryCount = 0;
  }

  public static IMSCConfReader getInstance()
  {
    return mInstance;
  }

  private void onReadISIMParameters()
  {
    if (readISIMParameters())
      this.mHandler.sendMessage(this.mHandler.obtainMessage(6));
    while (true)
    {
      return;
      if (iSimRetryCount > 4)
      {
        Log.e("IMSCConfReader", "ISIM parameters ready retry count > ISIM_RETRY_COUNT_MAX");
        continue;
      }
      this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(5), 1000L);
    }
  }

  private void onRetryUiccRefresh()
  {
    if (readUiccRefreshParameters())
      this.mHandler.sendMessage(this.mHandler.obtainMessage(10));
    while (true)
    {
      return;
      if (this.iUiccRefreshCount > 4)
      {
        Log.e("IMSCConfReader", "Uicc Refresh retry count > UICC_REFRESH_COUNT_MAX");
        continue;
      }
      this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(9), 30000L);
    }
  }

  private boolean readHomeDomain()
  {
    String str = "vzims.com";
    int i = 0;
    if (this.isimhandle != null)
    {
      str = this.isimhandle.getISIMHomeDomain();
      if ((str != null) && (str.length() != 0))
        break label153;
      Log.e("IMSCConfReader", "mISimInt.getISIMHomeDomain returns null, use default value");
      str = "vzims.com";
    }
    while (true)
    {
      if ((!this.paras.containsKey(IMSCDbKeys.IMS_SESSION_DB_PCSCF_REALM_KEY_STR)) || (i != 0))
      {
        this.paras.put(IMSCDbKeys.IMS_SESSION_DB_PCSCF_REALM_KEY_STR, str);
        Log.d("IMSCConfReader", "PCSCF REALM is " + str);
      }
      if ((!this.paras.containsKey(IMSCDbKeys.IMS_SESSION_DB_SCSCF_SERVER_NAME_KEY_STR)) || (i != 0))
      {
        this.paras.put(IMSCDbKeys.IMS_SESSION_DB_SCSCF_SERVER_NAME_KEY_STR, str);
        Log.d("IMSCConfReader", "SCSCF SERVER NAME is " + str);
      }
      return i;
      label153: i = 1;
      Log.d("IMSCConfReader", "homeDomain read is " + str);
    }
  }

  private void readIMSRegistrationDisable()
  {
    if (SystemProperties.getInt("radio.lte.ignoreims", 0) == 2);
    for (boolean bool = true; ; bool = false)
    {
      this.mIMSRegDisabled = bool;
      Log.d("IMSCConfReader", "Read IMS Registration Disable =  " + this.mIMSRegDisabled);
      return;
    }
  }

  private boolean readISIMParameters()
  {
    iSimRetryCount = 1 + iSimRetryCount;
    boolean bool = readPrivateIdentity();
    if (bool)
      bool = readPublicIdentity();
    if (bool)
      bool = readHomeDomain();
    if (bool)
      bool = readPCSCFAddr();
    return bool;
  }

  private void readIpsecEnable()
  {
    int i = 0;
    String str = SystemProperties.get("net.ims.ipsecenable");
    if (str.length() != 0)
      i = 1;
    Log.d("IMSCConfReader", "Read IPSec Enable =  " + str);
    if ((!this.paras.containsKey(IMSCDbKeys.IMS_SESSION_DB_PCSCF_SEC_AGREE_SUPPORT_KEY_STR)) || (i != 0))
      this.paras.put(IMSCDbKeys.IMS_SESSION_DB_PCSCF_SEC_AGREE_SUPPORT_KEY_STR, str);
  }

  private boolean readPCSCFAddr()
  {
    int i = 0;
    int j;
    if (this.isimhandle != null)
    {
      String[] arrayOfString = this.isimhandle.getISIMPCSCF();
      if (arrayOfString == null)
      {
        Log.e("IMSCConfReader", "addr returned is null");
        j = i;
        return j;
      }
      StringBuffer localStringBuffer = new StringBuffer();
      if (localStringBuffer == null)
        break label177;
      int k = 0;
      for (int m = 0; m < arrayOfString.length; m++)
      {
        if (arrayOfString[m] == null)
          continue;
        if (k != 0)
          localStringBuffer.append(";");
        localStringBuffer.append(arrayOfString[m]);
        k = 1;
      }
      String str = localStringBuffer.toString();
      if (str.length() > 255)
        str = str.substring(0, 255);
      Log.d("IMSCConfReader", "pcscf read from sim is:" + str);
      this.paras.put(IMSCDbKeys.IMS_SESSION_DB_PCSCF_SERVER_ADDR_KEY_STR, str);
      i = 1;
    }
    while (true)
    {
      j = i;
      break;
      label177: Log.d("IMSCConfReader", "Failed to create StringBuffer for pcscf");
    }
  }

  private void readPCscfPort()
  {
    int i = 5060;
    int j = 0;
    if (this.mNvInfo != null);
    try
    {
      i = this.mNvInfo.getImsPcscfPort();
      if (i < 0)
      {
        Log.d("IMSCConfReader", "mNvInfo.getImsPcscfPort returns < 0, use default value");
        i = 5060;
      }
      while (true)
      {
        if ((!this.paras.containsKey(IMSCDbKeys.IMS_SESSION_DB_PCSCF_PORT_NUM_KEY_STR)) || (j != 0))
          this.paras.put(IMSCDbKeys.IMS_SESSION_DB_PCSCF_PORT_NUM_KEY_STR, Integer.toString(i));
        return;
        j = 1;
      }
    }
    catch (RemoteException localRemoteException)
    {
      while (true)
        Log.d("IMSCConfReader", "RemoteException, use default value");
    }
  }

  private void readPhoneContext()
  {
    String str = "vzims.com";
    int i = 0;
    if (this.mNvInfo != null);
    try
    {
      str = this.mNvInfo.getImsPhoneContextURI();
      if (str == null)
      {
        Log.d("IMSCConfReader", "mNvInfo.getImsPhoneContextURI returns null, use default value");
        str = "vzims.com";
      }
      while (true)
      {
        if ((!this.paras.containsKey(IMSCDbKeys.IMS_SESSION_DB_PHONE_CONTEXT)) || (i != 0))
          this.paras.put(IMSCDbKeys.IMS_SESSION_DB_PHONE_CONTEXT, str);
        return;
        i = 1;
      }
    }
    catch (RemoteException localRemoteException)
    {
      while (true)
        Log.d("IMSCConfReader", "RemoteException, use default value");
    }
  }

  private boolean readPrivateIdentity()
  {
    int i = 0;
    String str1 = null;
    String str2 = null;
    if ((this.mIMSI != null) && (this.mIMSI.length() >= 6))
    {
      String str3 = this.mIMSI.substring(0, 3);
      String str4 = this.mIMSI.substring(3, 6);
      str1 = "6" + this.mIMSI + "@nai.epc.mnc" + str4 + ".mcc" + str3 + ".3gppnetwork.org";
      Log.d("IMSCConfReader", "Default Private Id is " + str1);
    }
    if (this.isimhandle != null)
      str2 = this.isimhandle.getISIMPrivateIdentity();
    if ((str2 == null) || (str2.length() == 0))
      if (str2 == null)
      {
        Log.d("IMSCConfReader", "Can not read Private Identity, use default based on ISIM");
        if ((!this.paras.containsKey(IMSCDbKeys.IMS_SESSION_DB_PCSCF_PRIVATE_USER_ID_KEY_STR)) && (str1 != null))
          this.paras.put(IMSCDbKeys.IMS_SESSION_DB_PCSCF_PRIVATE_USER_ID_KEY_STR, str1);
      }
    while (true)
    {
      return i;
      Log.d("IMSCConfReader", "Private Identity length is 0, use default based on ISIM");
      break;
      i = 1;
      Log.d("IMSCConfReader", "private identity read from sim: " + str2);
      this.paras.put(IMSCDbKeys.IMS_SESSION_DB_PCSCF_PRIVATE_USER_ID_KEY_STR, str2);
    }
  }

  private boolean readPublicIdentity()
  {
    int i = 0;
    if (this.isimhandle != null)
    {
      String[] arrayOfString = this.isimhandle.getAllISIMPublicIdentity();
      if (arrayOfString != null)
      {
        this.mPublicId = arrayOfString;
        i = 1;
        for (int j = 0; j < this.mPublicId.length; j++)
          Log.d("IMSCConfReader", "Public Identity read is " + this.mPublicId[j]);
      }
    }
    return i;
  }

  private void readSigComp()
  {
    int i = 0;
    int j = 0;
    if (this.mNvInfo != null);
    try
    {
      boolean bool = this.mNvInfo.getImsSigComp();
      if (bool);
      for (i = 1; ; i = 0)
      {
        j = 1;
        if ((!this.paras.containsKey(IMSCDbKeys.IMS_SESSION_DB_SIGCOMP)) || (j != 0))
          this.paras.put(IMSCDbKeys.IMS_SESSION_DB_SIGCOMP, Integer.toString(i));
        return;
      }
    }
    catch (RemoteException localRemoteException)
    {
      while (true)
        Log.d("IMSCConfReader", "RemoteException, use default value");
    }
  }

  private void readSmsFormat()
  {
    this.mSmsFormat = 0;
    if (this.mNvInfo != null);
    try
    {
      if (this.mNvInfo.getImsSmsFormat())
        this.mSmsFormat = 1;
      this.paras.put(IMSCDbKeys.IMS_SESSION_DB_SMS_OVER_IMS_PROTOCOL_TYPE_KEY_STR, Integer.toString(this.mSmsFormat));
      Log.d("IMSCConfReader", "SMS Format is " + Integer.toString(this.mSmsFormat));
      return;
    }
    catch (RemoteException localRemoteException)
    {
      while (true)
        Log.d("IMSCConfReader", "RemoteException, use default value");
    }
  }

  private void readSmsOverImsStatus()
  {
    if (this.mNvInfo != null);
    try
    {
      this.mImsStatus = this.mNvInfo.getImsSmsOverIP();
      Log.d("IMSCConfReader", "SMS over IMS STATUS is " + Boolean.toString(this.mImsStatus));
      return;
    }
    catch (RemoteException localRemoteException)
    {
      while (true)
        Log.d("IMSCConfReader", "RemoteException, use default value");
    }
  }

  private void readT1Timer()
  {
    int i = 3;
    int j = 0;
    if (this.mNvInfo != null);
    try
    {
      i = this.mNvInfo.getImsT1Timer();
      if (i < 0)
      {
        Log.d("IMSCConfReader", "mNvInfo.getImsT1Timer returns < 0, use default value");
        i = 3;
      }
      while (true)
      {
        if ((!this.paras.containsKey(IMSCDbKeys.IMS_SESSION_DB_RETRANS_T1_KEY_STR)) || (j != 0))
          this.paras.put(IMSCDbKeys.IMS_SESSION_DB_RETRANS_T1_KEY_STR, Integer.toString(i * 1000));
        return;
        j = 1;
      }
    }
    catch (RemoteException localRemoteException)
    {
      while (true)
        Log.d("IMSCConfReader", "RemoteException, use default value");
    }
  }

  private void readT2Timer()
  {
    int i = 16;
    int j = 0;
    if (this.mNvInfo != null);
    try
    {
      i = this.mNvInfo.getImsT2Timer();
      if (i < 0)
      {
        Log.d("IMSCConfReader", "mNvInfo.getImsT2Timer returns < 0, use default value");
        i = 16;
      }
      while (true)
      {
        if ((!this.paras.containsKey(IMSCDbKeys.IMS_SESSION_DB_RETRANS_T2_KEY_STR)) || (j != 0))
          this.paras.put(IMSCDbKeys.IMS_SESSION_DB_RETRANS_T2_KEY_STR, Integer.toString(i * 1000));
        return;
        j = 1;
      }
    }
    catch (RemoteException localRemoteException)
    {
      while (true)
        Log.d("IMSCConfReader", "RemoteException, use default value");
    }
  }

  private void readTestMode()
  {
    this.mTestMode = 0;
    if (this.mNvInfo != null)
      try
      {
        boolean bool = this.mNvInfo.getImsTestMode();
        Log.d("IMSCConfReader", "Read Test Mode from NV " + Boolean.toString(bool));
        if (bool)
          this.mTestMode = 1;
        else
          this.mTestMode = 0;
      }
      catch (RemoteException localRemoteException)
      {
        Log.d("IMSCConfReader", "RemoteException, use default value");
      }
  }

  private void readTfTimer()
  {
    int i = 30;
    int j = 0;
    if (this.mNvInfo != null);
    try
    {
      i = this.mNvInfo.getImsTfTimer();
      if (i < 0)
      {
        Log.d("IMSCConfReader", "mNvInfo.getImsTfTimer returns < 0, use default value");
        i = 30;
      }
      while (true)
      {
        if ((!this.paras.containsKey(IMSCDbKeys.IMS_SESSION_DB_REQUEST_TIMEOUT_KEY_STR)) || (j != 0))
          this.paras.put(IMSCDbKeys.IMS_SESSION_DB_REQUEST_TIMEOUT_KEY_STR, Integer.toString(i * 1000));
        return;
        j = 1;
      }
    }
    catch (RemoteException localRemoteException)
    {
      while (true)
        Log.d("IMSCConfReader", "RemoteException, use default value");
    }
  }

  private boolean readUiccRefreshParameters()
  {
    this.iUiccRefreshCount = (1 + this.iUiccRefreshCount);
    boolean bool = readPrivateIdentity();
    if (bool)
      bool = readPublicIdentity();
    if (bool)
      bool = readHomeDomain();
    if (bool)
      bool = readPCSCFAddr();
    return bool;
  }

  private void start()
  {
    Log.d("IMSCConfReader", "Enter into start");
    this.paras = new EnumMap(IMSCDbKeys.class);
    this.paras.put(IMSCDbKeys.IMS_SESSION_DB_MAX_NUM_TAPI_SESSIONS, "1");
    this.paras.put(IMSCDbKeys.IMS_SESSION_DB_PCSCF_AUTH_INITIAL_RES_STR, "0");
    this.mNvInfo = IPhoneNVInfo.Stub.asInterface(ServiceManager.getService("iphonenvinfo"));
    readT1Timer();
    readT2Timer();
    readTfTimer();
    readPhoneContext();
    readPCscfPort();
    readSigComp();
    readTestMode();
    readIpsecEnable();
    readIMSRegistrationDisable();
    readSmsFormat();
    readSmsOverImsStatus();
    this.telManager = ((TelephonyManager)this.mContext.getSystemService("phone"));
    this.mIMSI = this.telManager.getSubscriberId();
    if (this.mIMSI == null)
      Log.d("IMSCConfReader", "mIMSI is null");
    this.isimhandle = new ISIMInterfaceManager();
    if (this.isimhandle == null)
      Log.d("IMSCConfReader", "isimhandle is null");
    if (this.mNvInfo != null);
    try
    {
      this.mNvInfo.registerObserver(this.mObserver);
      if (readISIMParameters())
      {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(6));
        return;
      }
    }
    catch (RemoteException localRemoteException)
    {
      while (true)
      {
        Log.d("IMSCConfReader", "RemoteException when registerObserver");
        continue;
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(5), 1000L);
      }
    }
  }

  public static void start(Context paramContext, Handler paramHandler)
  {
    if (mInstance == null)
      mInstance = new IMSCConfReader();
    if (mInstance != null)
    {
      mInstance.mContext = paramContext;
      mInstance.mServerHandler = paramHandler;
      mInstance.start();
    }
  }

  // ERROR //
  private boolean updateConfigFile()
  {
    // Byte code:
    //   0: new 309	java/lang/StringBuffer
    //   3: dup
    //   4: sipush 256
    //   7: invokespecial 503	java/lang/StringBuffer:<init>	(I)V
    //   10: astore_1
    //   11: aload_0
    //   12: getfield 226	com/motorola/android/server/ims/IMSCConfReader:paras	Ljava/util/EnumMap;
    //   15: invokevirtual 507	java/util/EnumMap:entrySet	()Ljava/util/Set;
    //   18: invokeinterface 513 1 0
    //   23: astore_2
    //   24: aload_2
    //   25: invokeinterface 518 1 0
    //   30: ifeq +66 -> 96
    //   33: aload_2
    //   34: invokeinterface 522 1 0
    //   39: checkcast 524	java/util/Map$Entry
    //   42: astore 26
    //   44: aload_1
    //   45: aload 26
    //   47: invokeinterface 527 1 0
    //   52: checkcast 228	com/motorola/android/server/ims/IMSCDbKeys
    //   55: invokevirtual 530	com/motorola/android/server/ims/IMSCDbKeys:ordinal	()I
    //   58: invokevirtual 533	java/lang/StringBuffer:append	(I)Ljava/lang/StringBuffer;
    //   61: pop
    //   62: aload_1
    //   63: ldc_w 535
    //   66: invokevirtual 315	java/lang/StringBuffer:append	(Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   69: pop
    //   70: aload_1
    //   71: aload 26
    //   73: invokeinterface 538 1 0
    //   78: checkcast 218	java/lang/String
    //   81: invokevirtual 315	java/lang/StringBuffer:append	(Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   84: pop
    //   85: aload_1
    //   86: ldc_w 540
    //   89: invokevirtual 315	java/lang/StringBuffer:append	(Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   92: pop
    //   93: goto -69 -> 24
    //   96: ldc 42
    //   98: ldc_w 542
    //   101: invokestatic 257	android/util/Log:d	(Ljava/lang/String;Ljava/lang/String;)I
    //   104: pop
    //   105: ldc 42
    //   107: aload_1
    //   108: invokevirtual 316	java/lang/StringBuffer:toString	()Ljava/lang/String;
    //   111: invokestatic 257	android/util/Log:d	(Ljava/lang/String;Ljava/lang/String;)I
    //   114: pop
    //   115: aconst_null
    //   116: astore 5
    //   118: iconst_0
    //   119: istore 6
    //   121: new 544	java/io/File
    //   124: dup
    //   125: ldc 52
    //   127: invokespecial 547	java/io/File:<init>	(Ljava/lang/String;)V
    //   130: astore 7
    //   132: aload 7
    //   134: invokevirtual 550	java/io/File:exists	()Z
    //   137: ifeq +9 -> 146
    //   140: aload 7
    //   142: invokevirtual 553	java/io/File:delete	()Z
    //   145: pop
    //   146: aload 7
    //   148: invokevirtual 556	java/io/File:createNewFile	()Z
    //   151: pop
    //   152: new 558	java/io/BufferedWriter
    //   155: dup
    //   156: new 560	java/io/FileWriter
    //   159: dup
    //   160: aload 7
    //   162: invokespecial 563	java/io/FileWriter:<init>	(Ljava/io/File;)V
    //   165: sipush 256
    //   168: invokespecial 566	java/io/BufferedWriter:<init>	(Ljava/io/Writer;I)V
    //   171: astore 20
    //   173: aload 20
    //   175: aload_1
    //   176: invokevirtual 316	java/lang/StringBuffer:toString	()Ljava/lang/String;
    //   179: invokevirtual 571	java/io/Writer:write	(Ljava/lang/String;)V
    //   182: iconst_1
    //   183: istore 6
    //   185: aload 20
    //   187: ifnull +8 -> 195
    //   190: aload 20
    //   192: invokevirtual 574	java/io/Writer:close	()V
    //   195: iload 6
    //   197: ireturn
    //   198: astore 23
    //   200: ldc 42
    //   202: ldc_w 576
    //   205: invokestatic 257	android/util/Log:d	(Ljava/lang/String;Ljava/lang/String;)I
    //   208: pop
    //   209: goto -14 -> 195
    //   212: astore 15
    //   214: ldc 42
    //   216: ldc_w 578
    //   219: invokestatic 257	android/util/Log:d	(Ljava/lang/String;Ljava/lang/String;)I
    //   222: pop
    //   223: aload 5
    //   225: ifnull -30 -> 195
    //   228: aload 5
    //   230: invokevirtual 574	java/io/Writer:close	()V
    //   233: goto -38 -> 195
    //   236: astore 17
    //   238: ldc 42
    //   240: ldc_w 576
    //   243: invokestatic 257	android/util/Log:d	(Ljava/lang/String;Ljava/lang/String;)I
    //   246: pop
    //   247: goto -52 -> 195
    //   250: astore 11
    //   252: ldc 42
    //   254: ldc_w 580
    //   257: invokestatic 257	android/util/Log:d	(Ljava/lang/String;Ljava/lang/String;)I
    //   260: pop
    //   261: aload 5
    //   263: ifnull -68 -> 195
    //   266: aload 5
    //   268: invokevirtual 574	java/io/Writer:close	()V
    //   271: goto -76 -> 195
    //   274: astore 13
    //   276: ldc 42
    //   278: ldc_w 576
    //   281: invokestatic 257	android/util/Log:d	(Ljava/lang/String;Ljava/lang/String;)I
    //   284: pop
    //   285: goto -90 -> 195
    //   288: astore 8
    //   290: aload 5
    //   292: ifnull +8 -> 300
    //   295: aload 5
    //   297: invokevirtual 574	java/io/Writer:close	()V
    //   300: aload 8
    //   302: athrow
    //   303: astore 9
    //   305: ldc 42
    //   307: ldc_w 576
    //   310: invokestatic 257	android/util/Log:d	(Ljava/lang/String;Ljava/lang/String;)I
    //   313: pop
    //   314: goto -14 -> 300
    //   317: astore 8
    //   319: aload 20
    //   321: astore 5
    //   323: goto -33 -> 290
    //   326: astore 22
    //   328: aload 20
    //   330: astore 5
    //   332: goto -80 -> 252
    //   335: astore 21
    //   337: aload 20
    //   339: astore 5
    //   341: goto -127 -> 214
    //
    // Exception table:
    //   from	to	target	type
    //   190	195	198	java/io/IOException
    //   121	173	212	java/io/FileNotFoundException
    //   228	233	236	java/io/IOException
    //   121	173	250	java/io/IOException
    //   266	271	274	java/io/IOException
    //   121	173	288	finally
    //   214	223	288	finally
    //   252	261	288	finally
    //   295	300	303	java/io/IOException
    //   173	182	317	finally
    //   173	182	326	java/io/IOException
    //   173	182	335	java/io/FileNotFoundException
  }

  public void doAkaAuth(int paramInt, byte[] paramArrayOfByte)
  {
    byte[] arrayOfByte1 = new byte[16];
    System.arraycopy(paramArrayOfByte, 0, arrayOfByte1, 0, arrayOfByte1.length);
    byte[] arrayOfByte2 = new byte[16];
    System.arraycopy(paramArrayOfByte, 16, arrayOfByte2, 0, arrayOfByte2.length);
    IMSCNative localIMSCNative = IMSCNative.getInstance();
    if (localIMSCNative == null)
      Log.d("IMSCConfReader", "Failed to get IMSCNative instance, AKA aborted...");
    while (true)
    {
      return;
      if (this.isimhandle != null)
      {
        ISIMAuthRes localISIMAuthRes = this.isimhandle.ISIMAuthReq(16, arrayOfByte1, 16, arrayOfByte2);
        if (localISIMAuthRes != null)
          Log.i("IMSCConfReader", "length of res:" + localISIMAuthRes.res_length + " ik:" + localISIMAuthRes.ik_length + " ck:" + localISIMAuthRes.ck_length + " auts:" + localISIMAuthRes.auts_length);
        if ((localISIMAuthRes != null) && ((localISIMAuthRes.res_length == 0) || ((localISIMAuthRes.res_length >= 4) && (localISIMAuthRes.res_length <= 16) && (localISIMAuthRes.res != null) && (localISIMAuthRes.res.length == localISIMAuthRes.res_length))) && ((localISIMAuthRes.ik_length == 0) || ((localISIMAuthRes.ik_length == 16) && (localISIMAuthRes.ik != null) && (localISIMAuthRes.ik.length == 16))) && ((localISIMAuthRes.ck_length == 0) || ((localISIMAuthRes.ck_length == 16) && (localISIMAuthRes.ck != null) && (localISIMAuthRes.ck.length == 16))) && ((localISIMAuthRes.auts_length == 0) || ((localISIMAuthRes.auts != null) && (localISIMAuthRes.auts_length == localISIMAuthRes.auts.length))))
        {
          if (localISIMAuthRes.auts_length == 0)
          {
            localIMSCNative.setAKAKeys(paramInt, 1, localISIMAuthRes.res, localISIMAuthRes.res_length, localISIMAuthRes.ik, localISIMAuthRes.ik_length, localISIMAuthRes.ck, localISIMAuthRes.ck_length, null, 0);
            continue;
          }
          localIMSCNative.setAKAKeys(paramInt, 2, localISIMAuthRes.res, localISIMAuthRes.res_length, localISIMAuthRes.ik, localISIMAuthRes.ik_length, localISIMAuthRes.ck, localISIMAuthRes.ck_length, localISIMAuthRes.auts, localISIMAuthRes.auts_length);
          continue;
        }
        Log.e("IMSCConfReader", "invalid AKA Auth returned, shutdown registration");
        localIMSCNative.setAKAKeys(paramInt, 0, null, 0, null, 0, null, 0, null, 0);
        continue;
      }
      Log.e("IMSCConfReader", "isimhandle is null, can not do AKA");
      localIMSCNative.setAKAKeys(paramInt, 0, null, 0, null, 0, null, 0, null, 0);
    }
  }

  public boolean getIMSRegistrationDisable()
  {
    return this.mIMSRegDisabled;
  }

  public String[] getPublicIdentity()
  {
    return this.mPublicId;
  }

  public int getSmsFormat()
  {
    return this.mSmsFormat;
  }

  public boolean getSmsOverImsStatus()
  {
    return this.mImsStatus;
  }

  public int getTestMode()
  {
    return this.mTestMode;
  }

  public void onUiccRefresh()
  {
    this.iUiccRefreshCount = 0;
    if (readUiccRefreshParameters())
      this.mHandler.sendMessage(this.mHandler.obtainMessage(10));
    while (true)
    {
      return;
      this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(9), 30000L);
    }
  }

  public void stop()
  {
    if (this.mNvInfo != null);
    try
    {
      this.mNvInfo.unregisterObserver(this.mObserver);
      return;
    }
    catch (RemoteException localRemoteException)
    {
      while (true)
        Log.d("IMSCConfReader", "RemoteException when un-registerObserver");
    }
  }
}

/* Location:           /home/dhacker29/jd/classes_dex2jar.jar
 * Qualified Name:     com.motorola.android.server.ims.IMSCConfReader
 * JD-Core Version:    0.6.0
 */