package com.itemis.p2queryservice.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.core.runtime.CoreException;

import com.itemis.p2.service.P2ResourcesFinder;
import com.itemis.p2queryservice.server.P2RestActivator;

@Path("/p2/repository")
public class RestService {

	private static final Logger logger = Logger.getLogger(RestService.class.getName());

	public RestService() {
		logger.info("Construct TestRestService");
	}

	@GET
	@Produces("text/plain")
	@Path("/hello/world")
	public String getHelloWorld() {
		return "Hello World";
	}

	@POST
	// @Produces("text/plain")
	public Response addRepo(@Context UriInfo uriInfo, @FormParam("uri") String uri) {
		if (uri == null)
			return Response.status(Response.Status.NOT_FOUND).build();
		URI location;
		int index = P2RestActivator.getDefault().uriAlreadyExists(uri);
		if (index >= 0) {
			location = uriInfo.getRequestUriBuilder().path(index + "/").build();
			logger.info("IF: " + location.toString());
			return Response.status(Response.Status.CONFLICT).header(HttpHeaders.LOCATION, location)
					.entity("Repository already exists").build();
		} else {
			try {
				index = P2RestActivator.getDefault().addUri(uri);
			} catch (CoreException e) {
				return Response.serverError().build();
			}
			location = uriInfo.getRequestUriBuilder().path(index + "/").build();
			logger.info("ELSE: " + location.toString());
			return Response.created(location).build();
		}
	}

	@GET
	// @Produces("text/plain")
	@Path("{id}")
	public Response getRepo(@PathParam("id") String id) {
		int repoId = 0;
		try {
			repoId = Integer.parseInt(id);
		} catch (NumberFormatException nfe) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		String repo = P2RestActivator.getDefault().getUri(repoId);
		if (repo == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		} else {
			P2ResourcesFinder finder = new P2ResourcesFinder();
			try {
				String resource = finder.find(new URI(repo));
				if (resource == null) {
					return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
				} else {
					return Response.ok(resource).build();
				}
			} catch (URISyntaxException e) {
				e.printStackTrace();
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
			}
		}
	}
}