package com.itemis.p2queryservice.rest.dto;

import java.net.URI;

import com.itemis.p2.service.model.RepositoryInfo;

public class RepositoryInfoDTO {

	private int id;
	private URI uri;
	private String status;
	private long modificationStamp = -1;
	
	public RepositoryInfoDTO() {
		super();
	}
	
	public RepositoryInfoDTO(int id, URI uri, String status, long modificationStamp) {
		super();
		this.id = id;
		this.uri = uri;
		this.status = status;
		this.modificationStamp = modificationStamp;
	}
	
	public RepositoryInfoDTO(RepositoryInfo transfer) {
		super();
		this.id = transfer.getId();
		this.uri = transfer.getUri();
		this.status = transfer.getStatus();
		this.modificationStamp = transfer.getModificationStamp();
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

}
