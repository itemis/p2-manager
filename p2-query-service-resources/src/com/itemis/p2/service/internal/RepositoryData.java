package com.itemis.p2.service.internal;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

import copied.com.ifedorenko.p2browser.model.IGroupedInstallableUnits;

public class RepositoryData {
	/**
	 * Root repository URIs explicitly added by the user
	 */
	private final Set<URI> repositories = Collections.synchronizedSet(new LinkedHashSet<URI>());
	/**
	 * All repositories, including children of composite repositories
	 */
	private final Map<URI, IMetadataRepository> allrepositories = Collections
			.synchronizedMap(new LinkedHashMap<URI, IMetadataRepository>());

	private final Map<URI, IGroupedInstallableUnits> repositoryContent = Collections
			.synchronizedMap(new HashMap<URI, IGroupedInstallableUnits>());
	
	public Set<URI> getRepositories(){
		return repositories;
	}
	
	public Map<URI, IMetadataRepository> getAllrepositories(){
		return allrepositories;
	}
	
	public Map<URI, IGroupedInstallableUnits> getRepositoryContent(){
		return repositoryContent;
	}
	
	public void addLocation (URI location) {
		repositories.add(location);
	}
	
	public void removeLocation (URI location) {
		repositories.remove(location);
	}

	public boolean containsRepository (URI location) {
		return allrepositories.containsKey(location);
	}
	
	public void addRepository (URI location, IMetadataRepository repository) {
		allrepositories.put(location, repository);
	}
	
	public IMetadataRepository getRepository (URI location) {
		return allrepositories.get(location);
	}
	
	public void addRepositoryContents (URI location, IGroupedInstallableUnits content) {
		repositoryContent.put(location, content);
	}
}
