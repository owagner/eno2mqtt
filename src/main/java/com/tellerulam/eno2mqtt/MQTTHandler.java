package com.tellerulam.eno2mqtt;

import java.nio.charset.*;
import java.util.*;
import java.util.logging.*;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.*;

import com.eclipsesource.json.*;

public class MQTTHandler
{
	private final Logger L=Logger.getLogger(getClass().getName());

	public static void init() throws MqttException
	{
		instance=new MQTTHandler();
		instance.doInit();
	}

	private final String topicPrefix;
	private MQTTHandler()
	{
		String tp=System.getProperty("eno2mqtt.mqtt.topic","eno");
		if(!tp.endsWith("/"))
			tp+="/";
		topicPrefix=tp;
	}

	private static MQTTHandler instance;

	private MqttClient mqttc;

	private void queueConnect()
	{
		shouldBeConnected=false;
		Main.t.schedule(new TimerTask(){
			@Override
			public void run()
			{
				doConnect();
			}
		},10*1000);
	}

	private class StateChecker extends TimerTask
	{
		@Override
		public void run()
		{
			if(!mqttc.isConnected() && shouldBeConnected)
			{
				L.warning("Should be connected but aren't, reconnecting");
				queueConnect();
			}
		}
	}

	private boolean shouldBeConnected;

	private void processSetGet(String namePart,MqttMessage msg,boolean set)
	{
		if(msg.isRetained())
		{
			L.finer("Ignoring retained message "+msg+" to "+namePart);
			return;
		}
	}

	void processMessage(String topic,MqttMessage msg)
	{
		L.fine("Received "+msg+" to "+topic);
		topic=topic.substring(topicPrefix.length(),topic.length());
		if(topic.startsWith("set/"))
			processSetGet(topic.substring(4),msg,true);
		else if(topic.startsWith("get/"))
			processSetGet(topic.substring(4),msg,false);
	}


	private void doConnect()
	{
		L.info("Connecting to MQTT broker "+mqttc.getServerURI()+" with CLIENTID="+mqttc.getClientId()+" and TOPIC PREFIX="+topicPrefix);

		MqttConnectOptions copts=new MqttConnectOptions();
		copts.setWill(topicPrefix+"connected", "0".getBytes(), 1, true);
		copts.setCleanSession(true);
		try
		{
			mqttc.connect(copts);
			setEnoceanConnectionState(false);
			L.info("Successfully connected to broker, subscribing to "+topicPrefix+"set/#");
			try
			{
				mqttc.subscribe(topicPrefix+"set/#",1);
				shouldBeConnected=true;
			}
			catch(MqttException mqe)
			{
				L.log(Level.WARNING,"Error subscribing to topic hierarchy, check your configuration",mqe);
				throw mqe;
			}
		}
		catch(MqttException mqe)
		{
			L.log(Level.WARNING,"Error while connecting to MQTT broker, will retry: "+mqe.getMessage(),mqe);
			queueConnect(); // Attempt reconnect
		}
	}

	private void doInit() throws MqttException
	{
		String server=System.getProperty("eno2mqtt.mqtt.server","tcp://localhost:1883");
		String clientID=System.getProperty("eno2mqtt.mqtt.clientid","eno2mqtt");
		mqttc=new MqttClient(server,clientID,new MemoryPersistence());
		mqttc.setCallback(new MqttCallback() {
			@Override
			public void messageArrived(String topic, MqttMessage msg) throws Exception
			{
				try
				{
					processMessage(topic,msg);
				}
				catch(Exception e)
				{
					L.log(Level.WARNING,"Error when processing message "+msg+" for "+topic,e);
				}
			}
			@Override
			public void deliveryComplete(IMqttDeliveryToken token)
			{
				/* Intentionally ignored */
			}
			@Override
			public void connectionLost(Throwable t)
			{
				L.log(Level.WARNING,"Connection to MQTT broker lost",t);
				queueConnect();
			}
		});
		doConnect();
		Main.t.schedule(new StateChecker(),30*1000,30*1000);
	}

	private void doPublish(String name, Object val, String src,boolean retain,int dbm)
	{
		JsonObject jso=new JsonObject();
		if(val instanceof Integer)
			jso.add("val",((Integer)val).intValue());
		else if(val instanceof Float)
			jso.add("val",((Float)val).floatValue());
		else
			jso.add("val",val.toString());
		jso.add("eno_srcid",src);
		jso.add("eno_dbm",-dbm);
		String txtmsg=jso.toString();
		MqttMessage msg=new MqttMessage(jso.toString().getBytes(StandardCharsets.UTF_8));
		msg.setQos(0);
		msg.setRetained(retain);
		try
		{
			String fullTopic=topicPrefix+"status/"+name;
			mqttc.publish(fullTopic, msg);
			L.info("Published "+txtmsg+" to "+fullTopic);
		}
		catch(MqttException e)
		{
			L.log(Level.WARNING,"Error when publishing message "+txtmsg,e);
		}
	}

	public static void setEnoceanConnectionState(boolean connected)
	{
		try
		{
			instance.mqttc.publish(instance.topicPrefix+"connected",(connected?"2":"1").getBytes(),1,true);
		}
		catch(MqttException e)
		{
			/* Ignore */
		}
	}

	public static void publish(String name, Object val, String src,boolean retain,int dbm)
	{
		instance.doPublish(name,val,src,retain,dbm);
	}
	public static void publish(String name, Object val, String src,int dbm)
	{
		instance.doPublish(name,val,src,true,dbm);
	}

}
