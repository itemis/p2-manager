package com.itemis.p2.service.internal;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

public class Log {
	public static Bundle bundle;
	
	public static void info(String message) {
		log(IStatus.INFO, message, null);
	}

	public static void error(String message, Exception cause) {
		log(IStatus.ERROR, message, cause);
	}

	public static void log (int severity, String message, Throwable exception) {
		Assert.isNotNull(bundle);
		Status status = new Status(severity, bundle.getSymbolicName(), message, exception);
		Platform.getLog(bundle).log(status);
	}
	
}
