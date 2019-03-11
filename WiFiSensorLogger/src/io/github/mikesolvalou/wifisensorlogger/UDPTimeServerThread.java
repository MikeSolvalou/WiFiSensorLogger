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
				
				InetAddress origin = packet.getAddress();
				long unixTime = Instant.now().getEpochSecond();
				byte[] tBuf = {rBuf[0], (byte) unixTime,
						(byte) (unixTime>>8),
						(byte) (unixTime>>16),
						(byte) (unixTime>>24)};
				
				DatagramPacket packet2 = new DatagramPacket(tBuf, 5, origin, 8007);
				socket.send(packet2);
			}
		}
		catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
