package com.itemis.p2queryservice.server;

import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class JettyActivator implements BundleActivator{

    private static final Logger logger = Logger.getLogger(JettyActivator.class.getName());
    
    @Override
    public synchronized void start(BundleContext bundleContext) throws Exception {
    	logger.info("START");
    }
    
    @Override
    public synchronized void stop(BundleContext bundleContext) throws Exception {
    	logger.info("STOP");
    }

}
