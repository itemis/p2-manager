package com.itemis.p2.service.model;

import com.itemis.p2queryservice.constants.RepositoryStatus;
import java.net.URI;

public class RepositoryInfo {
	private int id;
	private URI uri;
	private transient String status;
	private transient long modificationStamp = -1;
	
	public RepositoryInfo() {
		this.id = -1;
		this.status = RepositoryStatus.PENDING;
	}

	public RepositoryInfo(int id, URI uri) {
		super();
		this.id = id;
		this.uri = uri;
		status = RepositoryStatus.ADDED;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
		if (uri != null)
			status = RepositoryStatus.ADDED;
	}

	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
		if (id > 0)
			status = RepositoryStatus.ADDED;
	}
	
	public void addedToQueryService() {
		status = RepositoryStatus.ADDED;
	}
	
	public void removedFromCache() {
		status = RepositoryStatus.ADDED;
	}
	
	public void childrenAreLoaded() {
		if (status.equals(RepositoryStatus.LOADED)) {
			return;
		}
		else if (status.equals(RepositoryStatus.UNIT)) {
			status = RepositoryStatus.LOADED;
		}
		else {
			status = RepositoryStatus.CHILD;
		}
	}
	
	public void unitsAreLoaded() {
		if (status.equals(RepositoryStatus.LOADED)) {
			return;
		}
		else if (status.equals(RepositoryStatus.CHILD)) {
			status = RepositoryStatus.LOADED;
		}
		else {
			status = RepositoryStatus.UNIT;
		}
	}
	
	public String getStatus() {
		return this.status;
	}
	
	public void touch () {
		modificationStamp = System.currentTimeMillis();
	}
	
	public long getModificationStamp() {
		return modificationStamp;
	}
	
	public boolean isLoaded() {
		return this.status.equals(RepositoryStatus.LOADED);
	}
	
	public boolean areChildrenLoaded() {
		return this.status.equals(RepositoryStatus.CHILD) || isLoaded();
	}
	
	public boolean areUnitsLoaded() {
		return this.status.equals(RepositoryStatus.UNIT) || isLoaded();
	}
	
	public boolean isLoading() {
		return status.equals(RepositoryStatus.LOADING) || status.equals(RepositoryStatus.CHILD) || status.equals(RepositoryStatus.UNIT);
	}
	
	public void startLoading() {
		this.status = RepositoryStatus.LOADING;
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
