package com.motorola.android.server.ims;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.provider.Settings.System;
import android.util.Log;
import com.motorola.android.ims.IIMSServer.Stub;

public class IMSCServer extends Service
{
  private static final String ACTION_IMS_DEREGISTER = "com.motorola.ims.action.deregister";
  private static final String ACTION_IMS_DEREG_NEEDED = "com.motorola.internal.intent.ACTION_IMS_DEREG_NEEDED";
  private static final String ACTION_IMS_REGISTER = "com.motorola.ims.action.register";
  public static final String ACTION_UICC_REFRESH = "com.motorola.telephony.uicc.UICC_REFRESH";
  private static final String TAG = IMSCServer.class.getSimpleName();
  private final IIMSServer.Stub mBinder = new IIMSServer.Stub()
  {
    public IBinder getNetworkService()
    {
      return IMSCServer.this.mIMSCNetwork;
    }

    public IBinder getSMSService()
    {
      return IMSCServer.this.mIMSCSmsService;
    }
  };
  private final BroadcastReceiver mHipriReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context paramContext, Intent paramIntent)
    {
      String str = paramIntent.getAction();
      Log.i(IMSCServer.TAG, "Receive intent: " + paramIntent + " Action: " + str);
      if ("android.intent.action.ACTION_SHUTDOWN".equals(str))
        IMSCServer.this.onShutdown();
      while (true)
      {
        return;
        if (("com.motorola.ims.action.link-reconnect".equals(str)) && (IMSCServer.this.mIMSCNetwork != null))
        {
          IMSCServer.this.mIMSCNetwork.onReconnectTimer();
          continue;
        }
      }
    }
  };
  private IMSCNetwork mIMSCNetwork;
  private IMSCSmsService mIMSCSmsService;
  private final BroadcastReceiver mReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context paramContext, Intent paramIntent)
    {
      String str1 = paramIntent.getAction();
      Log.i(IMSCServer.TAG, "Receive intent: " + paramIntent + " Action: " + str1);
      if ((Intent.ACTION_AIRPLANE_MODE_CHANGED).equals(str1))
        IMSCServer.this.onAirplaneModeChanged();
      while (true)
      {
        return;
        if ((TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED).equals(str1))
        {
          if (IMSCServer.this.mIMSCNetwork == null)
            continue;
          IMSCServer.this.mIMSCNetwork.onConnectivityChanged(paramIntent);
          continue;
        }
        if ((TelephonyIntents.ACTION_SERVICE_STATE_CHANGED).equals(str1))
        {
          if (IMSCServer.this.mIMSCNetwork == null)
            continue;
          IMSCServer.this.mIMSCNetwork.onNetworkStateChanged();
          continue;
        }
        if ((TelephonyIntents.ACTION_SIM_STATE_CHANGE).equals(str1))
        {
          String str2 = paramIntent.getStringExtra("ss");
          Log.i(IMSCServer.TAG, "sim extra state:" + str2);
          if ((str2 != null) && ((str2.equals("READY")) || (str2.equals("LOADED"))) && (IMSCServer.this.mIMSCNetwork != null))
          {
            IMSCServer.this.mIMSCNetwork.onSimReady();
            continue;
          }
          if ((str2 == null) || (!str2.equals("NOT_READY")) || (IMSCServer.this.mIMSCNetwork == null))
            continue;
          IMSCServer.this.mIMSCNetwork.onSimNotReady();
          continue;
        }
        if ("com.motorola.ims.action.mgrInit".equals(str1))
        {
          if (IMSCServer.this.mIMSCNetwork == null)
            continue;
          IMSCServer.this.mIMSCNetwork.startImsMgrInit();
          continue;
        }
        if ("com.motorola.internal.intent.ACTION_IMS_DEREG_NEEDED".equals(str1))
        {
          if (IMSCServer.this.mIMSCNetwork == null)
            continue;
          IMSCServer.this.mIMSCNetwork.deRegister();
          continue;
        }
        if ("com.motorola.ims.action.register".equals(str1))
        {
          if (IMSCServer.this.mIMSCNetwork == null)
            continue;
          IMSCServer.this.mIMSCNetwork.doRegister();
          continue;
        }
        if ("com.motorola.ims.action.deregister".equals(str1))
        {
          if (IMSCServer.this.mIMSCNetwork == null)
            continue;
          IMSCServer.this.mIMSCNetwork.deRegister();
          continue;
        }
        if ("com.motorola.ims.action.nativetimer".equals(str1))
        {
          if (IMSCServer.this.mIMSCNetwork == null)
            continue;
          IMSCServer.this.mIMSCNetwork.onNativeTimer();
          continue;
        }
        if ((TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED).equals(str1))
        {
          if (IMSCServer.this.mIMSCNetwork == null)
            continue;
          IMSCServer.this.mIMSCNetwork.onMobileDataEnabledChanged(paramIntent);
          continue;
        }
        if ((!"com.motorola.telephony.uicc.UICC_REFRESH".equals(str1)) || (IMSCServer.this.mIMSCNetwork == null))
          continue;
        IMSCServer.this.mIMSCNetwork.onUiccRefresh(paramIntent);
      }
    }
  };

  private void onAirplaneModeChanged()
  {
    if (Settings.System.getInt(getContentResolver(), "airplane_mode_on", 0) == 1);
    for (boolean bool = true; ; bool = false)
    {
      Log.i(TAG, "onAirplaneModeChanged(): " + bool);
      if ((!bool) || (this.mIMSCSmsService != null))
        this.mIMSCSmsService.onAirplaneModeChanged(bool);
      if (this.mIMSCNetwork != null)
        this.mIMSCNetwork.onAirplaneModeChanged(bool);
      return;
    }
  }

  private void onShutdown()
  {
    Log.i(TAG, "Close services and deregister all clients before shutdown...");
    if (this.mIMSCSmsService != null)
      this.mIMSCSmsService.onShutdown();
    if (this.mIMSCNetwork != null)
      this.mIMSCNetwork.onShutdown();
  }

  public IBinder onBind(Intent paramIntent)
  {
    return this.mBinder;
  }

  public void onCreate()
  {
    Log.i(TAG, "onCreate");
    super.onCreate();
    IntentFilter localIntentFilter1 = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
    localIntentFilter1.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
    localIntentFilter1.addAction(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
    localIntentFilter1.addAction(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED);
    localIntentFilter1.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
    localIntentFilter1.addAction("com.motorola.ims.action.mgrInit");
    localIntentFilter1.addAction("com.motorola.internal.intent.ACTION_IMS_DEREG_NEEDED");
    localIntentFilter1.addAction("com.motorola.ims.action.nativetimer");
    localIntentFilter1.addAction("com.motorola.ims.action.register");
    localIntentFilter1.addAction("com.motorola.ims.action.deregister");
    localIntentFilter1.addAction("com.motorola.telephony.uicc.UICC_REFRESH");
    registerReceiver(this.mReceiver, localIntentFilter1);
    IntentFilter localIntentFilter2 = new IntentFilter("android.intent.action.ACTION_SHUTDOWN");
    localIntentFilter2.addAction("com.motorola.ims.action.link-reconnect");
    localIntentFilter2.setPriority(200);
    registerReceiver(this.mHipriReceiver, localIntentFilter2);
    this.mIMSCNetwork = IMSCNetwork.getInstance();
    if (this.mIMSCNetwork != null)
      this.mIMSCNetwork.setContext(this);
    this.mIMSCSmsService = IMSCSmsService.getInstance();
  }

  public void onDestroy()
  {
    Log.i(TAG, "onDestroy");
    try
    {
      unregisterReceiver(this.mHipriReceiver);
      unregisterReceiver(this.mReceiver);
      onShutdown();
      if (this.mIMSCNetwork != null)
        this.mIMSCNetwork.onDestroy();
      IMSCNative localIMSCNative = IMSCNative.getInstance();
      if (localIMSCNative != null)
        localIMSCNative.imscMgrDeInit();
      super.onDestroy();
      return;
    }
    catch (Exception localException)
    {
      while (true)
        Log.e(TAG, "Exception caught to unregisterReceiver:" + localException);
    }
  }
}

/* Location:           /home/dhacker29/jd/classes_dex2jar.jar
 * Qualified Name:     com.motorola.android.server.ims.IMSCServer
 * JD-Core Version:    0.6.0
 */
