eno2mqtt
========

  Written and (C) 2015 Oliver Wagner <owagner@tellerulam.com> 
  
  Provided under the terms of the MIT license.


Overview
--------
eno2mqtt is a gateway between a Enocean USB300 interface and MQTT. It receives Enocean ERP1 telegrams and 
publishes them as MQTT topics.

It's intended as a building block in heterogenous smart home environments where an MQTT message broker is 
used as the centralized message bus. See https://github.com/mqtt-smarthome for a rationale and architectural 
overview.


Dependencies
------------
* Java 1.7 SE Runtime Environment: https://www.java.com/
* Eclipse Paho: https://www.eclipse.org/paho/clients/java/ (used for MQTT communication)
* Minimal-JSON: https://github.com/ralfstx/minimal-json (used for JSON creation and parsing)
* NRJavaSerial: https://github.com/NeuronRobotics/nrjavaserial (used for serial communication)

[![Build Status](https://travis-ci.org/owagner/eno2mqtt.svg)](https://travis-ci.org/owagner/eno2mqtt) Automatically built jars can be downloaded from the release page on GitHub at https://github.com/owagner/eno2mqtt/releases


Connection
----------
The connection to the USB300 device can either be made locally (although it's an USB stick, it's effectivly
just a serial device) or remotely via network via some remote serial service. The latter is useful if you want to
put the USB300 stick in the most efficient position RF-wise, but have the actual processing running elsewhere. 
Some very low grade hardware (e.g. a TP-LINK WR703N with OpenWRT) can then be used as a remserial server for the
stick.


DeviceList file
---------------
In order to know what type an Encoean device is and how to interpret it's messages, eno2mqtt needs a device list
file. It's a simple text file with three columns:

	Enocean_ID	EEP		Symbolic_Name

The *Enocean_ID* is the ID of the given device in hex notation, without a 0x prefix.

The *EEP* is the Enocean Equipment Profile ID, in the form RORG-FUNC-TYPE. A list of all EEPs is available on the Enocean web site
at https://www.enocean.com/en/knowledge-base/

The *Symbolic_Name* is used as the topic prefix when publishing status reports from the given device.


Topics
------
A special topic is *prefix/connected*. It holds an enum value which denotes whether the adapter is
currently running (1) and connected to the USB300 adapter (2). It's set to 0 on disconnect using a MQTT will.


MQTT Message format
--------------------
The message format generated is a JSON encoded object with the following members:

* val - the actual value, in numeric format
* eno_srcid - when sending message, eno2mqtt fills in the source ID of the sending device. 
  This field is ignored on incoming messages.


EEP specifics
-------------

*** F6-10-00 ***

Window handle: val=0 down, val=1 left/right, val=2 up


Usage
-----
Configuration options can either be specified on the command line, or as system properties with the prefix "eno2mqtt".
Examples:

    java -jar eno2mqtt.jar eno.usb300=/dev/ttyUSB03 eno.deviceList=devicelist.txt
    
    java -Dknxmqtt.knx.ip=127.0.0.1 -jar knx2mqtt.jar
    
### Available options:    

- eno.usb300
  
  Either a com port specification, or a network address in the form
  
  NET:host:port
  
  Must be specified.
  
- eno.deviceList

  The device list file. Must be specified.
  
- mqtt.server

  ServerURI of the MQTT broker to connect to. Defaults to "tcp://localhost:1883".
  
- mqtt.clientid

  ClientID to use in the MQTT connection. Defaults to "eno2mqtt".
  
- mqtt.topic

  The topic prefix used for publishing and subscribing. Defaults to "eno/".

When running eno2mqtt on a server class machine, it makes sense to limit the memory usage
to 64MB using the java options

    -Xmx64M
    

See also
--------
- Project overview: https://github.com/mqtt-smarthome
  
  
Changelog
---------
* 0.1 - 2015/03/11 - owagner
  - initial version
      

 
