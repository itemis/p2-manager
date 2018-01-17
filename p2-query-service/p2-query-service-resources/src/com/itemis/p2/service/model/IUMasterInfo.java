package com.itemis.p2.service.model;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public class IUMasterInfo {
	private String id;
	private String version;

	public IUMasterInfo (IInstallableUnit unit) {
		id = unit.getId();
		version = unit.getVersion().toString();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
}
