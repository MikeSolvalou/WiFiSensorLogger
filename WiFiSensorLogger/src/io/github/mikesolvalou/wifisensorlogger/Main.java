package io.github.mikesolvalou.wifisensorlogger;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Main {

	public static void main(String[] args) {
		
		//if the db file doesn't exist, create a new one and create the tables
		if(!(new File("C:/sqlite/sensordata.sl3").exists())) {
			initDb();
		}
		//else, presume the tables are already created
		
		//start listening for TCP connection requests; on another thread, because it blocks
		// when a connection request comes in, start a new thread to handle it
		// client should send some data, then close the connection when it's done
		// usually write something to db in response to what comes in
		new TCPDataListenerThread().start();
		
		//start listening for datagrams requesting the current unix time
		new UDPTimeServerThread().start();
		
		//start listening for strings delivered via TCP, to be printed to std out.
		new TCPLoggerThread().start();

		
		//also start a jetty server to respond to http reqs for webpages and data?
		
	}
	
	
	/**connect to database and create tables*/
	private static void initDb() {
		try(Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/sqlite/sensordata.sl3");
				Statement stmt = conn.createStatement()) {
			
			//each row describes a sensor and the surrounding hardware that connects it via WiFi
			// a sensor's id is stored in its WiFi module's program memory
			stmt.execute("CREATE TABLE Sensors(id INTEGER PRIMARY KEY, "
					+ "sensorModel TEXT, "	//ex. DHT-11, DS18B20
					+ "serial BLOB, "	//ex. DS18B20's 64b serial code
					+ "wifiModuleModel TEXT,"	//ex. ESP-01, ESP-12F
					//description of the hardware around the actual sensor, anything that may affect the measurement
					+ "hardwareDescription TEXT);");
			
			//each row describes a location that a sensor may be or has been placed in
			stmt.execute("CREATE TABLE Locations(id INTEGER PRIMARY KEY, " + 
					"description TEXT UNIQUE NOT NULL);");	//description of location
			
			//each row represents the placement of a sensor, at a location, at a certain time
			stmt.execute("CREATE TABLE Installations(id INTEGER PRIMARY KEY, " + 
					"sensor INTEGER NOT NULL," + 
					"location INTEGER NOT NULL," + //sensor presumed to be at this location until another row says it has been moved
					"time INTEGER NOT NULL," + //unix time sensor was placed at location
					"UNIQUE(sensor, time)," + 
					"FOREIGN KEY(sensor) REFERENCES Sensors(id)," + 
					"FOREIGN KEY(location) REFERENCES Locations(id));");
			
			//each row represents a temperature measurement, made at a certain time, by a certain sensor
			stmt.execute("CREATE TABLE Temperatures(" + 
					//corresponds to a sensor id, but no foreign key constraint, to permit measurements by sensors not in the database
					" sensor INTEGER NOT NULL," + 
					" timestamp INTEGER NOT NULL," + //unix time of measurement
					" temperature REAL NOT NULL," + //in Celsius
					" PRIMARY KEY(sensor, timestamp));");//permits fast lookup of measurements by sensor, then by time range
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
	}

}
