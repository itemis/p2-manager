package com.itemis.p2.service;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

import com.itemis.p2.service.model.RepositoryInfo;

import copied.com.ifedorenko.p2browser.model.IGroupedInstallableUnits;

public interface IRepositoryData {

	Optional<RepositoryInfo> getRepositoryByUri (URI uri);
	Optional<RepositoryInfo> getRepositoryById (int repositoryId);

	Map<URI, IGroupedInstallableUnits> getRepositoryContent();

	RepositoryInfo addLocation(URI location);

	void removeLocation(URI location);

	boolean containsRepository(URI location);

	void addRepository(URI location, IMetadataRepository repository);

	IMetadataRepository getRepository(URI location);

	void addRepositoryContents(URI location, IGroupedInstallableUnits content);

	List<RepositoryInfo> getAllRepositories();

}