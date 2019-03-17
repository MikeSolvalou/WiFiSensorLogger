//derived from https://docs.oracle.com/javase/tutorial/networking/sockets/examples/EchoServer.java
//and https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html

package io.github.mikesolvalou.wifisensorlogger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**Thread to listen on port 8005 for TCP connection requests carrying sensor data to the server.*/
public class TCPDataListenerThread extends Thread {
	
	public void run() {
		System.out.println("TCP data listener thread started.");
		//open socket to listen for connection requests
		try(ServerSocket serverSocket = new ServerSocket(8005)){
			
			while(true) {
				Socket clientSocket = serverSocket.accept();	//blocks until a connection request arrives
				System.out.printf("Sensor data connection established, local address: %s, local port: %d, "+
						"remote address: %s, remote port: %d%n",
						clientSocket.getLocalAddress(), clientSocket.getLocalPort(),
						clientSocket.getInetAddress(), clientSocket.getPort() );
				//create thread to deal with each incoming connection
				new TCPDataReceiverThread(clientSocket).start();
			}
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}
}
