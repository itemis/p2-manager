package com.itemis.p2queryservice.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

import com.itemis.p2.service.IRepositoryData;
import com.itemis.p2.service.P2ResourcesActivator;
import com.itemis.p2queryservice.model.RepositoryInfo;

@Path("/repositories")
public class RestService {
	public RestService() {
	}
	
	private IRepositoryData getRepositoryData() {
		return P2ResourcesActivator.getDefault().getRepositoryData();
	}
	
	private List<RepositoryInfo> toRepositoryInfos (Iterable<URI> uris) {
		IRepositoryData data = getRepositoryData();
		List<RepositoryInfo> result = new ArrayList<>();
		for (URI uri: uris) {
			int id = data.getRepositoryID(uri);
			result.add(new RepositoryInfo(id,uri));
		}
		return result;
	}
	

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Iterable<RepositoryInfo> getAllRepositories () {
		return toRepositoryInfos(getRepositoryData().getAllLocations());
	}

	@POST
	public Response addRepo(@Context UriInfo uriInfo, @FormParam("uri") URI uri) {
		if (uri == null)
			return Response.status(Response.Status.NOT_FOUND).build();
		IRepositoryData data = getRepositoryData();
		
		int id = data.getRepositoryID(uri);

		if (id < 0) {
			data.addLocation(uri);
			id = data.getRepositoryID(uri);
			URI location = uriInfo.getRequestUriBuilder().path(id + "/").build();
			return Response.created(location).build();
		} else {
			URI location = uriInfo.getRequestUriBuilder().path(id + "/").build();
			return Response.status(Response.Status.CONFLICT).header(HttpHeaders.LOCATION, location)
					.entity("Repository already exists").build();
		}
	}

	@GET
	@Path("{id}")
	public Response getRepo(@PathParam("id") int repoId) {
		IRepositoryData data = getRepositoryData();
		Optional<URI> uri = data.getLocation(repoId);
		if (!uri.isPresent()) {
			return Response.status(Response.Status.NOT_FOUND).build();
		} 

		IMetadataRepository repository = data.getRepository(uri.get());
		return Response.ok(repository).build();
	}

	@GET
	@Path("{id}/children")
	public Response getChildRepositories (@PathParam("id") int repoId) {
		IRepositoryData data = getRepositoryData();
		Optional<URI> uri = data.getLocation(repoId);
		if (!uri.isPresent()) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		
		IMetadataRepository repository = data.getRepository(uri.get());
		List<RepositoryInfo> result = new ArrayList<>();
		if (repository instanceof CompositeMetadataRepository) {
			for (URI childRepoUri: ((CompositeMetadataRepository)repository).getChildren()) {
				int id = data.getRepositoryID(childRepoUri);
				result.add(new RepositoryInfo(id, childRepoUri));
			}
		}
		
		return Response.ok(result).build();
	}
}