package io.github.mikesolvalou.wifisensorlogger;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

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
		
		//parse query segment of url and create map of keys to values
		Map<String, String> urlQueryPairs;
		String queryString = request.getQueryString();
		if(queryString==null)
			queryString="";
		
		try {
			urlQueryPairs = parseQuery(queryString);
		}
		catch (ParseException e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		//check url query parameters
		if(!checkUrlQuery(urlQueryPairs)) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		
		//query database and produce response
		try(Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/sqlite/sensordata.sl3");
				PreparedStatement pstmt0 = conn.prepareStatement(
						"SELECT V.id, V.sensor, V.time timePlaced, W.time timeRemoved " + 
						"FROM Installations V " + 
						"LEFT JOIN Installations W " + 
						"ON W.sensor=V.sensor AND W.time= " + 
						"(SELECT min(U.time) FROM Installations U WHERE U.sensor=V.sensor AND U.time>V.time) " + 
						"WHERE V.location=?;");
				PreparedStatement pstmt1 = conn.prepareStatement(
					"SELECT sensor, timestamp, temperature FROM Temperatures WHERE sensor=? AND timestamp BETWEEN ? AND ?;");
				PreparedStatement pstmt2 = conn.prepareStatement(
					"SELECT sensor, timestamp, temperature FROM Temperatures WHERE sensor=? AND timestamp > ?;")) {
			
			//query to find out which sensors were at location X at what times; X = location given in url query
			pstmt0.setInt(1, Integer.parseInt(urlQueryPairs.get("loc")) );
			ResultSet rs1 = pstmt0.executeQuery();
			
			//queries for all measurements from location X
			// start writing response body csv, print headings
			//  see bottom of https://commons.apache.org/proper/commons-csv/user-guide.html
			final CSVPrinter printer = CSVFormat.DEFAULT.withHeader("sensor", "timestamp", "temperature")
					.print(response.getWriter());
			
			// run a query for each row of rs1	//TODO: adjust the time bounds on each sql query for the time bounds in the url query
			while(rs1.next()) {
				ResultSet rs2=null;
				
				rs1.getInt("timeRemoved");
				if(rs1.wasNull()) {	//if timeRemoved field is NULL
					//run pstmt2
					pstmt2.setInt(1, rs1.getInt("sensor"));
					pstmt2.setInt(2, rs1.getInt("timePlaced"));
					rs2=pstmt2.executeQuery();
				}
				else {	//if timeRemoved field is not NULL
					//run pstmt1
					pstmt1.setInt(1, rs1.getInt("sensor"));
					pstmt1.setInt(2, rs1.getInt("timePlaced"));
					pstmt1.setInt(3, rs1.getInt("timeRemoved"));
					rs2=pstmt1.executeQuery();
				}
				
				//print rs2 as csv to HTTP reponse body
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

	
	/**Convert URL query string into Map of parameter keys to values.
	 * @param	queryString		query segment of URL as string, excluding leading '?'
	 * @return	Map of parameter keys to values
	 * @throws	ParseException	if queryString is not an &-separated list of "key=value" pairs*/
	private Map<String, String> parseQuery(String queryString) throws ParseException{
		Map<String,String> urlQueryPairs = new HashMap<>();
		if(queryString.isEmpty())
			return urlQueryPairs;
		
		String[] pairs = queryString.split("&");
		for(String pair : pairs) {
			String[] kv = pair.split("=");
			
			if(kv.length!=2)
				throw new ParseException("Expected one '=' in: "+pair, 0);
			
			urlQueryPairs.put(kv[0], kv[1]);
		}
		
		return urlQueryPairs;
	}


	/**Check that there is a parameter with key='loc' (location), and all values can be parsed as integers.
	 * @param	urlQueryPairs	Map of URL parameter keys to values
	 * @return	true if URL query segment is okay, false otherwise.*/
	private boolean checkUrlQuery(Map<String, String> urlQueryPairs) {
		//check parameter 'loc' exists
		String locValue = urlQueryPairs.get("loc");
		if(locValue==null)
			return false;
		
		//check all values can be parsed as integers
		for(String value : urlQueryPairs.values()) {
			try {
				Integer.parseInt(value);
			}
			catch(NumberFormatException nfe){
				return false;
			}
		}
		
		return true;
	}
}
