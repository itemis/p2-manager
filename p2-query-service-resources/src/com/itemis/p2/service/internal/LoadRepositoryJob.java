package com.itemis.p2.service.internal;

import static com.itemis.p2.service.internal.Log.info;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

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

import com.itemis.p2.service.IRepositoryData;
import com.itemis.p2.service.P2ResourcesActivator;

import copied.com.ifedorenko.p2browser.director.InstallableUnitDAG;
import copied.com.ifedorenko.p2browser.model.IncludedInstallableUnits;
import copied.com.ifedorenko.p2browser.model.InstallableUnitDependencyTree;
import copied.com.ifedorenko.p2browser.model.UngroupedInstallableUnits;
import copied.com.ifedorenko.p2browser.model.match.IInstallableUnitMatcher;

@SuppressWarnings("restriction")
public class LoadRepositoryJob extends Job {
	public static final String FAMILY = "LoadRepositoryJob";

	private URI location;
	private IRepositoryData data;
	private boolean revealCompositeRepositories = true;
	private boolean groupIncludedIUs = false;
	private IInstallableUnitMatcher unitMatcher;

	public LoadRepositoryJob(URI location, IRepositoryData data) {
		this(location, data, true, false);
	}

	public LoadRepositoryJob(URI location, IRepositoryData data, boolean revealCompositeRepositories,
			boolean groupIncludedIUs) {
		super("Load repository metadata " + location);
		this.location = location;
		setUser(true);
		this.data = data;
		this.revealCompositeRepositories = revealCompositeRepositories;
		this.groupIncludedIUs = groupIncludedIUs;
	}

	@Override
	public boolean belongsTo(Object family) {
		return FAMILY.equals(family);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		List<IStatus> errors = new ArrayList<IStatus>();

		try {
			IMetadataRepositoryManager repoMgr = P2ResourcesActivator.getRepositoryManager();
			info("Repository " + location + ": Loading...");
			loadRepository(repoMgr, location, monitor);
			loadRepositoryContent(location, monitor);
			info("Repository " + location + ": Successfully loaded.");
		} catch (ProvisionException e) {
			errors.add(e.getStatus());
		} catch (OperationCanceledException e) {
			data.removeLocation(location);
			return Status.CANCEL_STATUS;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return toStatus(errors);
	}

	private void loadRepository(IMetadataRepositoryManager repoMgr, URI location, IProgressMonitor monitor)
			throws ProvisionException, OperationCanceledException {
		if (data.containsRepository(location)) {
			return;
		}
		IMetadataRepository repository = repoMgr.loadRepository(location, monitor);
		data.addRepository(location, repository);

		if (repository instanceof CompositeMetadataRepository) {
			CompositeMetadataRepository composite = (CompositeMetadataRepository) repository;
			info("Repository " + location + ": is a composite with " + composite.getChildren().size()
					+ " children:");
			for (URI childUri : composite.getChildren()) {
				// composite repository refresh refreshes all child
				// repositories. do not re-refresh children
				// here
				info ("   - "+childUri);
				data.addLocation(childUri, false, true);
				new LoadRepositoryJob(childUri, data, revealCompositeRepositories, groupIncludedIUs).schedule();
			}
		}
		data.getRepositoryByUri(location).ifPresent(repo -> repo.childrenAreLoaded());
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
		try {
			data.getRepositoryByUri(location).get().unitsAreLoaded();
		} catch (NoSuchElementException e) {
			// TODO: handle exception
		}
	}

	protected IStatus toStatus(List<IStatus> errors) {
		if (errors.isEmpty()) {
			return Status.OK_STATUS;
		} else if (errors.size() == 1) {
			return errors.get(0);
		} else {
			MultiStatus status = new MultiStatus(P2ResourcesActivator.getDefault().getBundle().getSymbolicName(), -1,
					errors.toArray(new IStatus[errors.size()]), "Problems loading repository", null);
			return status;
		}
	}

	public URI getLocation() {
		return location;
	}

}
