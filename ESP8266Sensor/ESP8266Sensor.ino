//derived from https://arduino-esp8266.readthedocs.io/en/latest/esp8266wifi/readme.html
//and https://tttapa.github.io/ESP8266/Chap07%20-%20Wi-Fi%20Connections.html
//and http://www.cplusplus.com/doc/tutorial/preprocessor/


#define DS18B20 1
#define DHT11 2


//uncomment the type of sensor being used
#define SENSOR_TYPE DS18B20
//#define SENSOR_TYPE DHT11

//set the ID number of this sensor; reserve 0 for testing
// Used to link a measurement in the database to the hardware that made it.
// Allows cancellation of errors inherent to a particular combination of hardware.
// Mounting the WiFi module too close to a temperature sensor (as done by ESP-01 compatible boards)
// can add a few degrees C to the measurement, so mounting the WiFi module further away should count
// as a hardware change, and get a different sensor id.
#define SENSOR_ID 0


#if SENSOR_TYPE==DHT11
 #include <DHT.h>
#elif SENSOR_TYPE==DS18B20
 #include <DallasTemperature.h>
 #include <OneWire.h>
#endif

#include <ESP8266WiFi.h>
#include <WiFiUdp.h>
#include "WiFiCredentials.h"  //git is set to ignore this file, defines NETWORK_NAME and NETWORK_PASSWORD


WiFiClient client;  //represents TCP connection from this device to server
WiFiUDP udp;  //use to send/receive UDP datagrams


bool isTimeSet=false; //does the ESP-01 have a good estimate of what the unix time is?
unsigned long timeLastTimeReq;  //time that last datagram requesting the unix time was sent; as time in ms from power on
unsigned long timeLastTimeSync; //time that last valid datagram containing the unix time was received; as time in ms from power on

unsigned long unixTimeLastSync; //unix time contained in last valid datagram
unsigned int unixTimeLastSyncMs; //'milliseconds of second' part of unix time from last valid datagram

//send a byte with every UDP datagram requesting the current unix time, in order to match sent datagrams to received ones
byte lastTimeSyncVerif=0; //last number used as verification


//unsigned long timeLastMessageSent=0;  //FOR TIME SYNC TESTING


#if SENSOR_TYPE==DHT11
DHT dht(2, DHT11);  //DHT-11 sensor connected on pin GPIO2
unsigned long timeLastTempReading=0;  //time of last temperature reading, as ms since power on

#elif SENSOR_TYPE==DS18B20
OneWire oneWire(2); //represents onewire bus on GPIO2
DallasTemperature tempSensor(&oneWire); //represents temperature sensor on onewire bus
unsigned long timeLastConvStart=0;
bool conversionStarted=false;
bool firstConversionStarted=false;
#endif


void setup() {
  //connect to home network //TODO: handle WiFi connection loss? doesn't seem to actually happen...
  WiFi.mode(WIFI_STA);  //permit auto modem sleep?, from https://github.com/esp8266/Arduino/issues/1381
  WiFi.begin(NETWORK_NAME, NETWORK_PASSWORD);
  while(WiFi.status() != WL_CONNECTED){
    delay(1000);
  }
  printlnTCP("Connected.");

  //start listening for UDP datagrams on port 8007; outgoing datagrams also use this port number
  udp.begin(8007);

#if SENSOR_TYPE==DHT11
  dht.begin();

#elif SENSOR_TYPE==DS18B20
  tempSensor.setResolution(12);  //12 bits = 1/16 C resolution?
  tempSensor.setWaitForConversion(false);
  tempSensor.begin();
#endif

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
      //record the time that this datagram was received
      // don't call millis() again to get the current time
      // if anything, the delay between now and the last call of millis() cancels some of the latency to the time server
      timeLastTimeSync=currentTime;

      // these actually don't need casts to unsigned long
      unixTimeLastSync = buf[1] | (buf[2]<<8) | (buf[3]<<16) | (buf[4]<<24);
      unixTimeLastSyncMs = buf[5] | buf[6]<<8;
      
      isTimeSet=true;
    }
  }

  //if (over 60 minutes since the last time sync, OR, time isn't set),
  // AND over 4 seconds since the last time sync request
  if((currentTime-timeLastTimeSync > 3600000UL || !isTimeSet) && currentTime-timeLastTimeReq > 4000){
    //send another datagram requesting the unix time
    requestUnixTime();
  }

  //do not proceed without a good estimate of the current unix time
  if(!isTimeSet){
    return;
  }

  //unix time at start of this loop //don't need to do these calculations every loop
  //unsigned long currentUnixTime = unixTimeLastSync+(unixTimeLastSyncMs+currentTime-timeLastTimeSync)/1000;
  //unsigned short currentUnixTimeMs = (unixTimeLastSyncMs+currentTime-timeLastTimeSync)%1000;


  //TIME SYNC TESTING: every 5s, send the ESP-01's estimate of unix time in a UDP datagram to a monitoring program at 192.168.1.5:8008
  /*if(currentTime - timeLastMessageSent > 5000){
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
  }*/


#if SENSOR_TYPE==DHT11
  //every 10s, get temperature, then send to server
  if(currentTime-timeLastTempReading > 10000){
    timeLastTempReading = currentTime;

    //do this after since it blocks the main loop for about 250ms
    float temperature = dht.readTemperature();

    printlnTCP(String(temperature));
  }

#elif SENSOR_TYPE==DS18B20
  //every 10s, start temperature A->D conversion
  if(currentTime-timeLastConvStart > 10000 || !firstConversionStarted){
    timeLastConvStart = currentTime;
    
    tempSensor.requestTemperatures(); //asks all sensors on onewire bus to start A->D conversion
    conversionStarted=true;
    firstConversionStarted=true;
  }

  //900ms after A->D conversion started, read temperature and print over TCP
  if(conversionStarted==true && currentTime-timeLastConvStart > 900){
    conversionStarted=false;

    //compute unix timestamp for this temperature reading from timeLastConvStart
    unsigned long timestamp = unixTimeLastSync+(unixTimeLastSyncMs+timeLastConvStart-timeLastTimeSync)/1000;
    unsigned long timestampMs = (unixTimeLastSyncMs+timeLastConvStart-timeLastTimeSync)%1000;
    if(timestampMs > 499)
      timestamp++;

    //get temperature reading
    float temperature = tempSensor.getTempCByIndex(0);

    //send timestamp and reading to server
    if(client.connect("192.168.1.5", 8005)){
      client.write((byte)SENSOR_ID);  //1st byte = sensor id
      client.write(timestamp);  //next 4 bytes = timestamp, LSB first, whole seconds only
      client.write(timestamp>>8);
      client.write(timestamp>>16);
      client.write(timestamp>>24);
      client.write('T');  //next byte = letter T
      //from https://stackoverflow.com/questions/11136408/extract-bits-from-32-bit-float-numbers-in-c
      client.write( ((byte*)&temperature)[0] ); //next 4 bytes = float of temperature, LSB first
      client.write( ((byte*)&temperature)[1] );
      client.write( ((byte*)&temperature)[2] );
      client.write( ((byte*)&temperature)[3] );
      client.flush();
      delay(2); //wait to ensure data goes through TCP connection?
      client.stop();
    }
    else{ //could not establish TCP connection to server
      //TODO: log temperature and timestamp to file, to be sent later, when TCP connection can be established
    }
  }
#endif

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
    delay(2); //wait to ensure data goes through TCP connection?
    client.stop();
  }
}
