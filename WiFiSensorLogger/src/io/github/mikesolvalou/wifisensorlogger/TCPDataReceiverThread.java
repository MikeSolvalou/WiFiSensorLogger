//derived from https://docs.oracle.com/javase/tutorial/networking/sockets/examples/EchoServer.java
//and https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html

package io.github.mikesolvalou.wifisensorlogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TCPDataReceiverThread extends Thread {
	
	private Socket clientSocket;	//socket to connected ESP-01
	
	public TCPDataReceiverThread(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}

	public void run() {
		//open Writer and Reader to send/receive data through Socket
		try(PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);	//might not be needed...
				BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())) ){
			
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				System.out.println("Rx: "+inputLine);	//just print to std out for now
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
