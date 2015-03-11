package com.tellerulam.eno2mqtt.eep;

import com.tellerulam.eno2mqtt.*;
import com.tellerulam.eno2mqtt.DeviceManager.Device;
import com.tellerulam.eno2mqtt.esp3.*;

public abstract class EEP
{
	public static EEP findBySpec(String eepspec)
	{
		switch(eepspec.toUpperCase())
		{
			case "F6-10-00":
				return new EEP_F61000();

			default:
				return null;
		}
	}

	public abstract void handleMessage(DeviceManager.Device d,ESP3ERP1Packet p);

	protected void assertPacketType(ESP3ERP1Packet p,Class<? extends ESP3ERP1Packet> whichPacket)
	{
		if(p.getClass()!=whichPacket)
			throw new IllegalArgumentException("Got unexpected packet type "+p.getPacketType()+" for profile "+this);
	}

	protected void publish(Device d,Object val)
	{
		MQTTHandler.publish(
			d.name,
			val,
			d.getHexID()
		);
	}
}
