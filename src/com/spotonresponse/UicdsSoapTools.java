package com.spotonresponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

final class readXML {
	public InputStream getXML() {
		return getClass().getResourceAsStream("SOAPnewIncidentRequest.xml");
	}
	public InputStream getCloseXML() {
		return getClass().getResourceAsStream("RequestClose.xml");
	}
	public InputStream getArchiveXML() {
		return getClass().getResourceAsStream("RequestArchive.xml");
	}
	public InputStream getCloseSOIXML() {
		return getClass().getResourceAsStream("RequestCloseSOI.xml");
	}
	public InputStream getArchiveSOIXML() {
		return getClass().getResourceAsStream("RequestArchiveSOI.xml");
	}
}



public class UicdsSoapTools {
	private String _WpID = "";
	private String _IGid = "";
	private CCFdata _ccf;
	
	private String closeXML = "";
    private String archiveXML = "";
	
	public UicdsSoapTools() {

		HostnameVerifier hv = new HostnameVerifier() {
			public boolean verify(String urlHostName, SSLSession session) {
				System.out.println("Warning: URL Host: " + urlHostName
						+ " vs. " + session.getPeerHost());
				return true;
			}
		};

		HttpsURLConnection.setDefaultHostnameVerifier(hv);
		
	}

	private String sendRequest(String xml) {
		try {
      
			String SOAPUrl = Global.uicdsURL;
			System.out.println("Connecting to UICDS: " + SOAPUrl);
			
			String authString = Global.uicdsUser + ":" + Global.uicdsPass;
			byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
			String authStringEnc = new String(authEncBytes);

			URL url;
			HttpURLConnection connection = null;

			byte[] b = xml.getBytes();

			// Create connection
			url = new URL(SOAPUrl);
			System.out.println("Connecting to: " + url.toString());
			connection = (HttpURLConnection) url.openConnection();
			HttpURLConnection httpConn = (HttpURLConnection) connection;
			connection.setConnectTimeout(Global.UICDStimeout * 1000);
			connection.setRequestProperty("Authorization", "Basic "
					+ authStringEnc);

			httpConn.setRequestProperty("Content-Length",
					String.valueOf(b.length));
			httpConn.setRequestProperty("Content-Type",
					"text/xml; charset=utf-8");
			httpConn.setRequestProperty("SOAPAction", "");
			httpConn.setRequestMethod("POST");
			httpConn.setDoOutput(true);
			httpConn.setDoInput(true);

			// Everything's set up; send the XML that was read in to b.
			OutputStream out = httpConn.getOutputStream();
			out.write(b);
			out.close();

			// Read the response and write it to standard out.

			InputStreamReader isr = new InputStreamReader(
					httpConn.getInputStream());
			BufferedReader in = new BufferedReader(isr);

			StringBuilder sb = new StringBuilder();

			String line;
			while ((line = in.readLine()) != null) {
				sb.append(line);
			}
			return sb.toString();
		} catch (java.net.SocketTimeoutException ste) {
			System.err.println("Timeout connecting to UICDS");
			return null;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	public void sendXML() {
		CCFdata ccf = this._ccf;
		String xml = convertStreamToString(new readXML().getXML());
		
		// parse the string
		DateTimeFormatter formatter = DateTimeFormat
				.forPattern("MMM dd yyyy hh:mma");
		DateTime dateTime = formatter.parseDateTime(ccf.getDateTime());

		SimpleDateFormat outputFormatter = new SimpleDateFormat(
				"yyyy-MM-dd'T'HH:mm:ss");
		String dt = outputFormatter.format(dateTime.toDate());

		xml = xml.replace("{DATETIME}", dt);
		xml = xml.replace("{ACTIVITY}", ccf.getService());
		xml = xml.replace("{DESCRIPTION}", ccf.getLocation() + "\nDistrict: " + ccf.getCode() + "\nAddress: " + ccf.getAddress());
		xml = xml.replace("{NAME}", "CCF Website");
		xml = xml.replace("{ORGANIZATION}", ccf.getCode());
		xml = xml.replace("{ADDRESS}", ccf.getAddress());
		xml = xml.replace("{RADIUS}", "2");

		// Calculate LAT deg/min/dec
		Double decLatitude = ccf.getLat();
		int latDegree = (int) Math.round(decLatitude);
		Double vFraction = Math.abs(((decLatitude - latDegree) * 3600) % 3600);
		int latMinute = (int) Math.round(vFraction / 60);
		int latSecond = (int) Math.round(vFraction % 60);

		xml = xml.replace("{LATDEG}", Double.toString(latDegree));
		xml = xml.replace("{LATMIN}", Double.toString(latMinute));
		xml = xml.replace("{LATSEC}", Double.toString(latSecond));

		Double decLongitude = ccf.getLon();
		int lonDegree = (int) Math.round(decLongitude);
		Double vFraction2 = Math
				.abs(((decLongitude - lonDegree) * 3600) % 3600);
		int lonMinute = (int) Math.round(vFraction2 / 60);
		int lonSecond = (int) Math.round(vFraction2 % 60);

		xml = xml.replace("{LONDEG}", Double.toString(lonDegree));
		xml = xml.replace("{LONMIN}", Double.toString(lonMinute));
		xml = xml.replace("{LONSEC}", Double.toString(lonSecond));

		System.out.println("Sending document: " + xml);
		
		/*
		 * Send the document
		 * 
		 */
		long start = System.currentTimeMillis();
		String response = sendRequest(xml);
		long end = System.currentTimeMillis();
		long totalTime = end - start;
		System.out.println("UICDS add took: " + totalTime + "ms");
		
		/*
		 * Parse the response
		 */
		start = System.currentTimeMillis();
			try {
				Document doc = loadXMLFromString(response);
			
			String WpID = "";
			String IGid = "";

			/*
			 * Get the WPid
			 */
			NodeList nl = doc
					.getElementsByTagName("str:WorkProductIdentification");
			if (nl != null && nl.getLength() > 0) {
				for (int i = 0; i < nl.getLength(); i++) {
					Element el = (Element) nl.item(i);
					WpID = getIdentifier(el);
				}
			}

			this._WpID = WpID;
			
			
			
			/*
			 * Get the IGid
			 */
			nl = doc.getElementsByTagName("str:WorkProductProperties");
			if (nl != null && nl.getLength() > 0) {
				for (int i = 0; i < nl.getLength(); i++) {
					Element el = (Element) nl.item(i);
					NodeList nl2 = el
							.getElementsByTagName("base:AssociatedGroups");
					if (nl2 != null && nl2.getLength() > 0) {
						for (int ii = 0; ii < nl2.getLength(); ii++) {
							Element el2 = (Element) nl2.item(ii);
							IGid = getIdentifier(el2);
						}
					}
				}
			}

			
			this._IGid = IGid;
			
			end = System.currentTimeMillis();
			totalTime = end - start;
			System.out.println("Parse UICDS response took: " + totalTime + "ms");
			System.out.println("Got IgID: " + this._IGid);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
	}

	private String getIdentifier(Element el) {
		String id = getTextValue(el, "base:Identifier");
		return id;
	}

	public static Document loadXMLFromString(String xml) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		InputSource is = new InputSource(new StringReader(xml));
		return builder.parse(is);
	}

	/**
	 * I take a xml element and the tag name, look for the tag and get the text
	 * content i.e for <employee><name>John</name></employee> xml snippet if the
	 * Element points to employee node and tagName is 'name' I will return John
	 */
	private String getTextValue(Element ele, String tagName) {
		String textVal = null;
		NodeList nl = ele.getElementsByTagName(tagName);
		if (nl != null && nl.getLength() > 0) {
			Element el = (Element) nl.item(0);
			textVal = el.getFirstChild().getNodeValue();
		}

		return textVal;
	}

	/**
	 * Calls getTextValue and returns a int value
	 */
	private int getIntValue(Element ele, String tagName) {
		// in production application you would catch the exception
		return Integer.parseInt(getTextValue(ele, tagName));
	}

	// copy method from From E.R. Harold's book "Java I/O"
	public static void copy(InputStream in, OutputStream out)
			throws IOException {

		// do not allow other threads to read from the
		// input or write to the output while copying is
		// taking place

		synchronized (in) {
			synchronized (out) {

				byte[] buffer = new byte[256];
				while (true) {
					int bytesRead = in.read(buffer);
					if (bytesRead == -1)
						break;
					out.write(buffer, 0, bytesRead);
				}
			}
		}
	}

	
	
	public String convertStreamToString(InputStream stream) {
		try {
			StringWriter writer = new StringWriter();
			Charset encoding = Charset.defaultCharset();
			try {
				IOUtils.copy(stream, writer, encoding);
			} catch (Exception er) {
				System.out.println("ERROR: " + er);
			}
			return writer.toString();
		} catch (Exception e) {
			System.out.println("ERROR: " + e);
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	
	public boolean closeIncidents(String IGid) {
		String xml = convertStreamToString(new readXML().getCloseXML());	
		closeXML = xml.replace("{IGID}", IGid);	
		sendRequest(closeXML);

		
		String xml2 = convertStreamToString(new readXML().getArchiveXML());	
		archiveXML = xml2.replace("{IGID}", IGid);	
		sendRequest(archiveXML);
		return true;
		
	}
	
	public boolean closeSOI(String IGid) {
		String xml = convertStreamToString(new readXML().getCloseSOIXML());	
		xml = xml.replace("{IGID}", IGid);	
		long start = System.currentTimeMillis();
		String origURL = Global.uicdsURL;
		Global.uicdsURL = Global.uicdsURL.replace("IncidentManagementService", "SensorService");
		sendRequest(xml);
		Global.uicdsURL = origURL;
		long end = System.currentTimeMillis();
		long totalTime = end - start;
		System.out.println("Incident close took: " + totalTime + "ms");
		
	/*	xml = convertStreamToString(new readXML().getArchiveSOIXML());	
		xml = xml.replace("{IGID}", IGid);	
		start = System.currentTimeMillis();
		response = sendRequest(xml);	
		end = System.currentTimeMillis();
		totalTime = end - start;
		System.out.println("Incident archive took: " + totalTime + "ms");
		*/
		return true;
	}
	
	public void executeClose() {
		sendRequest(closeXML);
		sendRequest(archiveXML);
		archiveXML = "";
		closeXML = "";	
	}
	
	public  void closeIncident(String IGid) {
		String xml = convertStreamToString(new readXML().getCloseXML());	
		xml = xml.replace("{IGID}", IGid);	
		long start = System.currentTimeMillis();
		String response = sendRequest(xml);
		long end = System.currentTimeMillis();
		long totalTime = end - start;
		System.out.println("Incident close took: " + totalTime + "ms");
		/*
		 * Add Code here to make sure we were successful
		 */
		
		
		/*
		 * Archive the incident also
		 */
		archiveIncident(IGid);
	}
	
	public void archiveIncident(String IGid) {
		String xml = convertStreamToString(new readXML().getArchiveXML());	
		xml = xml.replace("{IGID}", IGid);	
		long start = System.currentTimeMillis();
		String response = sendRequest(xml);	
		long end = System.currentTimeMillis();
		long totalTime = end - start;
		System.out.println("Incident archive took: " + totalTime + "ms");
		/*
		 * Add Code here to make sure we were successful
		 */
	}
	
	public String getWpID() {
		return this._WpID;
	}

	public String getIGid() {
		return this._IGid;
	}

	public void setCCFD(CCFdata ccf) {
		this._ccf = ccf;
	}
	
}
