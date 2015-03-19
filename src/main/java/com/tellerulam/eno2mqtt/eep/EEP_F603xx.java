package com.tellerulam.eno2mqtt.eep;

public class EEP_F603xx extends EEP_F602xx
{
	public EEP_F603xx(int appStyle)
	{
		super(appStyle);
	}

	@Override
	public String getProfileName()
	{
		return super.getProfileName().replace("F6-02","F6-03");
	}


}
