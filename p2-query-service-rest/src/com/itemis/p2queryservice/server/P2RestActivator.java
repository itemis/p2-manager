package com.itemis.p2queryservice.server;

import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class P2RestActivator implements BundleActivator{

    private static final Logger logger = Logger.getLogger(P2RestActivator.class.getName());
    
    public static P2RestActivator activator;
    public BundleContext instance;
    
    @Override
    public synchronized void start(BundleContext bundleContext) throws Exception {
    	logger.info("START");
    	this.instance = bundleContext;
    	activator = this;
    }
    
    @Override
    public synchronized void stop(BundleContext bundleContext) throws Exception {
    	logger.info("STOP");
    	this.instance = null;
    	activator = null;
    }
    
    public static P2RestActivator getDeafault(){
    	return activator;
    }
}
