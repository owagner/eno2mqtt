package com.tellerulam.eno2mqtt.esp3;

import java.io.*;
import java.nio.charset.*;

import com.tellerulam.eno2mqtt.*;

public abstract class ESP3Packet
{
	protected boolean retOK;
	public boolean isResponseOK()
	{
		return retOK;
	}

	protected byte b[];

	protected long decodeN32(int offs)
	{
		long c=0;
		for(int n=0;n<4;n++)
			c=c<<8|(b[offs+n]&0xff);
		return c;
	}

	protected String decodeString(int offs,int maxLength)
	{
		int n;
		for(n=0;n<maxLength;n++)
			if(b[offs+n]==0)
				break;
		return new String(b,offs,n,StandardCharsets.ISO_8859_1);
	}

	public ESP3Packet(byte type,byte data[])
	{
		b=new byte[data.length+6+1];

		b[0]=(byte)0x55;
		b[1]=0;
		b[2]=(byte)data.length;		// Length
		b[3]=0;						// Optional length
		b[4]=type;					// Packet type
		b[5]=CRC8.calcCRC(b, 1, 4);
		System.arraycopy(data,0,b,6,data.length);
		b[data.length+6]=CRC8.calcCRC(b, 1, data.length+5);
	}

	public void send(OutputStream os) throws IOException
	{
		os.write(b);
	}

	public void parseResponse(byte resp[])
	{
		retOK=(resp[0]==0);
		b=resp;
	}
}
