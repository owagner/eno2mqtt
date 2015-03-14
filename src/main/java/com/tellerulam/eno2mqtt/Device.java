package com.tellerulam.eno2mqtt;

import java.util.*;

import com.tellerulam.eno2mqtt.eep.*;

public class Device
{
	public final Long id;
	public final EEP eep;
	public final String name;
	protected Device(Long id, EEP eep, String name)
	{
		this.id=id;
		this.eep=eep;
		this.name=name;
	}
	public String getHexID()
	{
		return Long.toHexString(id.longValue());
	}

	/* Generic state storage */
	private Map<String,Object> state;

	public synchronized Map<String,Object> getStates()
	{
		if(state!=null)
			return new HashMap<>(state);
		else
			return null;
	}

	public synchronized Object getState(String key)
	{
		if(state==null)
			return null;
		return state.get(key);
	}

	public synchronized void setState(String key,Object value)
	{
		if(state==null)
			state=new HashMap<>();
		state.put(key,value);
	}
}