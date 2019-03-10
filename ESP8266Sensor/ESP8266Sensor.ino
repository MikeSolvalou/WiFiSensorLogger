#include <ESP8266WiFi.h>
#include "WiFiCredentials.h"  //git is set to ignore this file

//derived from https://arduino-esp8266.readthedocs.io/en/latest/esp8266wifi/readme.html
//and https://tttapa.github.io/ESP8266/Chap07%20-%20Wi-Fi%20Connections.html

WiFiClient client;  //for TCP connection from this device to server
int counter=0;

void setup() {
  //init serial port
  Serial.begin(115200);
  delay(10);

  //connect to home network
  WiFi.begin(NETWORK_NAME, NETWORK_PASSWORD);
  Serial.print("Connecting to ");
  Serial.print(NETWORK_NAME);
  Serial.println(" ...");

  while(WiFi.status() != WL_CONNECTED){
    delay(1000);
    Serial.print(".");
  }

  Serial.println("Connected to WiFi");
  Serial.print("IP address:\t");
  Serial.println(WiFi.localIP());

  //open TCP connection with Java TCP server
  if(client.connect("192.168.1.5", 8005)){+
  
    Serial.println("Connected to TCP server.");
  }
  else{
    Serial.println("Could not connect to TCP server.");
  }
}

void loop() {
  if(client.connected()){
    client.println(counter);
    counter++;
  }
  else{
    Serial.println("Not connected to TCP server");
  }

  delay(3000);
}
