package com.itemis.p2.service.internal;

import static com.itemis.p2.service.P2ResourcesActivator.createCoreException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

import com.google.common.io.CharStreams;
import com.itemis.p2.service.IRepositoryData;

import copied.com.ifedorenko.p2browser.model.IGroupedInstallableUnits;

public class RepositoryData implements IRepositoryData {
	/**
	 * Root repository URIs explicitly added by the user
	 */
	private final List<URI> repositories = Collections.synchronizedList(new ArrayList<URI>());
	/**
	 * All repositories, including children of composite repositories
	 */
	private final Map<URI, IMetadataRepository> allMetadataRepositories = Collections
			.synchronizedMap(new LinkedHashMap<URI, IMetadataRepository>());

	private final Map<URI, IGroupedInstallableUnits> repositoryContent = Collections
			.synchronizedMap(new HashMap<URI, IGroupedInstallableUnits>());
	
	private final File storage;
	
	public RepositoryData (File stateFile) throws CoreException {
		this.storage = stateFile;
		readUriFile();
	}

	private void readUriFile() throws CoreException {
		if (!storage.exists()) {
			try {
				storage.createNewFile();
			} catch (IOException e) {
				throw createCoreException("Could not create " + storage.getAbsolutePath(), e);
			}
		}
		try (FileReader reader = new FileReader(storage)) {
			repositories.clear();
			for (String line: CharStreams.readLines(reader)) {
				repositories.add(URI.create(line));
			}
		} catch (IOException e) {
			throw createCoreException("Could not read " + storage.getAbsolutePath(), e);
		}
	}

	/* (non-Javadoc)
	 * @see com.itemis.p2.service.internal.IRepositoryData#getRepositoryID(java.net.URI)
	 */
	@Override
	public int getRepositoryID (URI uri) {
		return repositories.indexOf(uri);
	}

	@Override
	public Optional<URI> getLocation(int repositoryId) {
		if (repositoryId<0 || repositoryId>=repositories.size()) {
			return Optional.empty();
		}
		return Optional.of(repositories.get(repositoryId));
	}
	
	/* (non-Javadoc)
	 * @see com.itemis.p2.service.internal.IRepositoryData#getUris()
	 */
	@Override
	public List<URI> getAllLocations() {
		return repositories;
	}
	
	/* (non-Javadoc)
	 * @see com.itemis.p2.service.internal.IRepositoryData#getRepositoryContent()
	 */
	@Override
	public Map<URI, IGroupedInstallableUnits> getRepositoryContent(){
		return repositoryContent;
	}
	
	/* (non-Javadoc)
	 * @see com.itemis.p2.service.internal.IRepositoryData#addLocation(java.net.URI)
	 */
	@Override
	public void addLocation (URI location) {
		repositories.add(location);
		try (FileWriter writer = new FileWriter(storage, true)) {
			CharStreams.asWriter(writer).append("\n"+location);
		} catch (IOException e) {
			throw new RuntimeException("Could not read " + storage.getAbsolutePath(), e);
		}
		assureLoaded(location);
	}
	
	/* (non-Javadoc)
	 * @see com.itemis.p2.service.internal.IRepositoryData#removeLocation(java.net.URI)
	 */
	@Override
	public void removeLocation (URI location) {
		repositories.remove(location);
	}

	/* (non-Javadoc)
	 * @see com.itemis.p2.service.internal.IRepositoryData#containsRepository(java.net.URI)
	 */
	@Override
	public boolean containsRepository (URI location) {
		return allMetadataRepositories.containsKey(location);
	}
	
	/* (non-Javadoc)
	 * @see com.itemis.p2.service.internal.IRepositoryData#addRepository(java.net.URI, org.eclipse.equinox.p2.repository.metadata.IMetadataRepository)
	 */
	@Override
	public void addRepository (URI location, IMetadataRepository repository) {
		allMetadataRepositories.put(location, repository);
	}
	
	/* (non-Javadoc)
	 * @see com.itemis.p2.service.internal.IRepositoryData#getRepository(java.net.URI)
	 */
	@Override
	public IMetadataRepository getRepository (URI location) {
		return allMetadataRepositories.get(location);
	}
	
	/* (non-Javadoc)
	 * @see com.itemis.p2.service.internal.IRepositoryData#addRepositoryContents(java.net.URI, copied.com.ifedorenko.p2browser.model.IGroupedInstallableUnits)
	 */
	@Override
	public void addRepositoryContents (URI location, IGroupedInstallableUnits content) {
		repositoryContent.put(location, content);
	}


	private void assureLoaded(URI uri) {
		if (getRepository(uri)==null) {
			LoadRepositoryJob job = new LoadRepositoryJob(uri, this);
			job.schedule();
		}
		for (Job job: Job.getJobManager().find(LoadRepositoryJob.FAMILY)) {
			LoadRepositoryJob loadRepositoryJob = (LoadRepositoryJob) job;
			if (loadRepositoryJob.getLocation().equals(uri)) {
				try {
					loadRepositoryJob.join();
				} catch (InterruptedException e) {
					; // nothing
				}
			}
		}
	}

}
