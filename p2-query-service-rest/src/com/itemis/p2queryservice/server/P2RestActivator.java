package com.itemis.p2queryservice.server;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

import com.google.common.io.CharStreams;
import com.itemis.p2.service.model.RepositoryInfo;

public class P2RestActivator extends Plugin {
	private static P2RestActivator instance;

	public static P2RestActivator getDefault() {
		return instance;
	}

	@Override
	public synchronized void start(BundleContext bundleContext) throws Exception {
		instance = this;
		info("START");
	}

	@Override
	public synchronized void stop(BundleContext bundleContext) throws Exception {
		info("STOP");
		instance = null;
	}

	public static CoreException createCoreException(String message, Throwable cause) {
		Status status = new Status(IStatus.ERROR, instance.getBundle().getSymbolicName(), message, cause);
		return new CoreException(status);
	}

	public static void info(String message) {
		Status status = new Status(IStatus.INFO, instance.getBundle().getSymbolicName(), message);
		instance.getLog().log(status);
	}

//	public List<RepositoryInfo> getRepositories() {
//		List<RepositoryInfo> repositories = new ArrayList<>(uris.size());
//		for (int i=0; i<uris.size(); i++) {
//			repositories.add(new RepositoryInfo(i, uris.get(i)));
//		}
//		return repositories;
//	}

}
