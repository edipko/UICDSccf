package com.spotonresponse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;

public class DBUtils {

	private static Connection conn = null;
	private static int lastInsertId = 0;

	public DBUtils() {
		getConnection();
	}

	private static void getConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection(Global.DBURL, Global.DBUser, Global.DBPass);
			System.out.println("Database connection established");
		} catch (Exception e) {
			System.err.println("Cannot connect to database server.  -Error: "
					+ e);
			System.err.println("User: " + Global.DBUser);
			System.err.println("Password: " + Global.DBPass);
			System.err.println("URL: " + Global.DBURL);
		}
	}

	public boolean close() {
		try {
			conn.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public boolean insertData(CCFdata data) {
		PreparedStatement s = null;
		try {
			s = conn.prepareStatement("INSERT INTO data(incidentDateTime, location, service, address,"
					+ "code, lat, lon, UUID, active) VALUES(?,?,?,?,?,?,?,?,?);");
			s.setString(1, data.getDateTime());
			s.setString(2, data.getLocation());
			s.setString(3, data.getService());
			s.setString(4, data.getAddress());
			s.setString(5, data.getCode());
			s.setDouble(6, data.getLat());
			s.setDouble(7, data.getLon());
			s.setString(8, data.getUUID());
			s.setBoolean(9, true);
			int count = s.executeUpdate();
			s.close();
			System.out.println(count + " rows were inserted");
		} catch (Exception ex) {
			System.out.println("An error occurred in insertData: " + ex);
			System.out.println("Query: " + s.toString());
			ex.printStackTrace();
			return false;
		}
		return true;
	}

	private void getLastinsertVal() {
		// System.out.println("Getting last insert value");
		try {
			Statement s = conn.createStatement();
			s.executeQuery("SELECT LAST_INSERT_ID();");
			ResultSet rs = s.getResultSet();
			rs.next();
			lastInsertId = rs.getInt(1);
			rs.close();
			s.close();
			// System.out.println("Last insert was: " + lastInsertId);
		} catch (Exception ex) {
			System.out.println("Exception occurred: " + ex);
			lastInsertId = 0;
		}
	}

	public boolean insertGeofence(CCFdata data) {
		PreparedStatement s;
		try {
			s = conn.prepareStatement("INSERT into geofence(active,circle,radius,lat,lon,points) VALUES(?,?,?,?,?,?);");
			s.setBoolean(1, true);
			s.setBoolean(2, true);
			s.setInt(3, 2);
			s.setDouble(4, data.getLat());
			s.setDouble(5, data.getLon());
			s.setString(6, "");

			int count = s.executeUpdate();
			s.close();
			System.out.println(count + " rows were inserted");
			getLastinsertVal();

		} catch (Exception ex) {
			System.out.println("Error: " + ex);
			ex.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean insertIncident(CCFdata data) {
		PreparedStatement s = null;
		try {
			String now = new Date().toString();
			s = conn.prepareStatement("INSERT into incidentUICDS(Name, Snippet, Style, Code, Created, CreatedBy, Descriptor,"
					+ "Event, LastUpdated, LastUpdatedBy, State, Type, Ver, WpID, IGid, geoFenceID) "
					+ "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);");
			s.setString(1, data.getService());
			s.setString(2, data.getLocation() + "\nDistrict: " + data.getCode() + "\nAddress: " + data.getAddress());
			s.setString(3, "");
			s.setString(4, "CCF Alert");
			s.setString(5, data.getDateTime());
			s.setString(6, "CCF Website");
			s.setString(7, data.getLocation());
			s.setString(8, "");
			s.setString(9, now);
			s.setString(10, "CCF Website");
			s.setString(11, "Active");
			s.setString(12, "Incident");
			s.setInt(13, 1);
			s.setString(14, data.getUUID());
			s.setString(15, data.getIGID());
			s.setInt(16, lastInsertId);

			int count = s.executeUpdate();
			s.close();
			System.out.println(count + " rows were inserted - Timestamp is: " + data.getDateTime());
		} catch (Exception ex) {
			System.out.println("An error occurred in insertData: " + ex);
			System.out.println("Query: " + s.toString());
			ex.printStackTrace();
			return false;
		}
		return true;
	}



	public String getEntry(CCFdata data) {
		PreparedStatement s;
		String WpID = "";
		
		try {
			s = conn.prepareStatement("SELECT WpID FROM incidentUICDS WHERE Descriptor = ? AND Created = ?");
			s.setString(1,data.getLocation());
			s.setString(2, data.getDateTime());
			if (s.execute()) {
				ResultSet rs = s.getResultSet();
				if (rs.next()) {
					WpID = rs.getString(1);
				}
				rs.close();
				s.close();
			}
			return WpID;

		} catch (Exception ex) {
			System.out.println("Query error in entryExists: " + ex);
			return WpID;
		}
	}
	
	
	public boolean entryExists(String datetime, String location) {
		PreparedStatement s;
		try {
			s = conn.prepareStatement("SELECT WpID FROM incidentUICDS WHERE Descriptor = ? AND Created = ?");
			s.setString(1, location);
			s.setString(2, datetime);
			
			boolean result = false;
			if (s.execute()) {
				ResultSet rs = s.getResultSet();
				if (rs.next()) {
					result = true;
				}
				rs.close();
				s.close();
			}
			if (result) {
				return true;
			}
			return false;

		} catch (Exception ex) {
			System.out.println("Query error in entryExists: " + ex);
			return false;
		}
	}

	public boolean entryExists(String uuidHash) {
		PreparedStatement s;
		try {
			s = conn.prepareStatement("SELECT * FROM incidentUICDS WHERE WpID = ?");
			s.setString(1, uuidHash);
			boolean result = false;
			if (s.execute()) {
				ResultSet rs = s.getResultSet();
				if (rs.next()) {
					result = true;
				}
				rs.close();
				s.close();
			}
			if (result) {
				return true;
			}
			return false;

		} catch (Exception ex) {
			System.out.println("Query error in entryExists(String uuidHash): " + ex);
			return false;
		}
	}

	public void expireIncident(String wpid) {
		PreparedStatement s;
		try {
			// Expire the geofence first
						s = conn.prepareStatement("UPDATE geofence set active=0 where geofenceID in (SELECT geofenceID from incidentUICDS where WpID=?)");
						s.setString(1, wpid);
						s.execute();
						
			s = conn.prepareStatement("UPDATE incidentUICDS set State='Inactive', LastUpdatedBy='ccdDBUtils' WHERE WpID = ?");
			s.setString(1, wpid);
			s.executeUpdate();
		} catch (Exception ex) {
			System.err.println("expireIncident exception: ");
			ex.printStackTrace();
		}
	}

	public boolean checkExpiration(ArrayList<CCFdata> ccfdata) {
		ArrayList<String> al = new ArrayList<String>();
		for (CCFdata ccfd : ccfdata) {
			try {
				al.add(ccfd.getUUID());
				System.out.println("Checking for expiration: " + ccfd.getUUID());
			} catch (Exception ex) {
                 System.err.println("Error:" + ex);
                 ex.printStackTrace();
			}
		}
		
		PreparedStatement s;
		try {
			s = conn.prepareStatement("SELECT WpID,IGid FROM incidentUICDS WHERE State='Active'");
			if (s.execute()) {
				ResultSet rs = s.getResultSet();
				while (rs.next()) {

					if (!al.contains(rs.getString(1))) {
						System.out.println("Expiring incident: "
								+ rs.getString(1));
						expireIncident(rs.getString(1));
						
						UicdsSoapTools ust = new UicdsSoapTools();
						ust.closeIncident(rs.getString(2));
					}
				}
				rs.close();
			}

			s.close();

		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}

	public void expireOne(String Igid) {
		UicdsSoapTools ust = new UicdsSoapTools();
		ust.closeIncident(Igid);
	}
	
	public void expireSOI(String Igid) {
		UicdsSoapTools ust = new UicdsSoapTools();
		ust.closeSOI(Igid);
	}
	
	public void expireAll() {
		PreparedStatement s;
		try {		
			s = conn.prepareStatement("SELECT IGid FROM incidentUICDS");
			if (s.execute()) {
				ResultSet rs = s.getResultSet();
				UicdsSoapTools ust = new UicdsSoapTools();
				while (rs.next()) {
						ust.closeIncidents(rs.getString(1));	
				}
				ust.executeClose();
				rs.close();
			}

			s.close();		
			//UicdsSoapTools ust = new UicdsSoapTools();
			//ust.closeIncident("IG-139b70a3-c38b-4ee1-bb0a-de400cd272b7");			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
}
