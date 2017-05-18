package com.itemis.p2.service.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.equinox.internal.p2.metadata.ProvidedCapability;
import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

@SuppressWarnings("restriction")
public class IUMetaInfo {
	
	private String id;
	private String version;
	private boolean singleton;
	private List<IURequirementsInfo> requirement;
	private List<IUProvidedInfo> provided;

	public IUMetaInfo (IInstallableUnit unit) {
		id = unit.getId();
		version = unit.getVersion().toString();
		singleton = unit.isSingleton();
		requirement = new ArrayList<>(unit.getRequirements().size());
		unit.getRequirements().forEach(req -> requirement.add(new IURequirementsInfo((RequiredCapability)req)));
		provided = new ArrayList<>(unit.getProvidedCapabilities().size());
		unit.getProvidedCapabilities().forEach(prov -> provided.add(new IUProvidedInfo((ProvidedCapability)prov)));
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

	public boolean isSingleton() {
		return singleton;
	}

	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	public List<IURequirementsInfo> getRequirement() {
		return requirement;
	}

	public void setRequirement(List<IURequirementsInfo> requirement) {
		this.requirement = requirement;
	}

	public List<IUProvidedInfo> getProvided() {
		return provided;
	}

	public void setProvided(List<IUProvidedInfo> provided) {
		this.provided = provided;
	}
}
