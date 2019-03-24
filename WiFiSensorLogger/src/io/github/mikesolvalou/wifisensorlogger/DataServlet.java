package io.github.mikesolvalou.wifisensorlogger;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**Servlet to respond to requests for temperature data.*/
@SuppressWarnings("serial")
public class DataServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		try(Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/sqlite/sensordata.sl3");
				Statement stmt = conn.createStatement();
				PreparedStatement pstmt1 = conn.prepareStatement(
					"SELECT sensor, timestamp, temperature FROM Temperatures WHERE sensor=? AND timestamp BETWEEN ? AND ?;");
				PreparedStatement pstmt2 = conn.prepareStatement(
					"SELECT sensor, timestamp, temperature FROM Temperatures WHERE sensor=? AND timestamp > ?;")) {
			
			//query to find out which sensors were at location 1 at what times //TODO: choose location from url query segment
			ResultSet rs1 = stmt.executeQuery("SELECT V.id, V.sensor, V.time timePlaced, W.time timeRemoved " + 
					"FROM Installations V " + 
					"LEFT JOIN Installations W " + 
					"ON W.sensor=V.sensor AND W.time= " + 
					"(SELECT min(U.time) FROM Installations U WHERE U.sensor=V.sensor AND U.time>V.time) " + 
					"WHERE V.location=1;");
			
			//queries for all measurements from location 1 //TODO: add time filter, choose location from url query segment
			// start writing response body csv, print headings
			//  see bottom of https://commons.apache.org/proper/commons-csv/user-guide.html
			final CSVPrinter printer = CSVFormat.DEFAULT.withHeader("sensor", "timestamp", "temperature")	//TODO: extract headers from ResultSet object rs2
					.print(response.getWriter());
			
			// run a query for each row of rs1
			while(rs1.next()) {
				ResultSet rs2=null;
				
				rs1.getInt("timeRemoved");
				if(rs1.wasNull()) {	//if timeRemoved field is NULL
					//run pstmt2
					pstmt2.setInt(1, rs1.getInt("sensor"));
					pstmt2.setInt(2, rs1.getInt("timePlaced"));
					rs2=pstmt2.executeQuery();
				}
				else {
					//run pstmt1
					pstmt1.setInt(1, rs1.getInt("sensor"));
					pstmt1.setInt(2, rs1.getInt("timePlaced"));
					pstmt1.setInt(3, rs1.getInt("timeRemoved"));
					rs2=pstmt1.executeQuery();
				}
				
				//print rs2 as csv to HTTP reponse body	//TODO: on first while-loop, extract column headers from rs2 ?
				while(rs2.next()) {
					printer.print(rs2.getInt("sensor"));
					printer.print(rs2.getInt("timestamp"));
					printer.print(rs2.getFloat("temperature"));
					printer.println();
				}
				
			}
			
			response.setContentType("text/csv");
			response.setStatus(HttpServletResponse.SC_OK);
		}
		catch (SQLException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			e.printStackTrace();
		}
		//ResultSets should auto-close when Statements are closed
		
	}
}
