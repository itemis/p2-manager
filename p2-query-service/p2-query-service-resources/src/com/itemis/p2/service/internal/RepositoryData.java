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
import static com.itemis.p2.service.internal.Log.*;

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
		return repositories.stream().filter(r -> r.getUri().equals(uri)).findFirst();
	}

	@Override
	public Optional<RepositoryInfo> getRepositoryById (int repositoryId) {
		return repositories.stream().filter(r -> r.getId()==repositoryId).findFirst();
	}
	
	@Override
	public RepositoryInfo addLocation (URI location, boolean loadOnDemand, boolean isChild) {
		RepositoryInfo repository = createRepositoryInfo(location);
		if(!repositories.contains(repository)){
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
	
	@Override
	public void loadLocation (URI location) {
		LoadRepositoryJob job = new LoadRepositoryJob(location, this);
		job.schedule();
		P2ResourcesActivator.getDefault().saveRepositoryData();
	}
	
	private synchronized RepositoryInfo createRepositoryInfo (URI uri) {
		if (idCounter < 0) {
			idCounter = repositories.stream()
					.map(r->r.getId())
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
					.map(r->r.getId())
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
	public IGroupedInstallableUnits getRepositoryContent(URI uri, boolean reload) {
		assureLoaded(uri, reload);
		return repositoryContent.get(uri);
	}
	
	/* (non-Javadoc)
	 * @see com.itemis.p2.service.internal.IRepositoryData#removeLocation(java.net.URI)
	 */
	@Override
	public void removeLocation (URI location) {
		dispose(location);
		Optional<RepositoryInfo> repository = getRepositoryByUri(location);
		if (repository.isPresent()) {
			repositories.remove(repository.get());
		}
		repositoryContent.remove(location);
		info("Repository "+location+": Removed.");
		P2ResourcesActivator.getDefault().saveRepositoryData();
	}
	
	@Override
	public void dispose(URI location) {
		info("Repository "+location+": Unloaded content.");
		repositoryContent.remove(location);
		allMetadataRepositories.remove(location);
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
	 * @see com.itemis.p2.service.internal.IRepositoryData#getRepository(java.net.URI)
	 */
	@Override
	public IMetadataRepository getRepository (URI location, boolean reload) {
		assureLoaded(location, reload);
		return allMetadataRepositories.get(location);
	}
	
	/* (non-Javadoc)
	 * @see com.itemis.p2.service.internal.IRepositoryData#addRepositoryContents(java.net.URI, copied.com.ifedorenko.p2browser.model.IGroupedInstallableUnits)
	 */
	@Override
	public void addRepositoryContents (URI location, IGroupedInstallableUnits content) {
		repositoryContent.put(location, content);
		info("Repository "+location+": Stored "+content.getRootIncludedInstallableUnits().size()+" IUs.");
	}


	private void assureLoaded(URI uri, boolean reload) {
		if (getRepository(uri)==null || reload) {
			LoadRepositoryJob job = new LoadRepositoryJob(uri, this);
			job.schedule();
		}
		for (Job job: Job.getJobManager().find(LoadRepositoryJob.FAMILY)) {
			LoadRepositoryJob loadRepositoryJob = (LoadRepositoryJob) job;
			if (loadRepositoryJob.getLocation().equals(uri)) {
				try {
					info("Repository "+uri+": Load job is currently in progress. Waiting...");
					loadRepositoryJob.join();
				} catch (InterruptedException e) {
					; // nothing
				}
			}
		}
	}

}
