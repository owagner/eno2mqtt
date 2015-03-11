package com.tellerulam.eno2mqtt.esp3;

public class ESP3ReadVersionPacket extends ESP3Packet
{
	public String appVersion;
	public String apiVersion;
	public String chipID;
	public String chipVersion;
	public String appDescription;

	private static final byte data[]={0x03};

	public ESP3ReadVersionPacket()
	{
		super((byte)5,data);
	}

	@Override
	public void parseResponse(byte[] resp)
	{
		super.parseResponse(resp);
		appVersion=resp[1]+"."+resp[2]+"."+resp[3]+"."+resp[4];
		apiVersion=resp[5]+"."+resp[6]+"."+resp[7]+"."+resp[8];
		chipID=Long.toHexString(decodeN32(9));
		chipVersion=Long.toHexString(decodeN32(13));
		appDescription=decodeString(17,16);
	}

}
