package com.tellerulam.eno2mqtt;

import gnu.io.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import com.tellerulam.eno2mqtt.esp3.*;

public abstract class ESP3Connector extends Thread
{
	static private List<ESP3Connector> connectors=new ArrayList<>();
	static private final ScheduledExecutorService pierrepoint=Executors.newSingleThreadScheduledExecutor();

	private static synchronized void updateConnectionState()
	{
		for(ESP3Connector conn:connectors)
		{
			if(conn.isConnected())
			{
				MQTTHandler.setEnoceanConnectionState(true);
				return;
			}
		}
		MQTTHandler.setEnoceanConnectionState(false);
	}

	private static class ConnectionChecker implements Runnable
	{
		@Override
		public void run()
		{
			for(ESP3Connector conn:connectors)
			{
				if(conn.isConnected())
					return;
			}
			L.severe("No more active connections, terminating");
			System.exit(1);
		}
	}
	private static final ConnectionChecker connectionChecker=new ConnectionChecker();

	protected InputStream is;
	protected OutputStream os;

	private CountDownLatch responseLatch;
	private byte[] responseBuffer;

	protected final String logPrefix;

	public synchronized boolean transact(ESP3Packet p) throws IOException, InterruptedException
	{
		responseLatch=new CountDownLatch(1);
		p.send(os);
		if(responseLatch.await(500,TimeUnit.MILLISECONDS))
		{
			p.parseResponse(responseBuffer);
			return p.isResponseOK();
		}
		return false;
	}

	private class DutyCycleReader implements Runnable
	{
		@Override
		public void run()
		{
			ESP3ReadDutycyclePacket rdc=new ESP3ReadDutycyclePacket();
			try
			{
				if(transact(rdc))
				{
					L.info(logPrefix+"Duty cycle available "+rdc.availableCycle+"%, slots "+rdc.slots+" period "+rdc.slotPeriod+"s, time left in slot "+rdc.slotLeft+"s, load after actual "+rdc.loadAfterActual+"%");
				}
			}
			catch(IOException | InterruptedException e)
			{
				L.log(Level.WARNING,logPrefix+"Error while reading duty cycle",e);
			}
		}
	}
	private DutyCycleReader dutyCycleReader;

	private void sendInitPackets() throws IOException, InterruptedException
	{
		L.info(logPrefix+"Resetting device...");

		ESP3ResetPacket reset=new ESP3ResetPacket();
		transact(reset);

		Thread.sleep(500);

		ESP3ReadVersionPacket rvp=new ESP3ReadVersionPacket();
		if(!transact(rvp))
			throw new IllegalStateException("ReadVersion did not return OK");
		L.info(logPrefix+"APP version "+rvp.appVersion+" API version "+rvp.apiVersion+" Chip ID "+rvp.chipID+" Chip Version "+rvp.chipVersion+" APP description "+rvp.appDescription);

		ESP3ReadBaseIDPacket rbi=new ESP3ReadBaseIDPacket();
		if(!transact(rbi))
			throw new IllegalStateException("ReadBaseID did not return OK");
		L.info(logPrefix+"BaseID is "+rbi.baseID);

		String setRepeater=System.getProperty("eno2mqtt.eno.setRepeater");
		if(setRepeater!=null)
		{
			String srp[]=setRepeater.split(",");
			int level=1;
			if(srp.length==2 && "LEVEL2".equals(srp[1]))
				level=2;
			int enabled=0;
			if("OFF".equals(srp[0]))
				level=0;
			else if("ALL".equals(srp[0]))
				enabled=1;
			else if("FILTERED".equals(srp[0]))
				enabled=2;
			else
				L.warning(logPrefix+"Illegal setRepeater mode "+setRepeater);
			ESP3WriteRepeaterPacket wrp=new ESP3WriteRepeaterPacket(enabled, level);
			if(!transact(wrp))
				L.warning(logPrefix+"Changing repeater mode FAILED");
		}

		ESP3ReadRepeaterPacket rrp=new ESP3ReadRepeaterPacket();
		if(transact(rrp))
			L.info(logPrefix+"Repeater ENABLED="+rrp.getRepeaterEnable()+" LEVEL="+rrp.getRepeaterLevel());

		if(dutyCycleReader==null)
		{
			dutyCycleReader=new DutyCycleReader();
			pierrepoint.scheduleWithFixedDelay(dutyCycleReader,5,60,TimeUnit.SECONDS);
		}
	}

	private void parseResponse(byte b[])
	{
		responseBuffer=b;
		responseLatch.countDown();
	}

	@SuppressWarnings("boxing")
	private void dispatchERP1Packet(ESP3ERP1Packet p,ExtendedInfo ei)
	{
		L.info(logPrefix+"Packet "+p+" "+ei);
		Device d=DeviceManager.getDeviceByID(p.senderID);
		if(d==null)
		{
			L.warning(logPrefix+"ERP1 packet from unknown device "+Long.toHexString(p.senderID)+" ignored, please update your device list file!");
			return;
		}
		d.eep.handleMessage(d,p,ei);
	}

	private void parseERP1(byte b[],byte op[])
	{
		ESP3ERP1Packet p;

		ExtendedInfo ei=new ExtendedInfo();
		if(op!=null)
		{
			if(op.length>0)
				ei.subTelNum=op[0];
			if(op.length>4)
			{
				for(int n=1;n<5;n++)
					ei.destinationID=ei.destinationID<<8|(op[n]&0xff);
			}
			if(op.length>5)
				ei.dBm=op[5];
		}

		switch(b[0]&0xff)
		{
			case 0xf6:
				p=new ESP3ERP1_RPSPacket();
				p.parseResponse(b);
				dispatchERP1Packet(p,ei);
				break;

			case 0xd5:
				p=new ESP3ERP1_1BSPacket();
				p.parseResponse(b);
				dispatchERP1Packet(p,ei);
				break;

			default:
				L.warning(logPrefix+"Ignoring ERP1 packet of RORG "+b[0]+" "+ei);
		}
	}

