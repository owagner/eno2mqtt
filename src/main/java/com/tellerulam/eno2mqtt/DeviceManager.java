package com.tellerulam.eno2mqtt;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;

import com.tellerulam.eno2mqtt.eep.*;

public class DeviceManager
{
	private static Map<String,Device> devicesByName=new HashMap<>();
	private static Map<Long,Device> devicesByID=new HashMap<>();

	public static Device getDeviceByName(String name)
	{
		return devicesByName.get(name);
	}

	public static Device getDeviceByID(Long id)
	{
		return devicesByID.get(id);
	}

	public static void readDeviceList()
	{
		String deviceListFile=System.getProperty("eno2mqtt.eno.deviceList");
		if(deviceListFile==null)
		{
			L.severe("No device list file specified! No name resolution will take place, and parsing of messages will be broken!");
			return;
		}
		int line=0;
		try(BufferedReader br=new BufferedReader(new FileReader(deviceListFile)))
		{
			for(;;)
			{
				line++;
				String l=br.readLine();
				if(l==null)
					break;
				int coff=l.indexOf('#');
				if(coff>=0)
					l=l.substring(0,coff);
				l=l.trim();
				if(l.length()==0)
					continue;

				String lp[]=l.split("\\s+",3);
				if(lp.length!=3)
					throw new ParseException("Invalid format "+l,0);

				EEP eep=EEP.findBySpec(lp[1]);
				if(eep==null)
					throw new IllegalArgumentException("Unsupported EEP "+lp[1]);

				@SuppressWarnings("boxing")
				Device d=new Device(Long.parseLong(lp[0],16),eep,new String(lp[2])); // So we do not retain a copy of the full line in memory
				devicesByID.put(d.id,d);
				devicesByName.put(d.name,d);
			}
			L.info("Read "+devicesByID.size()+" devices: "+devicesByID.values());
		}
		catch(Exception e)
		{
			L.log(Level.WARNING,"Error while reading device list file "+deviceListFile+" in line "+line,e);
		}
	}

	private static final Logger L=Logger.getLogger(DeviceManager.class.getName());
}
