package com.itemis.p2.service.internal;

import static com.itemis.p2.service.internal.Log.info;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.itemis.p2.service.IRepositoryData;
import com.itemis.p2.service.model.RepositoryInfo;

public class RepositoryCleanupJob extends Job {
	private boolean running = true;
	private IRepositoryData data;
	private long maxAge = 30000;

	public RepositoryCleanupJob(IRepositoryData data) {
		super("Cleanup");
		this.data = data;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		schedule(10000);
		
		List<RepositoryInfo> toRemove = data.getAllRepositories()
			.stream()
			.filter(r -> r.getModificationStamp()>0 && (System.currentTimeMillis() - r.getModificationStamp()) > maxAge)
			.collect(Collectors.toList());
		
		if (!toRemove.isEmpty()) {
			info ("Cleaning up");
			toRemove.forEach(r -> {
				data.dispose(r.getUri());
				r.removedFromCache();
			});
		}
		return Status.OK_STATUS;
	}

	@Override
	public boolean shouldSchedule() {
		return running;
	}

	@Override
	protected void canceling() {
		running = false;
	}

}
