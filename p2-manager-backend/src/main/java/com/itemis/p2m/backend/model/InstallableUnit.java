package com.itemis.p2m.backend.model;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.itemis.p2m.backend.InstallableUnitController;

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
	
	public InstallableUnit(ArrayNode unitData) {
		this.setUnitId(unitData.get(0).asText());
		this.setVersion(unitData.get(1).asText());
		
		// HATEOAS links
		this.add(linkTo(methodOn(InstallableUnitController.class).listRepositoriesForUnitVersion(unitId, version)).withRel("repositories"));
	}
}
