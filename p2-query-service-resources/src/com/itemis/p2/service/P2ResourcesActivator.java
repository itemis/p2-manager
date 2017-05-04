package com.itemis.p2.service;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.itemis.p2.service.internal.RepositoryData;

public class P2ResourcesActivator extends Plugin {
	public static final String ID = "com.itemis.p2.service"; //$NON-NLS-1$
	private static BundleContext bundleContext;

	private IProvisioningAgent agent;
	private static P2ResourcesActivator plugin;

	private ServiceReference<IProvisioningAgent> agentReference;
	private RepositoryData repositoryData;

	public static BundleContext getContext() {
		return bundleContext;
	}

	public void start(BundleContext context) throws Exception {
		bundleContext = context;
		plugin = this;
		repositoryData = new RepositoryData();
	}

	public void stop() throws Exception {
		bundleContext = null;
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
		
		String serviceName = IMetadataRepositoryManager.SERVICE_NAME;
		Object service = agent.getService(serviceName);
		IMetadataRepositoryManager repoMgr = (IMetadataRepositoryManager) service;

		if (repoMgr == null) {
			throw new IllegalStateException();
		}

		return repoMgr;
	}

	public RepositoryData getRepositoryData() {
		return repositoryData;
	}
}
