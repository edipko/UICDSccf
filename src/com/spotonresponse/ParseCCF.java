package com.spotonresponse;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

public class ParseCCF {

	public static void main(String[] args) {

		/*
		 * Read the command line options to get the location of the properties
		 * file we are to use
		 */
		String propertiesFile = System.getProperty("propfile");
		try {
			propertiesFile.length();
		} catch (Exception ex) {
				System.out.println("No properties file specified: " + ex);
				System.exit(1);
		}
		

		/*
		 * Read the properties file for connection and user information
		 */
		Properties prop = new Properties();
		try {
			// load a properties file
			prop.load(new FileInputStream(propertiesFile));

			// get the property value and print it out
			Global.uicdsURL = prop.getProperty("uicdsURL");
			Global.uicdsUser = prop.getProperty("uicdsUser");
			Global.uicdsPass = prop.getProperty("uicdsPass");
			
			Global.DBURL = prop.getProperty("DBurl");
			Global.DBUser = prop.getProperty("DBuser");
			Global.DBPass = prop.getProperty("DBpassword");
			
			Global.UICDStimeout = Integer.valueOf(prop.getProperty("UICDStimeout"));
			
			System.out.println("Done reading properties: DBURL: " + Global.DBURL);
			
		} catch (IOException ex) {
			System.err.println("Error setting properties");
			ex.printStackTrace();
			System.exit(2);
		}

		
		try {
			String url = args[0];
			
			if (url.equals("ExpireSOI")) {
				String Igid = args[1];
				DBUtils dbu = new DBUtils();
				dbu.expireSOI(Igid);
				System.exit(0);
			}
			
			
			if (url.equals("ExpireAll")) {
				DBUtils dbu = new DBUtils();
				dbu.expireAll();
				System.exit(0);
			}
			
			if (url.equals("ExpireIgID")) {
				String Igid = args[1];
				DBUtils dbu = new DBUtils();
				dbu.expireOne(Igid);
				System.exit(0);
			}
			
			
			
			
			getData(url);
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void getData(String url) throws IOException {
		

		/*
		 * Create a connection to our local database
		 */
		DBUtils dbu = new DBUtils();

		/*
		 * Query the website and pull down the page 
		 */
		long start = System.currentTimeMillis();
		Document doc = Jsoup.connect(url).get();
		long end = System.currentTimeMillis();
		long totalTime = end - start;
        System.out.println("Got ccf data in: " + totalTime + "ms");
        
        /* 
         * Parse the HTML document - the data we need is inside the only 
         * HTML table on the page
         * 
         * Hope they don't change anything on the page or this will break very quickly
         */
		Elements trs = doc.select("tr");
		List<Map<String, String>> incidentList = new ArrayList<Map<String, String>>();
		Map<String, String> incidentData = new HashMap<String, String>();

		start = System.currentTimeMillis();
		for (Element tr : trs) {
			if (tr.hasAttr("bgcolor")) {
				int place = 0;
				incidentData = new HashMap<String, String>();
				boolean goodaddress=false;
				for (Element td : tr.children()) {
					Node font = td.childNode(0);
					String tdContents = font.childNode(0).toString();
					if (!tdContents.contains("<b>")
							&& (!tdContents.contains("a href"))) {

						switch (place) {
						case 0:
							incidentData.put("datetime", font.childNode(0)
									.toString());
							break;
						case 1:
							incidentData.put("location", font.childNode(0)
									.toString());
							break;
						case 2:
							incidentData.put("service", font.childNode(0)
									.toString());
							break;
						case 3:
							incidentData.put("address", font.childNode(0)
									.toString());
							goodaddress=true;
							break;
						case 4:
							incidentData.put("code", font.childNode(0)
									.toString());
							break;
						}
					}
					place++;
				}
				
				if (goodaddress) {
				   incidentList.add(incidentData);
				}
			}
		}
	    
		end = System.currentTimeMillis();
		totalTime = end - start;
		System.out.println("Done parsing web feed in " + totalTime + "ms");
		
		/*
		 * Create an arraylist of our CCFdata entires
		 * We will use this later to compare with the active entries in our
		 * database and expire anything that is no longer on the website
		 */
		ArrayList<CCFdata> ccfdata = new ArrayList<CCFdata>();

		
		/*
		 * Loop through the List we created by parsing the website
		 */
		for (Map<String, String> iMap : incidentList) {
			String dt = iMap.get("datetime");
			String location = iMap.get("location");
			String service = iMap.get("service");
			String address = iMap.get("address");
			String code = iMap.get("code");

			
			try {
				/*
				 * If we have an address - we can proceed
				 */
				//if (!iMap.get("address").isEmpty()) {
				if (iMap.get("address").length() > 4) {
					
					/*
					 * Store the data - add it to the array so we can remove
					 * expired events
					 */
					
					/*
					 * We get the date/time in this format from the site Sep 7 2012 11:11PM
					 * We need to convert it to UTC to store it in the database
					 */
					
					SimpleDateFormat lv_formatter;  
					SimpleDateFormat lv_parser; 
					//create a new Date object using the timezone of the specified city
					lv_parser = new SimpleDateFormat("MMM d yyyy h:mmaaa");
					
					TimeZone timeZone = TimeZone.getTimeZone("America/Los_Angeles");
					lv_parser.setTimeZone(timeZone);
					Date lv_localDate = lv_parser.parse(dt);

					//Convert the date from the local timezone to UTC timezone
					lv_formatter = new SimpleDateFormat("MMM d yyyy h:mmaaa"); 
					lv_formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
					String datetimeUTC = lv_formatter.format(lv_localDate);
					
					CCFdata ccfd = new CCFdata();
					ccfd.setAddress(address);
					ccfd.setCode(code);
					ccfd.setDateTime(datetimeUTC);
					ccfd.setLocation(location);
					ccfd.setService(service);
					start = System.currentTimeMillis();
					
					if (!dbu.entryExists(datetimeUTC, location)) {
						
						/*
						 * The entry doesn't exists
						 * Go get a lat/long to coorespond to the address we pulled
						 */
						//http://maps.googleapis.com/maps/api/geocode/xml 
						//String key = "AIzaSyDvu0lsvg3VCRWkne7dYzQN9XXyyg15ASk";
						URI uri = new URI("http", "maps.google.com", "/maps/api/geocode/xml",
								"address=" + address + "&sensor=false",
								null);
						String geurl = uri.toASCIIString();
						start = System.currentTimeMillis();
						//Document gedoc = Jsoup.connect(geurl).get();
						DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
						DocumentBuilder db = dbf.newDocumentBuilder();
						org.w3c.dom.Document gedoc = db.parse(new URL(geurl).openStream());
						
						end = System.currentTimeMillis();
						totalTime = end - start;
						System.out.println("Got lat/lon from Google in " + totalTime + "ms");
						
						
						
						XPathFactory factory = XPathFactory.newInstance();
					    XPath xpath = factory.newXPath();

					    XPathExpression xPathExpression = xpath.compile("//GeocodeResponse/result/geometry/location/lat");
					    ccfd.setLat(Double.valueOf(xPathExpression.evaluate(gedoc,XPathConstants.STRING).toString()));
					    xPathExpression = xpath.compile("//GeocodeResponse/result/geometry/location/lng");
					    ccfd.setLon(Double.valueOf(xPathExpression.evaluate(gedoc,XPathConstants.STRING).toString()));
					    
						end = System.currentTimeMillis();
						totalTime = end - start;
						System.out.println("Total Address lookup and parsing time took: " + totalTime + "ms");
						
						
						/*
						 * Now create a new WorkProduct and Incident in the UICDS core
						 * from the response - grab the WpID and IGid so we can store
						 * them in our database
						 */
						UicdsSoapTools ust = new UicdsSoapTools();
						
						ust.setCCFD(ccfd);
						ust.sendXML();			
						ccfd.setUUID(ust.getWpID());
						ccfd.setIGID(ust.getIGid());

						/*
						 * Add it to the array of incidents
						 */
						ccfdata.add(ccfd);

						/*
						 * This is a new entry, add to the database
						 */
						start = System.currentTimeMillis();
						/*
						 * Create the Geofence
						 */
						if (dbu.insertGeofence(ccfd)) {
							end = System.currentTimeMillis();
							totalTime = end - start;
							System.out.println("Geofence Insert took: " + totalTime + "ms");
							
							/*
							 * Create the incident
							 */
							start = System.currentTimeMillis();
							if (dbu.insertIncident(ccfd)) {
								end = System.currentTimeMillis();
								totalTime = end - start;
								System.out.println("Incident Insert took: " + totalTime + "ms");
								
							} else {
								System.out
										.println("incident data not inserted");
							}
						} else {
							
							System.out.println("Goefence data not inserted");
						}
						
					} else {
						
						/*
						 * The entry already exists... but we still need it in our array so that
						 * it doesn't get put through the expiry process later in the script.
						 */
						ccfd.setUUID(dbu.getEntry(ccfd));
						ccfdata.add(ccfd);
						//System.out.println("Entry exists");
					}
					
				}

			} catch (java.lang.NullPointerException e) {
				e.printStackTrace();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		/*
		 * Expire old entries in the database that no longer exist in the feed
		 */
		System.out.println("Checking Expiration...");
		dbu.checkExpiration(ccfdata);

	}
	
}
