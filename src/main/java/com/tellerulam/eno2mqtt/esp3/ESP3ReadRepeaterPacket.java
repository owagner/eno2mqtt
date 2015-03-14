package com.tellerulam.eno2mqtt.esp3;

public class ESP3ReadRepeaterPacket extends ESP3Packet
{
	private static final byte data[]={0x0a};

	public ESP3ReadRepeaterPacket()
	{
		super((byte)5,data);
	}

	public int repEnable;
	public int repLevel;

	@Override
	public void parseResponse(byte[] resp)
	{
		super.parseResponse(resp);
		repEnable=b[1];
		repLevel=b[2];
	}

	public String getRepeaterEnable()
	{
		switch(repEnable)
		{
			case 0:
				return "OFF";
			case 1:
				return "ALL";
			case 2:
				return "FILTERED";
			default:
				return "UNKNOWN"+repEnable;
		}
	}

	public String getRepeaterLevel()
	{
		if(repLevel==0)
			return "OFF";
		return "LEVEL"+repLevel;
	}

}
