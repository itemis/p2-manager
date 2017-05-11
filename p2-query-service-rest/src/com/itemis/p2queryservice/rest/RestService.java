package com.itemis.p2queryservice.rest;

import java.net.URI;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.core.runtime.CoreException;

import com.itemis.p2.service.P2ResourcesActivator;
import com.itemis.p2.service.P2ResourcesFinder;
import com.itemis.p2queryservice.model.RepositoryInfo;
import com.itemis.p2queryservice.server.P2RestActivator;

import copied.com.ifedorenko.p2browser.model.IGroupedInstallableUnits;

@Path("/repositories")
public class RestService {
	public RestService() {
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Iterable<RepositoryInfo> getAllRepositories () {
		return P2RestActivator.getDefault().getRepositories();
	}

	@POST
	public Response addRepo(@Context UriInfo uriInfo, @FormParam("uri") String uri) {
		if (uri == null)
			return Response.status(Response.Status.NOT_FOUND).build();
		URI location;
		
		P2RestActivator activator = P2RestActivator.getDefault();
		int id = activator.getRepositoryID(uri);

		if (id < 0) {
			try {
				id = activator.addUri(uri);
				P2ResourcesActivator.getDefault().addRepository(URI.create(uri));
			} catch (CoreException e) {
				return Response.serverError().build();
			}
			location = uriInfo.getRequestUriBuilder().path(id + "/").build();
			return Response.created(location).build();
		} else {
			location = uriInfo.getRequestUriBuilder().path(id + "/").build();
			return Response.status(Response.Status.CONFLICT).header(HttpHeaders.LOCATION, location)
					.entity("Repository already exists").build();
		}
	}

	@GET
	@Path("{id}")
	public Response getRepo(@PathParam("id") String id) {
		int repoId = 0;
		try {
			repoId = Integer.parseInt(id);
		} catch (NumberFormatException nfe) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		URI repo = P2RestActivator.getDefault().getUri(repoId);
		if (repo == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		} 

		IGroupedInstallableUnits contents = P2ResourcesActivator.getDefault().getRepositoryContents(repo);
		if (contents == null) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		} else {
			return Response.ok(contents).build();
		}
	}
}