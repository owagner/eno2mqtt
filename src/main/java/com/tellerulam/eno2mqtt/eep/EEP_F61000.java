package com.tellerulam.eno2mqtt.eep;

import com.tellerulam.eno2mqtt.DeviceManager.Device;
import com.tellerulam.eno2mqtt.esp3.*;

public class EEP_F61000 extends EEP
{
	@Override
	public void handleMessage(Device d, ESP3ERP1Packet p)
	{
		assertPacketType(p,ESP3ERP1_1BSPacket.class);

		int v=p.getByteValue()>>4;

		// Window handle: val=0 down, val=1 left/right, val=2 up

		if(v==15)
			v=0;
		else if(v==13)
			v=2;
		else if((v&13)==12)
			v=1;

		publish(d,v);
	}

}
