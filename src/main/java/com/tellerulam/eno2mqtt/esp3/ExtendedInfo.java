package com.tellerulam.eno2mqtt.esp3;

public class ExtendedInfo
{
	public int subTelNum;
	public long destinationID;
	public int dBm;

	@Override
	public String toString()
	{
		StringBuilder s=new StringBuilder();
		s.append('{');
		s.append(subTelNum);
		s.append('@');
		s.append(Long.toHexString(destinationID));
		s.append('-');
		s.append(dBm);
		s.append("dBm}");
		return s.toString();
	}
}