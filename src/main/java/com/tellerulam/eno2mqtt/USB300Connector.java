package com.tellerulam.eno2mqtt;

import gnu.io.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import com.tellerulam.eno2mqtt.esp3.*;

public abstract class USB300Connector extends Thread
{
	static private List<USB300Connector> connectors=new ArrayList<>();

	protected InputStream is;
	protected OutputStream os;

	private CountDownLatch responseLatch;
	private byte[] responseBuffer;

	public boolean transact(ESP3Packet p) throws IOException, InterruptedException
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

	private void sendInitPackets() throws IOException, InterruptedException
	{
		ESP3ReadVersionPacket rvp=new ESP3ReadVersionPacket();
		if(!transact(rvp))
			throw new IllegalStateException("ReadVersion did not return OK");
		L.info("USB300: APP version "+rvp.appVersion+" API version "+rvp.apiVersion+" Chip ID "+rvp.chipID+" Chip Version "+rvp.chipVersion+" APP description "+rvp.appDescription);

		ESP3ReadBaseIDPacket rbi=new ESP3ReadBaseIDPacket();
		if(!transact(rbi))
			throw new IllegalStateException("ReadBaseID did not return OK");
		L.info("USB300: BaseID is "+rbi.baseID);

		MQTTHandler.setEnoceanConnectionState(true);
	}

	private void parseResponse(byte b[])
	{
		responseBuffer=b;
		responseLatch.countDown();
	}

	@SuppressWarnings("boxing")
	private void dispatchERP1Packet(ESP3ERP1Packet p)
	{
		L.info("Packet from "+Long.toHexString(p.senderID));
		DeviceManager.Device d=DeviceManager.getDeviceByID(p.senderID);
		if(d==null)
		{
			L.warning("ERP1 packet from unknown device "+Long.toHexString(p.senderID)+" ignored, please update your device list file!");
			return;
		}
		d.eep.handleMessage(d,p);
	}

	private void parseERP1(byte b[])
	{
		ESP3ERP1Packet p;

		switch(b[0]&0xff)
		{
			case 0xf6:
			case 0xd5:
				p=new ESP3ERP1_1BSPacket();
				p.parseResponse(b);
				dispatchERP1Packet(p);
				break;

			default:
				L.warning("Ignoring ERP1 packet of RORG "+b[0]);
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
		try
		{
			byte header[]=new byte[4];
			for(;;)
			{
				int b=is.read();
				if(b<0)
					throw new EOFException();
				if(b!=0x55)
					continue;
				readBytes(header,4);
				int dataLength=((header[0]&0xff)<<8)|(header[1]&0xff);
				int opDataLength=header[2]&0xff;
				byte packetType=header[3];
				byte crc=CRC8.calcCRC(header,0,4);
				byte rcrc=(byte)is.read();
				if(crc!=rcrc)
				{
					L.warning("CRC error in packet header; expected "+rcrc+", have "+crc);
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
					L.warning("CRC error in data; expected "+dcrc+", have "+crc);
					continue;
				}

				switch(packetType)
				{
					case 1:
						parseERP1(data);
						break;

					case 2:
						parseResponse(data);
						break;
					default:
						L.info("Ignoring packet of type "+packetType+" with dlen="+dataLength+" oplen="+opDataLength);
				}
			}
		}
		catch(Exception e)
		{
			L.log(Level.WARNING, "Error in USB300 communication handler", e);
		}
	}

	private static class SerialConnector extends USB300Connector
	{
		SerialConnector(String port)
		{
			NRSerialPort serial=new NRSerialPort(port,57600);
			serial.connect();
			is=serial.getInputStream();
			os=serial.getOutputStream();
		}
	}

	private static class NetworkConnector extends USB300Connector
	{
		NetworkConnector(String hostspec) throws NumberFormatException, UnknownHostException, IOException
		{
			String spec[]=hostspec.split(":",2);
			@SuppressWarnings("resource")
			Socket s=new Socket(spec[0],spec.length==2?Integer.parseInt(spec[1]):23000);
			is=s.getInputStream();
			os=s.getOutputStream();
		}
	}

	static void setupConnections() throws NumberFormatException, UnknownHostException, IOException, InterruptedException
	{
		String connectionspecs=System.getProperty("eno2mqtt.eno.usb300");
		if(connectionspecs==null)
			throw new IllegalArgumentException("You must specify a serial or network connection to an USB300 device");
		String specs[]=connectionspecs.split(",");
		for(String spec:specs)
		{
			USB300Connector conn;
			if(spec.startsWith("NET:"))
				conn=new NetworkConnector(spec.substring(4));
			else
				conn=new SerialConnector(spec);
			connectors.add(conn);
			conn.start();
			conn.sendInitPackets();
		}
	}

	private final Logger L=Logger.getLogger(getClass().getName());

}
