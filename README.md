eno2mqtt
========

  Written and (C) 2015 Oliver Wagner <owagner@tellerulam.com> 
  
  Provided under the terms of the MIT license.


Overview
--------
eno2mqtt is a gateway between a Enocean ESP3-capable gateway module (e.g. a TCM310, USB300 or Enocean PI) and MQTT. 
It receives Enocean  ERP1 telegrams and publishes them as MQTT topics.

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
The connection to the ESP3 device can either be made locally through a serial port (the USB300 stick is just
a TCM310 with a FTDI USB<->Serial converter) or remotely via network via some remote serial service. The latter is useful if you 
want to put the USB300 stick in the most efficient position RF-wise, but have the actual processing running elsewhere. 
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
Depending on the EEP, this may be suffixed further.


Topics
------
A special topic is *prefix/connected*. It holds an enum value which denotes whether the adapter is
currently running (1) and connected to the ESP3 devices (2). It's set to 0 on disconnect using a MQTT will.


MQTT Message format
--------------------
The message format generated is a JSON encoded object with the following members:

* val - the actual value, in numeric format
* eno_srcid - when sending message, eno2mqtt fills in the source ID of the sending device. 
  This field is ignored on incoming messages.
* eno_dbm - the RSSI value of the received message


EEP specifics
-------------

* F6-02
* F6-03

Rocker switches. Publishes state to subtopics "AI", "AO" etc. as either 1 for pressed
or 0 for released. The published messages are not retained, as those are one-shot
events. Dual-button presses are reported on the respective dual-button topic, e.g. "AIBI".

* F6-10-00

Window handle: val=0 down, val=1 left/right, val=2 up


Usage
-----
Configuration options can either be specified on the command line, or as system properties with the prefix "eno2mqtt".
Examples:

    java -jar eno2mqtt.jar eno.esp3=/dev/ttyUSB0 eno.deviceList=devicelist.txt
    
    java -Deno.esp3=/dev/ttyUSB0 -jar eno2mqtt.jar
    
### Available options:    

- eno.esp3
  
  One or more ESP3 devices to connect to (TCM310, USB300, Enocean PI etc.)
  
  Either a com port specification, or a network address in the form
  
  NET:host:port
  
  for a remote serial server. Multiple devices can be specified in a comma-separated
  list. Must be specified.
  
- eno.deviceList

  The device list file. Must be specified.
  
- eno.setRepeater

  Set the repeater mode of the TCM. Format is "mode,level". Mode can be OFF, ALL or FILTERED,
  level can be LEVEL1 or LEVEL2
  
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
* 0.6 - 2015/07/28 - owagner
  - include timestamps ("ts") in generated JSON objects
  - updated minimal-json to 0.9.4 and eclipse-paho to 1.0.2

* 0.5 - 2015/03/19 - owagner
  - now properly supports multiple ESP3 connections. Will filter duplicate messages within 500ms.
  - changed state handling: will now retry failed connections every 10 seconds. Will keep running
    as long as there is at least one active connection within 15 seconds
  - made initial device list dump in log more readable
  - log sync errors
  - fixed setting repeater to OFF mode
  - relabeled all references to TCM310 to the even more accurate ESP3 designation
  
* 0.4 - 2015/03/15 - owagner
  - now sends RD_DUTYCYCLE every 60s and dumps a possible output, mostly as a serial-level keepalive

* 0.3 - 2015/03/14 - owagner
  - renamed property eno.usb300 to the more accurate eno.tcm
  - added eno.setRepeater to set the repeater mode of the TCM
  - more detailed log output about received frames (including dest ID, RSSI and status flags)
  - include RSSI value in generated messages
  
* 0.2 - 2015/03/12 - owagner
  - now supports F6-02 and F6-03 EEPs (rocker switches)

* 0.1 - 2015/03/11 - owagner
  - initial version
 
