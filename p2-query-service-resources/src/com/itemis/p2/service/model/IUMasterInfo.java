package com.itemis.p2.service.model;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public class IUMasterInfo {
	private String id;
	private String version;

	public IUMasterInfo (IInstallableUnit unit) {
		id = unit.getId();
		version = unit.getVersion().toString();
	}
}
