package com.itemis.p2.service.model;

import org.eclipse.equinox.internal.p2.metadata.ProvidedCapability;

@SuppressWarnings("restriction")
public class IUProvidedInfo {

	private String name;
	private String nameSpace;
	private String version;
	
	public IUProvidedInfo(ProvidedCapability prov) {
		name = prov.getName();
		nameSpace = prov.getNamespace();
		version = prov.getVersion().toString();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNameSpace() {
		return nameSpace;
	}

	public void setNameSpace(String nameSpace) {
		this.nameSpace = nameSpace;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

}
