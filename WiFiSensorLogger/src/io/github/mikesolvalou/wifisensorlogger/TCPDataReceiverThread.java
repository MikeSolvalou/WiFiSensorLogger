//derived from https://docs.oracle.com/javase/tutorial/networking/sockets/examples/EchoServer.java
//and https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html

package io.github.mikesolvalou.wifisensorlogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**Thread to receive data from a remote sensor and store it in the database.*/
public class TCPDataReceiverThread extends Thread {
	
	private Socket clientSocket;	//socket to remote device
	
	public TCPDataReceiverThread(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}

	public void run() {
		//open Writer and Reader to send/receive chars through Socket
		try(PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);	//might not need out...
				BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())) ){
			
			for(int nextChar=in.read(); in.read()!=-1; nextChar=in.read()) {
				char c = (char)nextChar;
			}
			
			System.out.printf("Connection closed. remote address: %s, remote port: %d%n%n",
					clientSocket.getInetAddress(), clientSocket.getPort());
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
