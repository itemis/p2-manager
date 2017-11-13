package com.itemis.p2m.backend.model;

import org.springframework.hateoas.ResourceSupport;

public class InstallableUnit extends ResourceSupport {
	
	private String unitId;
	private String version;
	public String getUnitId() {
		return unitId;
	}
	public void setUnitId(String unitId) {
		this.unitId = unitId;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public InstallableUnit() {
		
	}
	public InstallableUnit(String unitId, String version) {
		this.unitId = unitId;
		this.version = version;
	}
}
