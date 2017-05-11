package com.itemis.p2.service;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

import copied.com.ifedorenko.p2browser.model.IGroupedInstallableUnits;

public interface IRepositoryData {

	int getRepositoryID(URI uri);
	Optional<URI> getLocation (int repositoryId);

	Map<URI, IGroupedInstallableUnits> getRepositoryContent();

	void addLocation(URI location);

	void removeLocation(URI location);

	boolean containsRepository(URI location);

	void addRepository(URI location, IMetadataRepository repository);

	IMetadataRepository getRepository(URI location);

	void addRepositoryContents(URI location, IGroupedInstallableUnits content);

	List<URI> getAllLocations();

}