	private void readBytes(byte []to,int n) throws IOException
	{
		int ix;
		for(ix=0;n>0;ix++,n--)
		{
			int ch=is.read();
			if(ch<0)
				throw new EOFException();
			to[ix]=(byte)ch;
		}
	}

	@Override
	public void run()
	{
		for(;;)
		{
			try
			{
				connect();
				doIt();
			}
			catch(Exception e)
			{
				L.log(Level.WARNING,logPrefix+"Error in ESP3 communication handler, retrying in 10s",e);
			}
			connected=false;
			updateConnectionState();
			pierrepoint.schedule(connectionChecker,15,TimeUnit.SECONDS);
			try
			{
				disconnect();
			}
			catch(Exception e)
			{
				// Ignore
			}
			try
			{
				sleep(10*1000);
			}
			catch(InterruptedException e)
			{
				// Ignore
			}
		}
	}

	private boolean connected;

	private boolean isConnected()
	{
		return connected;
	}

	private void doIt() throws Exception
	{
		pierrepoint.execute(new Runnable(){
			@Override
			public void run()
			{
				try
				{
					sendInitPackets();
					connected=true;
					updateConnectionState();
				}
				catch(IOException | InterruptedException e)
				{
					// Ignore, main loop will terminate too
				}
			}
		});

		byte header[]=new byte[4];
		for(;;)
		{
			int b=is.read();
			if(b<0)
				throw new EOFException();
			if(b!=0x55)
			{
				L.warning(logPrefix+"Sync error, got "+Integer.toHexString(b)+" instead of 0x55!");
				continue;
			}
			readBytes(header,4);
			int dataLength=((header[0]&0xff)<<8)|(header[1]&0xff);
			int opDataLength=header[2]&0xff;
			byte packetType=header[3];
			byte crc=CRC8.calcCRC(header,0,4);
			byte rcrc=(byte)is.read();
			if(crc!=rcrc)
			{
				L.warning(logPrefix+"CRC error in packet header; expected "+rcrc+", have "+crc);
				continue;
			}
			byte data[]=new byte[dataLength];
			byte opData[]=new byte[opDataLength];
			readBytes(data,dataLength);
			readBytes(opData,opDataLength);
			byte dcrc=(byte)is.read();
			// Note: start value here is the header-crc!
			crc=CRC8.updateCRC(rcrc,crc);
			crc=CRC8.updateCRC(data,crc);
			crc=CRC8.updateCRC(opData,crc);
			if(crc!=dcrc)
			{
				L.warning(logPrefix+"CRC error in data; expected "+dcrc+", have "+crc);
				continue;
			}

			switch(packetType)
			{
				case 1:
					parseERP1(data,opData);
					break;

				case 2:
					parseResponse(data);
					break;

				default:
					L.info(logPrefix+"Ignoring packet of type "+packetType+" with dlen="+dataLength+" oplen="+opDataLength);
			}
		}
	}

	protected ESP3Connector(int instanceNr)
	{
		logPrefix="ESP("+instanceNr+"): ";
	}

	private static class SerialConnector extends ESP3Connector
	{
		private final String port;

		private NRSerialPort serial;

		@Override
		protected void connect()
		{
			L.info(logPrefix+"Using serial port "+port);
			serial=new NRSerialPort(port,57600);
			serial.connect();
            serial.getSerialPortInstance().disableReceiveTimeout();
            serial.getSerialPortInstance().enableReceiveThreshold(1);
			is=serial.getInputStream();
			os=serial.getOutputStream();
		}

		@Override
		protected void disconnect()
		{
			serial.disconnect();
		}

		SerialConnector(String port,int instanceNr)
		{
			super(instanceNr);
			this.port=port;
		}
	}

	protected abstract void connect() throws Exception;
	protected abstract void disconnect() throws Exception;

	private static class NetworkConnector extends ESP3Connector
	{
		private final String hostspec;

		private Socket s;

		@Override
		protected void connect() throws NumberFormatException, UnknownHostException, IOException
		{
			L.info(logPrefix+"Using network connection to "+hostspec);
			String spec[]=hostspec.split(":",2);
			s=new Socket(spec[0],spec.length==2?Integer.parseInt(spec[1]):23000);
			is=s.getInputStream();
			os=s.getOutputStream();
		}

		@Override
		protected void disconnect() throws IOException
		{
			s.close();
		}

		NetworkConnector(String hostspec,int instanceNr)
		{
			super(instanceNr);
			this.hostspec=hostspec;
		}
	}

	static void setupConnections()
	{
		String connectionspecs=System.getProperty("eno2mqtt.eno.esp3");
		if(connectionspecs==null)
			throw new IllegalArgumentException("You must specify a serial or network connection to an ESP3 device");
		String specs[]=connectionspecs.split(",");
		int instanceNr=0;
		for(String spec:specs)
		{
			ESP3Connector conn;
			if(spec.startsWith("NET:"))
				conn=new NetworkConnector(spec.substring(4),++instanceNr);
			else
				conn=new SerialConnector(spec,++instanceNr);
			connectors.add(conn);
			conn.start();
		}
		// Check in 15 seconds whether we're connected
		pierrepoint.schedule(connectionChecker,15,TimeUnit.SECONDS);
	}

	private static final Logger L=Logger.getLogger(ESP3Connector.class.getName());

}
