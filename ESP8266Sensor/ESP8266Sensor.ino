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
unsigned int unixTimeLastSyncMs; //'milliseconds of second' part of unix time from last valid datagram

//send a byte with every UDP datagram requesting the current unix time, in order to match sent datagrams to received ones
byte lastTimeSyncVerif=0; //last number used as verification


//FOR TIME SYNC TESTING
unsigned long timeLastMessageSent=0;

unsigned long timeLastLogSent=0;


void setup() {
  //connect to home network //TODO: handle WiFi connection loss? doesn't seem to actually happen...
  WiFi.begin(NETWORK_NAME, NETWORK_PASSWORD);
  while(WiFi.status() != WL_CONNECTED){
    delay(1000);
  }

  //start listening for UDP datagrams on port 8007; outgoing datagrams also use this port number
  udp.begin(8007);

  //send a UDP datagram requesting the unix time, to a server on the LAN
  requestUnixTime();
}

void loop() {
  unsigned long currentTime=millis(); //current time; as time in ms from power on
  
  //if there is a new datagram containing the current unix time, and it has been less than 100ms since the last request
  if(udp.parsePacket()!=0 && currentTime-timeLastTimeReq<100){
    byte buf[7];
    udp.read(buf, 7);

    if(buf[0]==lastTimeSyncVerif){  //if the verification byte matches
      //don't call millis() again to get the current time
      // if anything, the delay between now and the last call of millis() cancels some of the latency to the time server
      timeLastTimeSync=currentTime;

      //these actually don't need casts to unsigned long
      unixTimeLastSync = buf[1] | (buf[2]<<8) | (buf[3]<<16) | (buf[4]<<24);
      unixTimeLastSyncMs = buf[5] | buf[6]<<8;
      
      isTimeSet=true;
    }
  }

  //unix time at start of this loop
  unsigned long currentUnixTime = unixTimeLastSync+(unixTimeLastSyncMs+currentTime-timeLastTimeSync)/1000;
  unsigned short currentUnixTimeMs = (unixTimeLastSyncMs+currentTime-timeLastTimeSync)%1000;

  //if (over 30 minutes since the last time sync, OR, time isn't set),
  // AND over 4 seconds since the last time sync request
  if((currentTime-timeLastTimeSync > 1800000UL || !isTimeSet) && currentTime-timeLastTimeReq > 4000){
    //send another datagram requesting the unix time
    requestUnixTime();
  }

  //do not proceed without a good estimate of the current unix time
  if(!isTimeSet){
    return;
  }

  //TIME SYNC TESTING: every 5s, send the ESP-01's estimate of unix time in a UDP datagram to a monitoring program
  if(currentTime - timeLastMessageSent > 5000){
    IPAddress timeServer(192,168,1,5);
    unsigned long millisNow = millis();
    unsigned long esp01UnixTime = unixTimeLastSync+(unixTimeLastSyncMs+millisNow-timeLastTimeSync)/1000;
    unsigned short esp01UnixTimeMs = (unixTimeLastSyncMs+millisNow-timeLastTimeSync)%1000;
    byte buf[6] = {esp01UnixTime, esp01UnixTime>>8, esp01UnixTime>>16, esp01UnixTime>>24, esp01UnixTimeMs, esp01UnixTimeMs>>8};
    udp.beginPacket(timeServer, 8008);
    udp.write(buf, 6);
    udp.endPacket();

    if(currentTime - timeLastMessageSent > 6000)
      timeLastMessageSent=currentTime;
    else
      //adding 5000 instead of assigning currentTime makes the 5s period between datagrams more accurate
      timeLastMessageSent+=5000;
  }


  //every 60s, start temperature conversion; record current unix time, to be used as timestamp for this measurement
  //TODO


  if(currentTime-timeLastLogSent>6000){  //uses a random local port
    printlnTCP("hello");
    timeLastLogSent=currentTime;
  }

}



/**Send a UDP datagram to the time server, requesting the current unix time be sent back.*/
void requestUnixTime(){
  IPAddress timeServer(192,168,1,5);
  udp.beginPacket(timeServer, 8006);
  udp.write(++lastTimeSyncVerif);
  udp.endPacket();
  timeLastTimeReq=millis();
}


/**Send a character string to the server via a temporary TCP connection, to be printed to the server's standard out.*/
void printlnTCP(String content){
  if(client.connect("192.168.1.5", 8009)){
    client.println(content);
    client.flush();
    client.stop();
  }
}
