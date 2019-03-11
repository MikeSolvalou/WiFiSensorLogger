#include <ESP8266WiFi.h>
#include <WiFiUdp.h>
#include "WiFiCredentials.h"  //git is set to ignore this file

//derived from https://arduino-esp8266.readthedocs.io/en/latest/esp8266wifi/readme.html
//and https://tttapa.github.io/ESP8266/Chap07%20-%20Wi-Fi%20Connections.html

WiFiClient client;  //represents TCP connection from this device to server
WiFiUDP udp;  //use to send/receive UDP datagrams

bool isTimeSet=false; //does the ESP-01 have a good estimate of what the unix time is?
unsigned long timeLastTimeReq;  //time that last datagram requesting the unix time was sent; as time in ms from power on
unsigned long timeLastTimeSync; //time that last valid datagram containing the unix time was received; as time in ms from power on

unsigned long unixTimeLastSync; //unix time contained in last valid datagram

//send a byte with every UDP datagram requesting the current unix time, in order to match sent datagrams to received ones
byte lastTimeSyncVerif=0; //last number used as verification


//DEBUG
unsigned long timeLastMessageSent=0;


void setup() {
  //connect to home network //TODO: handle WiFi connection loss later? doesn't seem to actually happen...
  WiFi.begin(NETWORK_NAME, NETWORK_PASSWORD);

  while(WiFi.status() != WL_CONNECTED){
    delay(1000);
  }

  //start listening for UDP datagrams on port 8007
  udp.begin(8007);

  //send UDP datagram requesting the unix time, to a server on the LAN
  IPAddress timeServer(192,168,1,5);
  udp.beginPacket(timeServer, 8006);
  udp.write(++lastTimeSyncVerif);
  udp.endPacket();
  timeLastTimeReq=millis();
}

void loop() {
  unsigned long currentTime=millis(); //current time; as time in ms from power on
  
  //if there is a new datagram containing the current unix time, and it has been less than 300ms since the last request
  if(udp.parsePacket()!=0 && currentTime-timeLastTimeReq<300){
    byte buf[5];
    udp.read(buf, 5);

    if(buf[0]==lastTimeSyncVerif){  //if the verification byte matches
      timeLastTimeSync=currentTime;
      unixTimeLastSync = buf[1] | (buf[2]<<8) | (buf[3]<<16) | (buf[4]<<24); //this actually doesn't need casts to unsigned long
      //TODO?: could have a 'unixTimeLastSyncMs' variable (16b) to store the 'milliseconds of second' segment of unix time
      isTimeSet=true;
    }
  }

  //unix time at start of this loop
  unsigned long currentUnixTime = unixTimeLastSync+(currentTime-timeLastTimeSync)/1000;

  //if it's been over 30 mins since the last time sync, and over 1 minute since the last time sync request
  //TODO: send another datagram requesting the unix time

  //do not proceed without a good estimate of the current unix time
  if(!isTimeSet){
    return;
  }

  //every 60s, start temperature conversion; record current unix time, to be used as timestamp for this measurement
  //TODO


  //DEBUG: every 5s, send the ESP-01's estimate of unix time in a udp datagram back to the server
  if(currentTime - timeLastMessageSent > 5000){
    IPAddress timeServer(192,168,1,5);
    unsigned long esp01UnixTime = unixTimeLastSync+(millis()-timeLastTimeSync)/1000;
    byte buf[4] = {esp01UnixTime, esp01UnixTime>>8, esp01UnixTime>>16, esp01UnixTime>>24};
    udp.beginPacket(timeServer, 8008);
    udp.write(buf, 4);
    udp.endPacket();
    timeLastMessageSent=currentTime;
  }

}
