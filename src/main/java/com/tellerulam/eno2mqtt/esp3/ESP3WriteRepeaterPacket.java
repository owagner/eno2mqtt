package com.tellerulam.eno2mqtt.esp3;

public class ESP3WriteRepeaterPacket extends ESP3Packet
{
	public ESP3WriteRepeaterPacket(int enable,int level)
	{
		super();
		byte data[]=new byte[3];
		data[0]=0x09;
		data[1]=(byte)enable;
		data[2]=(byte)level;
		setData((byte)5,data);
	}

	@Override
	public void parseResponse(byte[] resp)
	{
		super.parseResponse(resp);
	}

}
