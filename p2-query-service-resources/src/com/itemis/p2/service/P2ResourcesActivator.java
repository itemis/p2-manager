package com.itemis.p2.service;


import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.itemis.p2.service.internal.RepositoryData;

public class P2ResourcesActivator extends Plugin {
	private IProvisioningAgent agent;
	private static P2ResourcesActivator instance;

	private ServiceReference<IProvisioningAgent> agentReference;
	private IRepositoryData repositoryData;
	private Gson gson;

	public void start(BundleContext context) throws Exception {
		instance = this;
		gson = new GsonBuilder().setPrettyPrinting().create();
		
		repositoryData = readStorage();
		
	}

	public void stop() throws Exception {
		instance = null;
		gson = null;
		repositoryData = null;
		if (agentReference != null) {
			getBundle().getBundleContext().ungetService(agentReference);
		}
	}

	public static P2ResourcesActivator getDefault() {
		return instance;
	}


	private IRepositoryData readStorage() throws CoreException {
		File storage = getStateLocation().append("repositories.dat").toFile();
		if (storage.exists()) {
			try (FileReader reader = new FileReader(storage)) {
				return gson.fromJson(reader, RepositoryData.class);
			} catch (IOException e) {
				return new RepositoryData();
			}
		} else {
			return new RepositoryData();
		}
	}
	
	public void saveRepositoryData () {
		File storage = getStateLocation().append("repositories.dat").toFile();
		if (!storage.exists()) {
			try {
				storage.createNewFile();
			} catch (IOException e) {
				error("Could not create " + storage.getAbsolutePath(), e);
			}
		}
		try (FileWriter writer = new FileWriter(storage)) {
			gson.toJson(repositoryData, writer);
		} catch (IOException e) {
			throw new RuntimeException("Could not write " + storage.getAbsolutePath(), e);
		}
		
	}

	public static CoreException createCoreException(String message, Throwable cause) {
		Status status = new Status(IStatus.ERROR, instance.getBundle().getSymbolicName(), message, cause);
		return new CoreException(status);
	}

	public static void info(String message) {
		Status status = new Status(IStatus.INFO, instance.getBundle().getSymbolicName(), message);
		instance.getLog().log(status);
	}

	public static void error(String message, Exception cause) {
		Status status = new Status(IStatus.ERROR, instance.getBundle().getSymbolicName(), message, cause);
		instance.getLog().log(status);
	}

	public static Gson getGSON () {
		return instance.gson;
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
