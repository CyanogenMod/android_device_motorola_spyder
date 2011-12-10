package com.motorola.android.server.ims;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class IMSCSmsMoEvent
  implements Parcelable
{
  public static final Parcelable.Creator<IMSCSmsMoEvent> CREATOR = new Parcelable.Creator()
  {
    public IMSCSmsMoEvent createFromParcel(Parcel paramParcel)
    {
      long l1 = paramParcel.readLong();
      long l2 = paramParcel.readLong();
      int i = paramParcel.readInt();
      int j = paramParcel.readInt();
      String str = paramParcel.readString();
      int k = paramParcel.readInt();
      byte[] arrayOfByte = new byte[paramParcel.readInt()];
      paramParcel.readByteArray(arrayOfByte);
      return new IMSCSmsMoEvent(l1, l2, i, j, str, k, arrayOfByte);
    }

    public IMSCSmsMoEvent[] newArray(int paramInt)
    {
      return new IMSCSmsMoEvent[paramInt];
    }
  };
  private int mMsgFormat;
  private long mMsgId;
  private long mRegId;
  private int mReplySeq;
  private byte[] mSmsBody;
  private int mStatusCode = -2;
  private String mToAddr;

  public IMSCSmsMoEvent()
  {
  }

  public IMSCSmsMoEvent(long paramLong1, long paramLong2, int paramInt1, int paramInt2, String paramString, int paramInt3, byte[] paramArrayOfByte)
  {
    this.mRegId = paramLong1;
    this.mMsgId = paramLong2;
    this.mReplySeq = paramInt1;
    this.mStatusCode = paramInt2;
    this.mToAddr = paramString;
    this.mMsgFormat = paramInt3;
    if ((paramArrayOfByte == null) || (paramArrayOfByte.length == 0))
      this.mSmsBody = null;
    while (true)
    {
      return;
      this.mSmsBody = new byte[paramArrayOfByte.length];
      for (int i = 0; i < paramArrayOfByte.length; i++)
        this.mSmsBody[i] = paramArrayOfByte[i];
    }
  }

  public int describeContents()
  {
    return 0;
  }

  public int getMsgFormat()
  {
    return this.mMsgFormat;
  }

  public String getMsgFormatStr()
  {
    String str = "3gpp2";
    if (this.mMsgFormat == 0)
      str = "0 -- 3gpp2";
    while (true)
    {
      return str;
      if (this.mMsgFormat == 1)
      {
        str = "1 -- 3gpp";
        continue;
      }
      if (this.mMsgFormat != 2)
        continue;
      str = "2 -- Trans";
    }
  }

  public long getMsgId()
  {
    return this.mMsgId;
  }

  public long getRegId()
  {
    return this.mRegId;
  }

  public int getReplySeq()
  {
    return this.mReplySeq;
  }

  public byte[] getSmsBody()
  {
    return this.mSmsBody;
  }

  public int getStatusCode()
  {
    return this.mStatusCode;
  }

  public String getStatusStr()
  {
    if (this.mStatusCode == -2);
    for (String str = "-2 -- not initialized"; ; str = IMSCRegistrationEvent.parseFailedReasonCode(this.mStatusCode))
      return str;
  }

  public String getToAddress()
  {
    return this.mToAddr;
  }

  public void setMsgFormat(int paramInt)
  {
    this.mMsgFormat = paramInt;
  }

  public void setMsgId(long paramLong)
  {
    this.mMsgId = paramLong;
  }

  public void setRegId(long paramLong)
  {
    this.mRegId = paramLong;
  }

  public void setReplySeq(int paramInt)
  {
    this.mReplySeq = paramInt;
  }

  public void setSmsBody(byte[] paramArrayOfByte)
  {
    if ((paramArrayOfByte == null) || (paramArrayOfByte.length == 0))
      this.mSmsBody = null;
    while (true)
    {
      return;
      this.mSmsBody = new byte[paramArrayOfByte.length];
      for (int i = 0; i < paramArrayOfByte.length; i++)
        this.mSmsBody[i] = paramArrayOfByte[i];
    }
  }

  public void setStatusCode(int paramInt)
  {
    this.mStatusCode = paramInt;
  }

  public void setToAddress(String paramString)
  {
    this.mToAddr = paramString;
  }

  public String toString()
  {
    StringBuffer localStringBuffer = new StringBuffer();
    localStringBuffer.append("IMS-SMS-MO event:\n");
    localStringBuffer.append("mRegId: ").append(this.mRegId).append('\n');
    localStringBuffer.append("mMsgId: ").append("0x").append(Long.toHexString(this.mMsgId)).append('\n');
    localStringBuffer.append("mReplySeq: ").append(this.mReplySeq).append('\n');
    localStringBuffer.append("mStatusCode: ").append(getStatusStr()).append('\n');
    localStringBuffer.append("mToAddr: ").append(this.mToAddr).append('\n');
    localStringBuffer.append("mMsgFormat: ").append(getMsgFormatStr()).append('\n');
    localStringBuffer.append("mSmsBody: ");
    if ((this.mSmsBody == null) || (this.mSmsBody.length == 0))
    {
      localStringBuffer.append("null");
      localStringBuffer.append('\n');
      return localStringBuffer.toString();
    }
    int i = 0;
    label181: int j;
    if (i < this.mSmsBody.length)
    {
      if (i % 20 == 0)
        localStringBuffer.append('\n');
      j = 0xFF & this.mSmsBody[i];
      if ((j >= 128) || (!Character.isLetterOrDigit(j)))
        break label292;
      String str2 = Character.toString((char)j);
      if (str2.length() == 1)
        str2 = "_" + str2;
      localStringBuffer.append(str2).append(" ");
    }
    while (true)
    {
      i++;
      break label181;
      break;
      label292: String str1 = Integer.toHexString(j);
      if (str1.length() == 1)
        str1 = '0' + str1;
      localStringBuffer.append(str1.toUpperCase()).append(" ");
    }
  }

  public void writeToParcel(Parcel paramParcel, int paramInt)
  {
    paramParcel.writeLong(this.mRegId);
    paramParcel.writeLong(this.mMsgId);
    paramParcel.writeInt(this.mReplySeq);
    paramParcel.writeInt(this.mStatusCode);
    paramParcel.writeString(this.mToAddr);
    paramParcel.writeInt(this.mMsgFormat);
    paramParcel.writeInt(this.mSmsBody.length);
    paramParcel.writeByteArray(this.mSmsBody);
  }
}

/* Location:           /home/dhacker29/jd/classes_dex2jar.jar
 * Qualified Name:     com.motorola.android.server.ims.IMSCSmsMoEvent
 * JD-Core Version:    0.6.0
 */