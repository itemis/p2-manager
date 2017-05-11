package com.itemis.p2queryservice.model;

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
	
}
