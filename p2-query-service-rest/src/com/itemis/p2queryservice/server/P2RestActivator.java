package com.itemis.p2queryservice.server;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.google.common.io.CharStreams;

public class P2RestActivator implements BundleActivator{

    private static final Logger logger = Logger.getLogger(P2RestActivator.class.getName());
    
    public static P2RestActivator activator;
    public BundleContext instance;
    
    //TODO: File is empty after restart
    private File txtFile;
    private FileReader reader;
    private FileWriter writer;
    
    List<String> uris;
    
    @Override
    public synchronized void start(BundleContext bundleContext) throws Exception {
    	logger.info("START");
    	this.instance = bundleContext;
    	activator = this;
    	txtFile = new File("P2RepoId.txt");
    	reader = new FileReader(txtFile);
    	writer = new FileWriter(txtFile);
    	uris = new ArrayList<>();
    	readUriFile();
    }
    
    @Override
    public synchronized void stop(BundleContext bundleContext) throws Exception {
    	logger.info("STOP");
    	uris.clear();
    	writer.close();
    	reader.close();
    	txtFile = null;
    	this.instance = null;
    	activator = null;
    }
    
    public static P2RestActivator getDefault(){
    	return activator;
    }
    
    private void readUriFile(){
    	uris.clear();
		try {
	    	uris.addAll(CharStreams.readLines(reader));
		} catch (IOException e) {
			// TODO ExceptionHandling
			e.printStackTrace();
		}
    }
    
    public int uriAlreadyExists(String uri){
    	return uris.indexOf(uri);
    }
    
    public int addUri(String uri){
    	uris.add(uri);
    	try {
			CharStreams.asWriter(writer).append(uri);
		} catch (IOException e) {
			// TODO ExceptionHandling
			e.printStackTrace();
		}
    	return uris.size()-1;
    }
    
    public String getUri(int index){
    	try{
    		return uris.get(index);
    	} catch (IndexOutOfBoundsException ioobe){
    		return null;
    	}
    }
}
