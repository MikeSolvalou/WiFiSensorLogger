package io.github.mikesolvalou.wifisensorlogger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TimeSyncTester {

	//listen for debug messages coming in via UDP datagrams
	public static void main(String[] args) throws IOException {
		System.out.println("Time sync tester started.");
		DatagramSocket socket = new DatagramSocket(8008);
		
		while(true) {
			byte[] buf = new byte[4];	//only expect 4-byte datagrams
			DatagramPacket packet = new DatagramPacket(buf, 4);
			
			//blocks until a datagram comes in
			socket.receive(packet);
			
			Instant systemTimePacketReceived = Instant.now();//do this as soon as possible after datagram received
			
			int esp01TimePacketSent_ = buf[0] & 0x000000ff | buf[1]<<8 & 0x0000ff00
					| buf[2]<<16 & 0x00ff0000 | buf[3]<<24 & 0xff000000;
			Instant esp01TimePacketSent = Instant.ofEpochSecond(esp01TimePacketSent_);
			
			
			System.out.printf("System: %s%nESP-01: %s%n%n",
					ZonedDateTime.ofInstant(systemTimePacketReceived, ZoneId.systemDefault()),
					ZonedDateTime.ofInstant(esp01TimePacketSent, ZoneId.systemDefault()) );
			
		}
	}

}
