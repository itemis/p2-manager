package com.itemis.p2.service.model;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public class IUsMetaInfo{
	
	private RepositoryInfo repository;
	private IUMetaInfo installableUnit;

	public IUsMetaInfo(IInstallableUnit unit, RepositoryInfo repo) {
		this.repository = repo;
		this.installableUnit = new IUMetaInfo(unit);
	}

	public RepositoryInfo getRepository() {
		return repository;
	}

	public void setRepository(RepositoryInfo repository) {
		this.repository = repository;
	}

	public IUMetaInfo getInstallableUnit() {
		return installableUnit;
	}

	public void setInstallableUnit(IUMetaInfo installableUnit) {
		this.installableUnit = installableUnit;
	}

}
