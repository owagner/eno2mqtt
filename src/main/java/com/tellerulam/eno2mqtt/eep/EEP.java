package com.tellerulam.eno2mqtt.eep;

import com.tellerulam.eno2mqtt.*;
import com.tellerulam.eno2mqtt.esp3.*;

public abstract class EEP
{
	public static EEP findBySpec(String eepspec)
	{
		switch(eepspec.toUpperCase())
		{
			case "F6-02-01":
				return new EEP_F602xx(1);
			case "F6-02-02":
				return new EEP_F602xx(2);
			case "F6-02-03":
				return new EEP_F602xx(3);
			case "F6-02-04":
				return new EEP_F602xx(4);

			case "F6-03-01":
				return new EEP_F603xx(1);
			case "F6-03-02":
				return new EEP_F603xx(2);

			case "F6-10-00":
				return new EEP_F61000();

			default:
				return null;
		}
	}

	public abstract void handleMessage(Device d,ESP3ERP1Packet p);

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
	protected void publish(Device d,String suffix,Object val,boolean retain)
	{
		MQTTHandler.publish(
			d.name+'/'+suffix,
			val,
			d.getHexID()
		);
	}
}
