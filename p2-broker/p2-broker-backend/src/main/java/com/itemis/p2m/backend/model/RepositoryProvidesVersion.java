package com.itemis.p2m.backend.model;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.itemis.p2m.backend.controllers.RepositoryController;

public class RepositoryProvidesVersion extends ResourceSupport {

	private int repoId;
	
	private String uri;
	
	private String version;
	
	public RepositoryProvidesVersion(int repoId, String uri, String version) {
		super();
		this.repoId = repoId;
		this.uri = uri;
		this.version = version;
	}
	
	public RepositoryProvidesVersion(ArrayNode repoData) {
		this.setRepoId(repoData.get(0).asInt());
		this.setUri(repoData.get(1).asText());
		this.setVersion(repoData.get(2).asText());
		
		// HATEOAS links
		this.add(linkTo(methodOn(RepositoryController.class).getRepositoryURI(this.getRepoId())).withSelfRel());
		this.add(linkTo(methodOn(RepositoryController.class).listUnitsInRepository(this.getRepoId())).withRel("installableUnits"));
	}
	
	public RepositoryProvidesVersion() {
		super();
	}

	public int getRepoId() {
		return repoId;
	}
	
	public void setRepoId(int unitId) {
		this.repoId = unitId;
	}
	
	public String getUri() {
		return uri;
	}
	
	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
}
