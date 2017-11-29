package com.itemis.p2m.backend.model;

import org.springframework.hateoas.ResourceSupport;

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
