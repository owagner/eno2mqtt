package com.tellerulam.eno2mqtt.esp3;

public class ESP3ERP1_RPSPacket extends ESP3ERP1Packet
{
	public ESP3ERP1_RPSPacket()
	{
		super(new byte[0]);
	}

	public ESP3ERP1_RPSPacket(byte payload)
	{
		// TODO implement this?
		super(new byte[1]);
	}

	@Override
	public void parseResponse(byte[] resp)
	{
		super.parseResponse(resp);
		// Extract Status and SenderID
		senderID=decodeN32(resp.length-5);
		status=resp[resp.length-1];
	}

	@Override
	public String getPacketType()
	{
		return "RPS";
	}

}
