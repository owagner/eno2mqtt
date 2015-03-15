package com.tellerulam.eno2mqtt.esp3;

public class ESP3ReadDutycyclePacket extends ESP3Packet
{
	private static final byte data[]={0x23};

	public ESP3ReadDutycyclePacket()
	{
		super((byte)5,data);
	}

	public int availableCycle;
	public int slots;
	public int slotPeriod;
	public int slotLeft;
	public int loadAfterActual;

	@Override
	public void parseResponse(byte[] resp)
	{
		super.parseResponse(resp);
		if(b[0]==0)
		{
			availableCycle=b[1];
			slots=b[2];
			slotPeriod=decodeN16(3);
			slotLeft=decodeN16(5);
			loadAfterActual=b[7];
		}
	}

}
