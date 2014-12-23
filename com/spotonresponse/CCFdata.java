package com.spotonresponse;

public class CCFdata {
	private String datetime;
	private String location;
	private String service;
	private String address;
	private String code;
	private Double lat;
	private Double lon;
	private String uuid;
	private String igid;

	public String getDateTime() {
		return datetime;
	}

	public String getLocation() {
		return location;
	}

	public String getService() {
		return service;
	}

	public String getAddress() {
		return address;
	}

	public String getCode() {
		return code;
	}

	public Double getLat() {
		return lat;
	}

	public Double getLon() {
		return lon;
	}

	public String getUUID() {
		return uuid;
	}
	
	public String getIGID() {
		return igid;
	}

	public void setDateTime(String DateTime) {
		this.datetime = DateTime;
	}

	public void setLocation(String Location) {
		this.location = Location;
	}

	public void setService(String Service) {
		this.service = Service;
	}

	public void setAddress(String Address) {
		this.address = Address;
	}

	public void setCode(String Code) {
		this.code = Code;
	}

	public void setLat(Double Lat) {
		this.lat = Lat;
	}

	public void setLon(Double Lon) {
		this.lon = Lon;
	}

	public void setUUID(String UUID) {
		this.uuid = UUID;
	}

	public void setIGID(String IGID) {
		this.igid = IGID;
	}
}
