package com.itemis.p2m.backend.model;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.Objects;

import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.itemis.p2m.backend.controllers.RepositoryController;

public class Repository extends ResourceSupport {

	private int repoId;
	
	private String uri;
	
	public Repository(int repoId, String uri) {
		super();
		this.repoId = repoId;
		this.uri = uri;
	}
	
	public Repository(ArrayNode repoData) {
		this.setRepoId(repoData.get(0).asInt());
		this.setUri(repoData.get(1).asText());
		
		// HATEOAS links
		this.add(linkTo(methodOn(RepositoryController.class).getRepositoryURI(this.getRepoId())).withSelfRel());
		this.add(linkTo(methodOn(RepositoryController.class).listUnitsInRepository(this.getRepoId())).withRel("installableUnits"));
	}
	
	public Repository() {
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
	
	@Override
	public boolean equals(Object o) {
		if (o == null || o.getClass() != this.getClass())
			return false;
		
		Repository r = (Repository)o;
		
		if (repoId == r.repoId && uri.equals(r.uri)) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(repoId, uri);
	}
}
