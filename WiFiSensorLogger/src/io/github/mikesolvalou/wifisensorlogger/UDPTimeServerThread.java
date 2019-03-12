package io.github.mikesolvalou.wifisensorlogger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.time.Instant;

public class UDPTimeServerThread extends Thread {
	
	public void run() {
		System.out.println("Time server thread started.");
		try (DatagramSocket socket = new DatagramSocket(8006)) {
			while(true) {
				byte[] rBuf = {0};
				DatagramPacket packet = new DatagramPacket(rBuf, 1);
				
				//blocks until datagram arrives
				socket.receive(packet);
				System.out.printf("Got time request from %s port %d.%n", packet.getAddress(), packet.getPort());
				InetAddress origin = packet.getAddress();
				
				//time critical processing start
				Instant now = Instant.now();
				long unixTime = now.getEpochSecond();
				int unixTimeMs = now.getNano()/1000000;
				
				byte[] tBuf = {rBuf[0], (byte) unixTime, (byte) (unixTime>>8),
						(byte) (unixTime>>16), (byte) (unixTime>>24),
						(byte) unixTimeMs, (byte) (unixTimeMs>>8) };
				
				DatagramPacket packet2 = new DatagramPacket(tBuf, 7, origin, 8007);
				socket.send(packet2);
				//time critical processing done
			}
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
