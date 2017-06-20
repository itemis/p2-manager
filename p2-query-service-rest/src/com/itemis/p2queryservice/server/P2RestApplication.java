package com.itemis.p2queryservice.server;

import static com.itemis.p2queryservice.server.P2RestActivator.createCoreException;
import static com.itemis.p2queryservice.server.P2RestActivator.info;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

import com.eclipsesource.jaxrs.provider.gson.GsonProvider;
import com.google.common.base.Joiner;
import com.itemis.p2queryservice.rest.FunService;
import com.itemis.p2queryservice.rest.PingService;
import com.itemis.p2queryservice.rest.RepositoryService;

public class P2RestApplication implements IApplication {
	public static final String PORT = "http.port";
	private Server server;
	
	private static final Class<?>[] SERVICE_CLASSES= {
		PingService.class,
		RepositoryService.class,
		FunService.class,
		GsonProvider.class
	};

	@Override
	public Object start(IApplicationContext appContext) throws Exception {
		info("START p2-rest-service");

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setClassLoader(this.getClass().getClassLoader());
		context.setContextPath("/");
		
		server = new Server(getPort());
		
		server.setHandler(context);

		ServletHolder jerseyServlet = context.addServlet(ServletContainer.class, "/*");
		jerseyServlet.setInitOrder(0);
		jerseyServlet.setInitParameter("jersey.config.server.provider.classnames",
				Joiner.on(',').join(SERVICE_CLASSES));
		jerseyServlet.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");

		try {
			server.start();
			server.join();
		} catch (Exception e) {
			throw createCoreException("Could not start server", e);
		}
		run();
		return IApplication.EXIT_OK;
	}

	private Integer getPort() {
		String port = System.getProperty(PORT);
		if (port==null) {
			P2RestActivator.info("Using default port 8080. To change set system property 'http.port'.");
			return 8080;
		}
		return Integer.valueOf(port);
	}

	@Override
	public void stop() {
		info("STOP p2-rest-service");
		server.destroy();
	}

	public void run() {
		while (server.isRunning()) {
			try {
				this.wait(10);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	
}
