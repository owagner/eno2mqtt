package com.tellerulam.eno2mqtt.eep;

import com.tellerulam.eno2mqtt.*;
import com.tellerulam.eno2mqtt.esp3.*;

public class EEP_F602xx extends EEP
{
	private final int appStyle;

	public EEP_F602xx(int appStyle)
	{
		this.appStyle=appStyle;
	}

	private static final String buttons[]={"AI","AO","BI","BO","CI","CO","DI","DO"};

	@SuppressWarnings("boxing")
	@Override
	public void handleMessage(Device d, ESP3ERP1Packet p,ExtendedInfo ei)
	{
		assertPacketType(p,ESP3ERP1_RPSPacket.class);

		int v=p.getByteValue();

		int r=v>>5;
		boolean pressed=(v&(1<<4))!=0;
		int s=(v>>1)&7;
		boolean second=(v&1)!=0;

		if(pressed)
		{
			String key;
			if(second)
				key=buttons[r]+buttons[s];
			else
				key=buttons[r];

			publish(d,key,1,false,ei.dBm);
			d.setState("PRESSED"+key,Boolean.TRUE);
		}
		else
		{
			// Reset all remembered buttons
			for(String key:d.getStates().keySet())
			{
				if(key.startsWith("PRESSED"))
				{
					if(d.getState(key)==Boolean.TRUE)
					{
						publish(d,key.substring(7),0,false,ei.dBm);
						d.setState(key,Boolean.FALSE);
					}
				}
			}
		}
	}

}
