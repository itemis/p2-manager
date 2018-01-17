package com.itemis.p2queryservice.rest.dto;

import java.net.URI;

import com.itemis.p2.service.model.RepositoryInfo;

public class RepositoryInfoDTO {

	private int id;
	private URI uri;
	private String status;
	private long modificationStamp = -1;
	private boolean loading = false;
	
	public RepositoryInfoDTO() {
		super();
	}
	
	public RepositoryInfoDTO(int id, URI uri, String status, long modificationStamp, boolean loading) {
		super();
		this.id = id;
		this.uri = uri;
		this.status = status;
		this.modificationStamp = modificationStamp;
		this.loading = loading;
	}
	
	public RepositoryInfoDTO(RepositoryInfo transfer) {
		super();
		this.id = transfer.getId();
		this.uri = transfer.getUri();
		this.status = transfer.getStatus();
		this.modificationStamp = transfer.getModificationStamp();
		this.loading = transfer.isLoading();
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public long getModificationStamp() {
		return modificationStamp;
	}

	public void setModificationStamp(long modificationStamp) {
		this.modificationStamp = modificationStamp;
	}

	public boolean isLoading() {
		return loading;
	}

	public void setLoading(boolean loading) {
		this.loading = loading;
	}

}
