package com.itemis.p2.service.internal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

import com.itemis.p2.service.P2ResourcesActivator;

import copied.com.ifedorenko.p2browser.director.InstallableUnitDAG;
import copied.com.ifedorenko.p2browser.model.IncludedInstallableUnits;
import copied.com.ifedorenko.p2browser.model.InstallableUnitDependencyTree;
import copied.com.ifedorenko.p2browser.model.UngroupedInstallableUnits;
import copied.com.ifedorenko.p2browser.model.match.IInstallableUnitMatcher;

@SuppressWarnings("restriction")
public class LoadRepositoryJob extends Job {

	private URI location;
	private RepositoryData data;
    private boolean revealCompositeRepositories = true;
    private boolean groupIncludedIUs = false;
    private IInstallableUnitMatcher unitMatcher;


	public LoadRepositoryJob(URI location, RepositoryData data) {
		this (location, data, true, false);
	}
	public LoadRepositoryJob(URI location, RepositoryData data, boolean revealCompositeRepositories, boolean groupIncludedIUs) {
		super("Load repository metadata");
		this.location = location;
		setUser(true);
		this.data = data;
		this.revealCompositeRepositories = revealCompositeRepositories;
		this.groupIncludedIUs = groupIncludedIUs;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		List<IStatus> errors = new ArrayList<IStatus>();

		try {
			IMetadataRepositoryManager repoMgr = P2ResourcesActivator.getRepositoryManager();
			data.addLocation(location);
			loadRepository(repoMgr, location, errors, monitor);
			loadRepositoryContent(location, monitor);
		}
		catch (ProvisionException e) {
			errors.add(e.getStatus());
		}
		catch (OperationCanceledException e) {
			data.removeLocation(location);
			return Status.CANCEL_STATUS;
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return toStatus(errors);
	}
	
	private void loadRepository(IMetadataRepositoryManager repoMgr, URI location, List<IStatus> errors, IProgressMonitor monitor)
			throws ProvisionException, OperationCanceledException {
		if (!data.containsRepository(location)) {
			try {
				IMetadataRepository repository;
				repository = repoMgr.loadRepository(location, monitor);
				data.addRepository(location, repository);

				if (repository instanceof CompositeMetadataRepository) {
					for (URI childUri : ((CompositeMetadataRepository) repository).getChildren()) {
						// composite repository refresh refreshes all child
						// repositories. do not re-refresh children
						// here
						loadRepository(repoMgr, childUri, errors, monitor);
					}
				}
			} catch (ProvisionException e) {
				errors.add(e.getStatus());
			}
		}
	}

	private void loadRepositoryContent(URI location, IProgressMonitor monitor) {
		IMetadataRepository repository = data.getRepository(location);

		if (repository == null) {
			// repository failed to load for some reason
			return;
		}

		if (revealCompositeRepositories && repository instanceof CompositeMetadataRepository) {
			for (URI childUri : ((CompositeMetadataRepository) repository).getChildren()) {
				loadRepositoryContent(childUri, monitor);
			}
		} else {
			InstallableUnitDAG dag;
			if (groupIncludedIUs) {
				dag = new IncludedInstallableUnits().toInstallableUnitDAG(repository, monitor);
			} else {
				dag = new UngroupedInstallableUnits().toInstallableUnitDAG(repository, monitor);
			}

			if (unitMatcher != null) {
				dag = dag.filter(unitMatcher, groupIncludedIUs);
			}

			dag = dag.sort(new InstallableUnitComparator());

			data.addRepositoryContents(location, new InstallableUnitDependencyTree(dag));
		}
	}

	protected IStatus toStatus(List<IStatus> errors) {
		if (errors.isEmpty()) {
			return Status.OK_STATUS;
		} else if (errors.size() == 1) {
			return errors.get(0);
		} else {
			MultiStatus status = new MultiStatus(P2ResourcesActivator.ID, -1, errors.toArray(new IStatus[errors.size()]),
					"Problems loading repository", null);
			return status;
		}
	}
}
