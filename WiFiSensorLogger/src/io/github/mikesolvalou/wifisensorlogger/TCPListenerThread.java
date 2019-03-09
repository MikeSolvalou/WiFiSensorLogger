//derived from https://docs.oracle.com/javase/tutorial/networking/sockets/examples/EchoServer.java
//and https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html

package io.github.mikesolvalou.wifisensorlogger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**Thread to listen for connection requests from ESP-01 modules.*/
public class TCPListenerThread extends Thread {
	
	public void run() {
		//open socket to listen for connection requests
		try(ServerSocket serverSocket = new ServerSocket(8005)){
			
			while(true) {
				//blocks until a connection request arrives
				Socket clientSocket = serverSocket.accept();
				
				//create thread to deal with each incoming connection
				new TCPDataReceiverThread(clientSocket).start();
			}
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}
}
