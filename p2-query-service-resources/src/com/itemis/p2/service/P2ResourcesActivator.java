package com.itemis.p2.service;

import java.net.URI;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.itemis.p2.service.internal.LoadRepositoryJob;
import com.itemis.p2.service.internal.RepositoryData;

import copied.com.ifedorenko.p2browser.model.IGroupedInstallableUnits;

public class P2ResourcesActivator extends Plugin {
	private IProvisioningAgent agent;
	private static P2ResourcesActivator plugin;

	private ServiceReference<IProvisioningAgent> agentReference;
	private RepositoryData repositoryData;

	public void start(BundleContext context) throws Exception {
		plugin = this;
		repositoryData = new RepositoryData();
	}

	public void stop() throws Exception {
		plugin = null;
		repositoryData = null;
		if (agentReference != null) {
			getBundle().getBundleContext().ungetService(agentReference);
		}
	}

	public static P2ResourcesActivator getDefault() {
		return plugin;
	}

	public synchronized IProvisioningAgent getProvisioningAgent() {
		if (agent == null) {
			BundleContext bundleContext = getBundle().getBundleContext();
			agentReference = bundleContext.getServiceReference(IProvisioningAgent.class);

			if (agentReference == null) {
				throw new IllegalStateException();
			}

			agent = bundleContext.getService(agentReference);

			if (agent == null) {
				throw new IllegalStateException();
			}
		}
		return agent;
	}

	public static IMetadataRepositoryManager getRepositoryManager() {
		IProvisioningAgent agent = getDefault().getProvisioningAgent();
		
		Object service = agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		IMetadataRepositoryManager repoMgr = (IMetadataRepositoryManager) service;

		if (repoMgr == null) {
			throw new IllegalStateException();
		}

		return repoMgr;
	}

	public RepositoryData getRepositoryData() {
		return repositoryData;
	}
	
	public void addRepository (URI uri) {
		LoadRepositoryJob job = new LoadRepositoryJob(uri, repositoryData);
		job.schedule();
	}
	
	public IGroupedInstallableUnits getRepositoryContents (URI uri) {
		if (!repositoryData.getRepositoryContent().containsKey(uri)) {
			addRepository(uri);
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
		return repositoryData.getRepositoryContent().get(uri);
	}
}
