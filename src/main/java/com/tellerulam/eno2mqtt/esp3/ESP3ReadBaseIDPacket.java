package com.tellerulam.eno2mqtt.esp3;

public class ESP3ReadBaseIDPacket extends ESP3Packet
{
	private static final byte data[]={0x08};

	public ESP3ReadBaseIDPacket()
	{
		super((byte)5,data);
	}

	public String baseID;

	@Override
	public void parseResponse(byte[] resp)
	{
		super.parseResponse(resp);
		baseID=Long.toHexString(decodeN32(1));
	}

}
