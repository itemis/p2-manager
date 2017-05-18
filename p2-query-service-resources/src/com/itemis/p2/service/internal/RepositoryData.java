package com.itemis.p2.service.internal;

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

import com.itemis.p2.service.IRepositoryData;
import com.itemis.p2.service.P2ResourcesActivator;
import com.itemis.p2.service.model.RepositoryInfo;

import copied.com.ifedorenko.p2browser.model.IGroupedInstallableUnits;

public class RepositoryData implements IRepositoryData {
	/**
	 * Root repository URIs explicitly added by the user
	 */
	private final List<RepositoryInfo> repositories = Collections.synchronizedList(new ArrayList<RepositoryInfo>());
	/**
	 * All repositories, including children of composite repositories
	 */
	private final transient Map<URI, IMetadataRepository> allMetadataRepositories = Collections
			.synchronizedMap(new LinkedHashMap<URI, IMetadataRepository>());

	private final transient Map<URI, IGroupedInstallableUnits> repositoryContent = Collections
			.synchronizedMap(new HashMap<URI, IGroupedInstallableUnits>());
	
	private transient int idCounter = -1;
	
	public RepositoryData () throws CoreException {
	}


	/* (non-Javadoc)
	 * @see com.itemis.p2.service.internal.IRepositoryData#getRepositoryID(java.net.URI)
	 */
	@Override
	public Optional<RepositoryInfo> getRepositoryByUri (URI uri) {
		return repositories.stream().filter(r -> r.uri.equals(uri)).findFirst();
	}

	@Override
	public Optional<RepositoryInfo> getRepositoryById (int repositoryId) {
		return repositories.stream().filter(r -> r.id==repositoryId).findFirst();
	}
	
	@Override
	public RepositoryInfo addLocation (URI location, boolean loadOnDemand, boolean isChild) {
		RepositoryInfo repository = createRepositoryInfo(location);
		// TODO: Filter child´s, which already exist if ()
		if(!isChild){
			isChild = true;
			repositories.add(repository);
		}
		else if(!repositories.contains(repository)){
			repositories.add(repository);
		}
		else
			idCounter--;
		if (loadOnDemand) {
			LoadRepositoryJob job = new LoadRepositoryJob(location, this);
			job.schedule();
		}
		P2ResourcesActivator.getDefault().saveRepositoryData();
		return repository;
	}
	
	private synchronized RepositoryInfo createRepositoryInfo (URI uri) {
		if (idCounter < 0) {
			idCounter = repositories.stream()
					.map(r->r.id)
					.max((id1,id2) -> id1-id2)
					.orElse(0);
		}
		idCounter++;
		return new RepositoryInfo(idCounter, uri);
	}

	@Override
	public synchronized int getIdCounter(){
		if (idCounter < 0) {
			idCounter = repositories.stream()
					.map(r->r.id)
					.max((id1,id2) -> id1-id2)
					.orElse(0);
		}
		return idCounter;
	}

	/* (non-Javadoc)
	 * @see com.itemis.p2.service.internal.IRepositoryData#getUris()
	 */
	@Override
	public List<RepositoryInfo> getAllRepositories() {
		return repositories;
	}
	
	/* (non-Javadoc)
	 * @see com.itemis.p2.service.internal.IRepositoryData#getRepositoryContent()
	 */
	@Override
	public IGroupedInstallableUnits getRepositoryContent(URI uri){
		assureLoaded(uri);
		return repositoryContent.get(uri);
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
