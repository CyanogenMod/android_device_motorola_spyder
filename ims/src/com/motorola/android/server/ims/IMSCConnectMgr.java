package com.motorola.android.server.ims;

import android.content.Context;
import android.util.Log;

public class IMSCConnectMgr
{
  private static final int APN_SWITCH_WAIT_TIMEOUT = 120000;
  protected static final int CONNECTION_CONNECTED = 1;
  protected static final int CONNECTION_MGR_CONN_PENDING = 1;
  protected static final int CONNECTION_MGR_IDLE = 2;
  protected static final int CONNECTION_NOCONNECT = 2;
  protected static final int CONNECTION_PENDING = 3;
  protected static final int IMS_DS_INTERFACE_RAT_EVDO = 0;
  protected static final int IMS_DS_INTERFACE_RAT_LTE = 2;
  protected static final int IMS_DS_INTERFACE_RAT_WIFI = 1;
  private static final String TAG = "IMSCConnectMgr";
  Context mContext = null;
  protected int state = 2;

  static
  {
    try
    {
      classInit();
      return;
    }
    catch (UnsatisfiedLinkError localUnsatisfiedLinkError)
    {
      while (true)
        Log.e("IMSCConnectMgr", "classInit() failed");
    }
  }

  public IMSCConnectMgr(Context paramContext)
  {
  }

  private static native void classInit();

  public void endDataConnection()
  {
  }

  public String getAccessNetworkInfo(int paramInt)
  {
    return "test";
  }

  public native void notifyConnectionStatus(int paramInt1, int paramInt2);

  public void startDataConnection(int paramInt1, int paramInt2)
  {
  }
}

/* Location:           /home/dhacker29/jd/classes_dex2jar.jar
 * Qualified Name:     com.motorola.android.server.ims.IMSCConnectMgr
 * JD-Core Version:    0.6.0
 */