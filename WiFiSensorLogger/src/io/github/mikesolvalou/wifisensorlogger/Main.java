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
			
			stmt.execute("CREATE TABLE Sensors(id INTEGER PRIMARY KEY, model TEXT, serial BLOB);");
			stmt.execute("CREATE TABLE Locations(id INTEGER PRIMARY KEY, " + 
					"description TEXT UNIQUE NOT NULL);");
			stmt.execute("CREATE TABLE Installations(id INTEGER PRIMARY KEY, " + 
					"sensor INTEGER NOT NULL," + 
					"location INTEGER NOT NULL," + 
					"time INTEGER NOT NULL," + 
					"UNIQUE(sensor, time)," + 
					"FOREIGN KEY(sensor) REFERENCES Sensors(id)," + 
					"FOREIGN KEY(location) REFERENCES Locations(id));");
			stmt.execute("CREATE TABLE Temperatures(" + 
					" sensor INTEGER NOT NULL," + 
					" timestamp INTEGER NOT NULL," + 
					" temperature REAL NOT NULL," + 
					" PRIMARY KEY(sensor, timestamp));");
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
	}

}
