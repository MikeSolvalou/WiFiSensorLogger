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
			
			//ESP-01 sends bytes, InputStream reads bytes
			//use counter to count bytes until end of stream; indicate error if too many or too few were sent
			
			//1 byte for sensor id
			//next 4 bytes for unix time, whole seconds only, lsB first
			//next byte = 'T' for temperature (in Celsius), or 'H' for humidity (unitless)
			//next 4 bytes for float of measurement, lsB first
			//10 bytes total
			
			//sanity checks:
			//sensor id under 100
			//unix time over some value
			//temperature between 0 and 50
			//humidity between 0 and 1
			
			//insert row into database
			
			System.out.printf("Connection closed. remote address: %s, remote port: %d%n%n",
					clientSocket.getInetAddress(), clientSocket.getPort());
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
