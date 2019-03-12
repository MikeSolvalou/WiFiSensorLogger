package io.github.mikesolvalou.wifisensorlogger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**Thread to listen on port 8009 for TCP connection requests carrying strings, and print them to standard out.
 * 
 * Permits printlnTCP() to be used in place of Serial.println() on the ESP-01, since Serial.println() only 
 * works when it's plugged in to the PC via USB.*/
public class TCPLoggerThread extends Thread {
	
	public void run() {
		System.out.println("TCP logger thread started.");
		//open socket to listen for connection requests
		try(ServerSocket serverSocket = new ServerSocket(8009)){
			
			while(true) {
				try(Socket clientSocket = serverSocket.accept();	//blocks until a connection request arrives
						InputStreamReader in = new InputStreamReader(clientSocket.getInputStream()) ){
					
					System.out.printf("From: %s:%d : ", clientSocket.getInetAddress(), clientSocket.getPort());
					
					StringBuffer s = new StringBuffer();
					for(int nextChar=in.read(); nextChar!=-1; nextChar=in.read()) {
						s.append((char)nextChar);
					}
					System.out.println(s);
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
