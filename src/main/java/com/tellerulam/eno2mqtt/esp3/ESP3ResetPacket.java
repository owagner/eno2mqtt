package com.tellerulam.eno2mqtt.esp3;

public class ESP3ResetPacket extends ESP3Packet
{
	private static final byte data[]={0x02};

	public ESP3ResetPacket()
	{
		super((byte)5,data);
	}

	@Override
	public void parseResponse(byte[] resp)
	{
		super.parseResponse(resp);
	}

}
