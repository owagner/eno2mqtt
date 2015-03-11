package com.tellerulam.eno2mqtt.esp3;

public abstract class ESP3ERP1Packet extends ESP3Packet
{
	public ESP3ERP1Packet(byte data[])
	{
		super((byte)1,data);
	}

	public abstract String getPacketType();

	public long senderID;
	public byte status;

	@Override
	public void parseResponse(byte[] resp)
	{
		super.parseResponse(resp);
		// Extract Status and SenderID
		senderID=decodeN32(resp.length-5);
		status=resp[resp.length-1];
	}

	public int getByteValue()
	{
		return b[1]&0xff;
	}

}
