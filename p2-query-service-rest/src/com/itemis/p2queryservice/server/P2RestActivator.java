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
import com.itemis.p2queryservice.model.RepositoryInfo;

public class P2RestActivator extends Plugin {
	private static final String REPO_DAT_FILE = "repositories.dat";

	private static P2RestActivator instance;

	private final List<URI> uris = new ArrayList<>();

	public static P2RestActivator getDefault() {
		return instance;
	}

	@Override
	public synchronized void start(BundleContext bundleContext) throws Exception {
		instance = this;
		info("START");
		readUriFile();
	}

	@Override
	public synchronized void stop(BundleContext bundleContext) throws Exception {
		info("STOP");
		uris.clear();
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

	private void readUriFile() throws CoreException {
		File repositoriesDataFile = getRepositoriesDataFile();
		if (!repositoriesDataFile.exists()) {
			try {
				repositoriesDataFile.createNewFile();
			} catch (IOException e) {
				throw createCoreException("Could not create " + REPO_DAT_FILE, e);
			}
		}
		try (FileReader reader = new FileReader(repositoriesDataFile)) {
			uris.clear();
			for (String line: CharStreams.readLines(reader)) {
				uris.add(URI.create(line));
			}
		} catch (IOException e) {
			throw createCoreException("Could not read " + REPO_DAT_FILE, e);
		}
	}

	public int addUri(String uri) throws CoreException {
		uris.add(URI.create(uri));
		try (FileWriter writer = new FileWriter(getRepositoriesDataFile(), true)) {
			CharStreams.asWriter(writer).append(uri);
		} catch (IOException e) {
			throw createCoreException("Could not read " + REPO_DAT_FILE, e);
		}
		return uris.size() - 1;
	}

	private File getRepositoriesDataFile() {
		return getStateLocation().append(REPO_DAT_FILE).toFile();
	}

	public int getRepositoryID (String uri) {
		return uris.indexOf(uri);
	}
	
	public List<URI> getUris() {
		return uris;
	}
	public List<RepositoryInfo> getRepositories() {
		List<RepositoryInfo> repositories = new ArrayList<>(uris.size());
		for (int i=0; i<uris.size(); i++) {
			repositories.add(new RepositoryInfo(i, uris.get(i)));
		}
		return repositories;
	}

	public URI getUri(int id) {
		return id >=0 && id < uris.size() ? uris.get(id) : null;
	}
}
