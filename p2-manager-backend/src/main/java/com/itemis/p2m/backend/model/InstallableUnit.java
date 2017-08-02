package com.itemis.p2m.backend.model;

public class InstallableUnit {
	private String id;
	private String version;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public InstallableUnit() {
		
	}
	public InstallableUnit(String id, String version) {
		this.id = id;
		this.version = version;
	}
}
