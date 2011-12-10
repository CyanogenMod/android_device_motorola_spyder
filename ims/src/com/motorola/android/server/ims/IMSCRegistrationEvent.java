package com.motorola.android.server.ims;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.text.TextUtils;

public class IMSCRegistrationEvent
  implements Parcelable
{
  public static final int AKA_AUTH_REQUEST = 11;
  public static final int BAD_REQUEST = 0;
  public static final int BUSY_EVERYWHERE = 2;
  public static final int BUSY_HERE = 1;
  public static final Parcelable.Creator<IMSCRegistrationEvent> CREATOR = new Parcelable.Creator()
  {
    public IMSCRegistrationEvent createFromParcel(Parcel paramParcel)
    {
      long l = paramParcel.readLong();
      int i = paramParcel.readInt();
      byte[] arrayOfByte = new byte[paramParcel.readInt()];
      paramParcel.readByteArray(arrayOfByte);
      return new IMSCRegistrationEvent(l, i, arrayOfByte);
    }

    public IMSCRegistrationEvent[] newArray(int paramInt)
    {
      return new IMSCRegistrationEvent[paramInt];
    }
  };
  public static final int DECLINE = 3;
  public static final int EVENT_MISSING = 4;
  public static final int FORBIDDEN = 5;
  public static final int GENERIC_FAIL = 0;
  public static final int GONE = 6;
  public static final int INTERNAL_ERROR = 7;
  public static final int LOCAL_FAILURE = 8;
  public static final int NOT_ACCEPTABLE = 10;
  public static final int NOT_ACCEPTABLE_HERE = 11;
  public static final int NOT_FOUND = 12;
  public static final int NO_TRANSAC = 9;
  public static final int OK = 13;
  public static final int OPERATION_COMPLETED = 10;
  public static final int PROXY_AUTH_REQUIRED = 14;
  public static final int REGISTERING = 0;
  public static final int REGISTRATION_FAILED = 2;
  public static final int REGISTRATION_REMOVED_BY_NETWORK = 3;
  public static final int REGISTRATION_SUCCESSFUL = 1;
  public static final int REMOTE_FAILURE = 15;
  public static final int REQ_TIMEOUT = 16;
  public static final int RETRY_403_FAIL = 1;
  public static final int RETRY_404_FAIL = 2;
  public static final int RE_REGISTERING = 4;
  public static final int RE_REGISTRATION_FAILED = 6;
  public static final int RE_REGISTRATION_SUCCESSFUL = 5;
  public static final int TEMP_UNAVAIL = 17;
  public static final int UNAUTHORIZED = 18;
  public static final int UNKNOWN_REASON = -1;
  public static final int UNSUPPORTED_MEDIA = 19;
  public static final int UN_REGISTERING = 7;
  public static final int UN_REGISTRATION_FAILED = 9;
  public static final int UN_REGISTRATION_SUCCESSFUL = 8;
  private byte[] mEventData;
  private int mEventType;
  private long mRegId;

  public IMSCRegistrationEvent()
  {
  }

  public IMSCRegistrationEvent(long paramLong, int paramInt, byte[] paramArrayOfByte)
  {
    this.mRegId = paramLong;
    this.mEventType = paramInt;
    if ((paramArrayOfByte == null) || (paramArrayOfByte.length == 0))
      this.mEventData = null;
    while (true)
    {
      return;
      this.mEventData = new byte[paramArrayOfByte.length];
      for (int i = 0; i < paramArrayOfByte.length; i++)
        this.mEventData[i] = paramArrayOfByte[i];
    }
  }

  public static String parseEventTypeStr(int paramInt)
  {
    IMSCRegistrationEvent.1EventType[] arrayOf1EventType = new IMSCRegistrationEvent.1EventType[12];
    arrayOf1EventType[0] = new IMSCRegistrationEvent.1EventType(0, "0 -- Registering");
    arrayOf1EventType[1] = new IMSCRegistrationEvent.1EventType(1, "1 -- Registration_successful");
    arrayOf1EventType[2] = new IMSCRegistrationEvent.1EventType(2, "2 -- Registration_failed");
    arrayOf1EventType[3] = new IMSCRegistrationEvent.1EventType(3, "3 -- Registration_removed_by_network");
    arrayOf1EventType[4] = new IMSCRegistrationEvent.1EventType(4, "4 -- Re_Registering");
    arrayOf1EventType[5] = new IMSCRegistrationEvent.1EventType(5, "5 -- Re_Registration_successful");
    arrayOf1EventType[6] = new IMSCRegistrationEvent.1EventType(6, "6 -- Re_Registration_failed");
    arrayOf1EventType[7] = new IMSCRegistrationEvent.1EventType(7, "7 -- Un_Registering");
    arrayOf1EventType[8] = new IMSCRegistrationEvent.1EventType(8, "8 -- Un_Registration_successful");
    arrayOf1EventType[9] = new IMSCRegistrationEvent.1EventType(9, "9 -- Un_Registration_failed");
    arrayOf1EventType[10] = new IMSCRegistrationEvent.1EventType(10, "10 -- Operation_completed");
    arrayOf1EventType[11] = new IMSCRegistrationEvent.1EventType(11, "11 -- AKA_auth_request");
    String str = null;
    for (int i = 0; ; i++)
    {
      if (i < arrayOf1EventType.length)
      {
        if (paramInt != arrayOf1EventType[i].type)
          continue;
        str = arrayOf1EventType[i].description;
      }
      if (TextUtils.isEmpty(str))
        str = paramInt + " -- unkown_type";
      return str;
    }
  }

  public static String parseFailedReasonCode(int paramInt)
  {
    IMSCRegistrationEvent.1FailureReason[] arrayOf1FailureReason = new IMSCRegistrationEvent.1FailureReason[21];
    arrayOf1FailureReason[0] = new IMSCRegistrationEvent.1FailureReason(-1, "-1 -- unknown_reason");
    arrayOf1FailureReason[1] = new IMSCRegistrationEvent.1FailureReason(0, "0 -- 400 Bad Request");
    arrayOf1FailureReason[2] = new IMSCRegistrationEvent.1FailureReason(1, "1 -- 486 Busy Here");
    arrayOf1FailureReason[3] = new IMSCRegistrationEvent.1FailureReason(2, "2 -- 600 Busy Everywhere");
    arrayOf1FailureReason[4] = new IMSCRegistrationEvent.1FailureReason(3, "3 -- 603 Decline");
    arrayOf1FailureReason[5] = new IMSCRegistrationEvent.1FailureReason(4, "4 -- 489 Event missing");
    arrayOf1FailureReason[6] = new IMSCRegistrationEvent.1FailureReason(5, "5 -- 403 Forbidden");
    arrayOf1FailureReason[7] = new IMSCRegistrationEvent.1FailureReason(6, "6 -- 410 Gone");
    arrayOf1FailureReason[8] = new IMSCRegistrationEvent.1FailureReason(7, "7 -- 500 Internal Error");
    arrayOf1FailureReason[9] = new IMSCRegistrationEvent.1FailureReason(8, "8 -- Any other unclassified local failure");
    arrayOf1FailureReason[10] = new IMSCRegistrationEvent.1FailureReason(9, "9 -- 481 No Transaction");
    arrayOf1FailureReason[11] = new IMSCRegistrationEvent.1FailureReason(10, "10 -- 406 Not Acceptable");
    arrayOf1FailureReason[12] = new IMSCRegistrationEvent.1FailureReason(11, "11 -- 488 Not Acceptable Here");
    arrayOf1FailureReason[13] = new IMSCRegistrationEvent.1FailureReason(12, "12 -- 404 not Found");
    arrayOf1FailureReason[14] = new IMSCRegistrationEvent.1FailureReason(13, "13 -- 200 OK / 202 ACCEPTED");
    arrayOf1FailureReason[15] = new IMSCRegistrationEvent.1FailureReason(14, "14 -- 407 Proxy Authentication Required");
    arrayOf1FailureReason[16] = new IMSCRegistrationEvent.1FailureReason(15, "15 -- Any other unclassified remote failure");
    arrayOf1FailureReason[17] = new IMSCRegistrationEvent.1FailureReason(16, "16 -- 408 Request Timeout");
    arrayOf1FailureReason[18] = new IMSCRegistrationEvent.1FailureReason(17, "17 -- 480 Temporarily Unavailable");
    arrayOf1FailureReason[19] = new IMSCRegistrationEvent.1FailureReason(18, "18 -- 401 Unauthorized");
    arrayOf1FailureReason[20] = new IMSCRegistrationEvent.1FailureReason(19, "19 -- 415 Unsupported Media Type");
    String str = null;
    for (int i = 0; ; i++)
    {
      if (i < arrayOf1FailureReason.length)
      {
        if (paramInt != arrayOf1FailureReason[i].code)
          continue;
        str = arrayOf1FailureReason[i].reason;
      }
      if (TextUtils.isEmpty(str))
        str = paramInt + " -- unkown_code";
      return str;
    }
  }

  public int describeContents()
  {
    return 0;
  }

  public byte[] getEventData()
  {
    return this.mEventData;
  }

  public int getEventType()
  {
    return this.mEventType;
  }

  public IMSCOperationCompletedData getOperationCompletedData()
  {
    IMSCNative localIMSCNative = IMSCNative.getInstance();
    if (localIMSCNative == null);
    for (IMSCOperationCompletedData localIMSCOperationCompletedData = null; ; localIMSCOperationCompletedData = (IMSCOperationCompletedData)(IMSCOperationCompletedData)localIMSCNative.imscParseOperationCompletedData(this.mEventData))
      return localIMSCOperationCompletedData;
  }

  public long getRegId()
  {
    return this.mRegId;
  }

  public IMSCRegistrationFailedReason getRegistrationFailedReason()
  {
    IMSCNative localIMSCNative = IMSCNative.getInstance();
    if (localIMSCNative == null);
    for (IMSCRegistrationFailedReason localIMSCRegistrationFailedReason = null; ; localIMSCRegistrationFailedReason = (IMSCRegistrationFailedReason)(IMSCRegistrationFailedReason)localIMSCNative.imscParseRegistrationFailedReason(this.mEventData))
      return localIMSCRegistrationFailedReason;
  }

  public IMSCRegistrationSuccessfulData getRegistrationSuccessfulData()
  {
    IMSCNative localIMSCNative = IMSCNative.getInstance();
    if (localIMSCNative == null);
    for (IMSCRegistrationSuccessfulData localIMSCRegistrationSuccessfulData = null; ; localIMSCRegistrationSuccessfulData = (IMSCRegistrationSuccessfulData)(IMSCRegistrationSuccessfulData)localIMSCNative.imscParseRegistrationSuccessfulData(this.mEventData))
      return localIMSCRegistrationSuccessfulData;
  }

  public String getServerName()
  {
    IMSCNative localIMSCNative = IMSCNative.getInstance();
    if (localIMSCNative == null);
    for (String str = null; ; str = localIMSCNative.imscParseServerName(this.mEventData))
      return str;
  }

  public void setEventData(byte[] paramArrayOfByte)
  {
    if ((paramArrayOfByte == null) || (paramArrayOfByte.length == 0))
      this.mEventData = null;
    while (true)
    {
      return;
      this.mEventData = new byte[paramArrayOfByte.length];
      for (int i = 0; i < paramArrayOfByte.length; i++)
        this.mEventData[i] = paramArrayOfByte[i];
    }
  }

  public void setEventType(int paramInt)
  {
    this.mEventType = paramInt;
  }

  public void setRegId(long paramLong)
  {
    this.mRegId = paramLong;
  }

  public String toString()
  {
    StringBuffer localStringBuffer = new StringBuffer();
    localStringBuffer.append("IMS-Registration callback event:\n");
    localStringBuffer.append("mRegId: ").append(this.mRegId).append('\n');
    localStringBuffer.append("mEventType: ").append(parseEventTypeStr(this.mEventType)).append('\n');
    localStringBuffer.append("mEventData: ");
    if ((this.mEventData == null) || (this.mEventData.length == 0))
    {
      localStringBuffer.append("null");
      localStringBuffer.append('\n');
      return localStringBuffer.toString();
    }
    int i = 0;
    label105: int j;
    if (i < this.mEventData.length)
    {
      if (i % 20 == 0)
        localStringBuffer.append('\n');
      j = 0xFF & this.mEventData[i];
      if ((j >= 128) || (!Character.isLetterOrDigit(j)))
        break label218;
      String str2 = Character.toString((char)j);
      if (str2.length() == 1)
        str2 = "_" + str2;
      localStringBuffer.append(str2).append(" ");
    }
    while (true)
    {
      i++;
      break label105;
      break;
      label218: String str1 = Integer.toHexString(j);
      if (str1.length() == 1)
        str1 = '0' + str1;
      localStringBuffer.append(str1.toUpperCase()).append(" ");
    }
  }

  public void writeToParcel(Parcel paramParcel, int paramInt)
  {
    paramParcel.writeLong(this.mRegId);
    paramParcel.writeInt(this.mEventType);
    paramParcel.writeInt(this.mEventData.length);
    paramParcel.writeByteArray(this.mEventData);
  }

  public static class IMSCRegistrationSuccessfulData
  {
    public long mExpiration;
    public String mServerName;
  }

  public static class IMSCRegistrationFailedReason
  {
    public int mExtensionCode;
    public int mReasonCode;
  }

  public static class IMSCOperationCompletedData
  {
    public long mOperationId;
    public boolean mResult;
  }
}

/* Location:           /home/dhacker29/jd/classes_dex2jar.jar
 * Qualified Name:     com.motorola.android.server.ims.IMSCRegistrationEvent
 * JD-Core Version:    0.6.0
 */