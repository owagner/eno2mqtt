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

	@Override
	public String toString()
	{
		StringBuilder s=new StringBuilder();
		s.append("{");
		for(int ix=1;ix<b.length-5;ix++)
		{
			int v=b[ix]&0xff;
			if(v<0x10)
				s.append('0');
			s.append(v);
		}
		s.append("|F=");
		s.append(Long.toHexString(senderID));
		s.append("|R=");
		s.append(status&0xf);
		s.append('}');
		return s.toString();
	}

}
