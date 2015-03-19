package com.tellerulam.eno2mqtt.eep;

import java.util.*;
import java.util.logging.*;

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

	public String getProfileName()
	{
		String n=getClass().getSimpleName().substring(4);
		return n.substring(0,2)+"-"+n.substring(2,4)+"-"+n.substring(4);
	}

	public abstract void handleMessage(Device d,ESP3ERP1Packet p,ExtendedInfo ei);

	protected void assertPacketType(ESP3ERP1Packet p,Class<? extends ESP3ERP1Packet> whichPacket)
	{
		if(p.getClass()!=whichPacket)
			throw new IllegalArgumentException("Got unexpected packet type "+p.getPacketType()+" for profile "+this);
	}

	private static class FilterEntry
	{
		private final Object value;
		private final Date ts;

		FilterEntry(Object value)
		{
			this.value=value;
			this.ts=new Date();
		}

		public boolean checkDupe(Object value)
		{
			if(this.value.equals(value))
			{
				long age=System.currentTimeMillis()-ts.getTime();
				return age<500;
			}
			return false;
		}
	}

	private static final Map<String,FilterEntry> filterMap=new HashMap<>();

	protected void publish(Device d,String suffix,Object val,boolean retain,int dbm)
	{
		String topic=d.name;
		if(suffix!=null)
			topic+="/"+suffix;

		synchronized(filterMap)
		{
			FilterEntry fe=filterMap.get(topic);
			if(fe!=null)
			{
				if(fe.checkDupe(val))
				{
					Logger.getLogger(getClass().getName()).info("Filtering duplicate message to "+d+" "+topic);
					return;
				}
			}
			filterMap.put(topic,new FilterEntry(val));
		}

		MQTTHandler.publish(
			topic,
			val,
			d.getHexID(),
			retain,
			dbm
		);
	}
}
