package com.itemis.p2m.backend.model;

import org.springframework.hateoas.ResourceSupport;

public class Repository extends ResourceSupport {

	private int repoId;
	
	private String uri;
	
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
}
