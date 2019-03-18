//derived from https://docs.oracle.com/javase/tutorial/networking/sockets/examples/EchoServer.java
//and https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html

package io.github.mikesolvalou.wifisensorlogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**Thread to receive data from a remote sensor and store it in the database.*/
public class TCPDataReceiverThread extends Thread {
	
	private Socket clientSocket;	//socket to remote device
	
	public TCPDataReceiverThread(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}

	public void run() {
		//open InputStream to receive bytes through Socket; clientSocket gets closed when in gets closed
		try(InputStream in = clientSocket.getInputStream() ){
			//WiFi module sends bytes, InputStream reads bytes
			// 1 byte for sensor id
			// next 4 bytes for unix time, whole seconds only, lsB first
			// next byte = 'T' for temperature (in Celsius), or 'H' for humidity (unitless)
			// next 4 bytes for float of measurement, lsB first
			// 10 bytes total
			byte[] bytes = new byte[20];
			int bytesRead=0;
			int nextByte=0;
			//need this loop because overloads of read() return without reading all the bytes
			while( (nextByte = in.read())!=-1 && bytesRead<20) {
				bytes[bytesRead++] = (byte) nextByte;
			}
			
			System.out.printf("Sensor data connection closed. remote address: %s, remote port: %d%n",
					clientSocket.getInetAddress(), clientSocket.getPort());
			
			//indicate error if too many or too few bytes were sent
			if(bytesRead!=10)
				throw new Exception("ERROR: wrong number of bytes received="+bytesRead);
			
			//sanity checks:
			// sensor id between 0 and 100
			int sensorId = bytes[0];
			if(sensorId<0 || sensorId>100)
				throw new Exception("ERROR: unexpected sensor id: "+bytes[0]);
			
			// unix time is after some value
			int timestamp = bytes[1] & 0x000000ff | bytes[2]<<8 & 0x0000ff00 |
					bytes[3]<<16 & 0x00ff0000 | bytes[4]<<24 & 0xff000000;
			if(timestamp < 1550000000)
				throw new Exception("ERROR: timestamp too early: "+timestamp);
			
			// bytes[5] is 'T', for temperature //TODO: or 'H', for humidity
			if(bytes[5]!='T')
				throw new Exception("ERROR: unexpected character at bytes[5]: "+((char)(bytes[5])) );
			
			// temperature between 3 and 40
			int floatBits = bytes[6] & 0x000000ff | bytes[7]<<8 & 0x0000ff00 |
					bytes[8]<<16 & 0x00ff0000 | bytes[9]<<24 & 0xff000000;
			float temperature = Float.intBitsToFloat(floatBits);
			if(temperature<3 || temperature>40)
				throw new Exception("ERROR: unexpected temperature: "+temperature+"C");
			
			// humidity between 0 and 1
			// TODO
			
			System.out.printf("Sanity check ok. timestamp: %s, temperature: %f C, sensorId: %d%n",
					ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault()),
					temperature, sensorId);
			
			
			//if sensor id != 0, insert row into database
			try(Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/sqlite/sensordata.sl3");
					PreparedStatement pstmt = conn.prepareStatement(
							"INSERT INTO Temperatures(sensor, timestamp, temperature) VALUES(?,?,?);")){
				pstmt.setInt(1, sensorId);
				pstmt.setInt(2, timestamp);
				pstmt.setFloat(3, temperature);
				int rowsModified = pstmt.executeUpdate();
				
				System.out.printf("Executed SQL:%n"
						+ "INSERT INTO Temperatures(sensor, timestamp, temperature) VALUES(%d,%d,%f);%n%n"
						+ "Rows modified: %d%n%n",
						sensorId, timestamp, temperature, rowsModified);
			}
			
			
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		//clientSocket closed
		
		
	}
}
