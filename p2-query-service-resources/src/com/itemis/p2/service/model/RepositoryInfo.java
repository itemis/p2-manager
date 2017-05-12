package com.itemis.p2.service.model;

import java.net.URI;

public class RepositoryInfo {
	public int id;
	public URI uri;
	
	public RepositoryInfo() {}

	public RepositoryInfo(int id, URI uri) {
		super();
		this.id = id;
		this.uri = uri;
	}

	public int getId() {
		return id;
	}

	public URI getUri() {
		return uri;
	}

	@Override
	public String toString() {
		return "RepositoryInfo [id=" + id + ", uri=" + uri + "]";
	}
	
}
