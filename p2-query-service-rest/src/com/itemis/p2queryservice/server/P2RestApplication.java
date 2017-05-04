package com.itemis.p2queryservice.server;

import java.util.logging.Logger;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

import com.itemis.p2queryservice.rest.RestService;

public class P2RestApplication implements IApplication{
	
    private static final Logger logger = Logger.getLogger(P2RestApplication.class.getName());
    
    private boolean runner = true;
    private static P2RestApplication application;
    
    Server s;
    
    @Override
	public Object start(IApplicationContext appContext) throws Exception {
    	logger.info("START");
    	application = this;
    	
    	ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    	context.setClassLoader(this.getClass().getClassLoader());
    	context.setContextPath("/");

    	s = new Server(8080);
    	s.setHandler(context);
    	
		ServletHolder jerseyServlet = context.addServlet(ServletContainer.class, "/*");
		jerseyServlet.setInitOrder(0);
		jerseyServlet.setInitParameter("jersey.config.server.provider.classnames", RestService.class.getCanonicalName());
		try {
			s.start();
			s.join();
			return run(null);
		} catch (Exception e1) {
			e1.printStackTrace();
			return null;
		}
	}

	@Override
	public void stop() {
    	logger.info("STOP");
		s.destroy();
		application = null;
	}
	
	public Object run(Object o){
		while(runner){ //TODO: Stop?
			try{
				this.wait(10);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		return EXIT_OK;
	}
	
	public void stopRun(){
		this.runner = false;
		this.notify();
	}
	
	public static P2RestApplication getDefault(){
		return application;
	}
}
