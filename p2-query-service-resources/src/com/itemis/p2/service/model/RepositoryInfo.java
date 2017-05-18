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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RepositoryInfo other = (RepositoryInfo) obj;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}
	
}
