package com.motorola.android.server.ims;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class IMSCMgrFTSInfo
  implements Parcelable
{
  public static final Parcelable.Creator<IMSCMgrFTSInfo> CREATOR = new Parcelable.Creator()
  {
    public IMSCMgrFTSInfo createFromParcel(Parcel paramParcel)
    {
      IMSCMgrFTSInfo localIMSCMgrFTSInfo = new IMSCMgrFTSInfo();
      IMSCMgrFTSInfo.access$002(localIMSCMgrFTSInfo, paramParcel.readString());
      IMSCMgrFTSInfo.access$102(localIMSCMgrFTSInfo, paramParcel.readInt());
      IMSCMgrFTSInfo.access$202(localIMSCMgrFTSInfo, paramParcel.readString());
      IMSCMgrFTSInfo.access$302(localIMSCMgrFTSInfo, paramParcel.readString());
      IMSCMgrFTSInfo.access$402(localIMSCMgrFTSInfo, paramParcel.readString());
      IMSCMgrFTSInfo.access$502(localIMSCMgrFTSInfo, paramParcel.readLong());
      return localIMSCMgrFTSInfo;
    }

    public IMSCMgrFTSInfo[] newArray(int paramInt)
    {
      return new IMSCMgrFTSInfo[paramInt];
    }
  };
  private String mLocalAddress;
  private int mMemUsage;
  private String mProxy;
  private String mPublicURI;
  private String mRegStatus;
  private long mRegTime;

  public int describeContents()
  {
    return 0;
  }

  public String getLocalAddress()
  {
    return this.mLocalAddress;
  }

  public int getMemUsage()
  {
    return this.mMemUsage;
  }

  public String getProxy()
  {
    return this.mProxy;
  }

  public String getPublicURI()
  {
    return this.mPublicURI;
  }

  public String getRegStatus()
  {
    return this.mRegStatus;
  }

  public long getRegTime()
  {
    return this.mRegTime;
  }

  public void setLocalAddress(String paramString)
  {
    this.mLocalAddress = paramString;
  }

  public void setMemUsage(int paramInt)
  {
    this.mMemUsage = paramInt;
  }

  public void setProxy(String paramString)
  {
    this.mProxy = paramString;
  }

  public void setPublicURI(String paramString)
  {
    this.mPublicURI = paramString;
  }

  public void setRegStatus(String paramString)
  {
    this.mRegStatus = paramString;
  }

  public void setRegTime(long paramLong)
  {
    this.mRegTime = paramLong;
  }

  public String toString()
  {
    StringBuffer localStringBuffer = new StringBuffer();
    localStringBuffer.append("IMS-FTS Info:\n");
    localStringBuffer.append("mLocalAddress: ").append(this.mLocalAddress).append('\n');
    localStringBuffer.append("mMemUsage: ").append(this.mMemUsage).append('\n');
    localStringBuffer.append("mProxy: ").append(this.mProxy).append('\n');
    localStringBuffer.append("mPublicURI: ").append(this.mPublicURI).append('\n');
    localStringBuffer.append("mRegStatus: ").append(this.mRegStatus).append('\n');
    localStringBuffer.append("mRegTime: ").append(this.mRegTime).append('\n');
    return localStringBuffer.toString();
  }

  public void writeToParcel(Parcel paramParcel, int paramInt)
  {
    paramParcel.writeString(this.mLocalAddress);
    paramParcel.writeInt(this.mMemUsage);
    paramParcel.writeString(this.mProxy);
    paramParcel.writeString(this.mPublicURI);
    paramParcel.writeString(this.mRegStatus);
    paramParcel.writeLong(this.mRegTime);
  }
}

/* Location:           /home/dhacker29/jd/classes_dex2jar.jar
 * Qualified Name:     com.motorola.android.server.ims.IMSCMgrFTSInfo
 * JD-Core Version:    0.6.0
 */