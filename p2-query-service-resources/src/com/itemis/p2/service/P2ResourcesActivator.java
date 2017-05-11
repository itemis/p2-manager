package com.itemis.p2.service;

import java.io.File;
import java.net.URI;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.itemis.p2.service.internal.LoadRepositoryJob;
import com.itemis.p2.service.internal.RepositoryData;

import copied.com.ifedorenko.p2browser.model.IGroupedInstallableUnits;

public class P2ResourcesActivator extends Plugin {
	private IProvisioningAgent agent;
	private static P2ResourcesActivator instance;

	private ServiceReference<IProvisioningAgent> agentReference;
	private IRepositoryData repositoryData;

	public void start(BundleContext context) throws Exception {
		instance = this;
		File storage = getStateLocation().append("repositories.dat").toFile();
		repositoryData = new RepositoryData(storage);
	}

	public void stop() throws Exception {
		instance = null;
		repositoryData = null;
		if (agentReference != null) {
			getBundle().getBundleContext().ungetService(agentReference);
		}
	}

	public static P2ResourcesActivator getDefault() {
		return instance;
	}

	public static CoreException createCoreException(String message, Throwable cause) {
		Status status = new Status(IStatus.ERROR, instance.getBundle().getSymbolicName(), message, cause);
		return new CoreException(status);
	}

	public static void info(String message) {
		Status status = new Status(IStatus.INFO, instance.getBundle().getSymbolicName(), message);
		instance.getLog().log(status);
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

	public IRepositoryData getRepositoryData() {
		return repositoryData;
	}
	
}
