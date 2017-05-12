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

import org.eclipse.equinox.p2.repository.ICompositeRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

import com.itemis.p2.service.IRepositoryData;
import com.itemis.p2.service.P2ResourcesActivator;
import com.itemis.p2.service.model.RepositoryInfo;

@Path("/repositories")
public class RestService {
	public RestService() {
	}
	
	private IRepositoryData getRepositoryData() {
		return P2ResourcesActivator.getDefault().getRepositoryData();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Iterable<RepositoryInfo> getAllRepositories () {
		return getRepositoryData().getAllRepositories();
	}

	@POST
	public Response addRepo(@Context UriInfo uriInfo, @FormParam("uri") URI uri) {
		if (uri == null)
			return Response.status(Response.Status.NOT_FOUND).build();
		IRepositoryData data = getRepositoryData();
		
		Optional<RepositoryInfo> repo = data.getRepositoryByUri(uri);

		if (!repo.isPresent()) {
			RepositoryInfo r = data.addLocation(uri);
			URI location = uriInfo.getRequestUriBuilder().path(r.id + "/").build();
			return Response.created(location).build();
		} else {
			URI location = uriInfo.getRequestUriBuilder().path(repo.get() + "/").build();
			return Response.status(Response.Status.CONFLICT).header(HttpHeaders.LOCATION, location)
					.entity("Repository already exists").build();
		}

	}

	@GET
	@Path("{id}")
	public Response getRepo(@PathParam("id") int repoId) {
		IRepositoryData data = getRepositoryData();
		Optional<RepositoryInfo> repo = data.getRepositoryById(repoId);
		if (!repo.isPresent()) {
			return Response.status(Response.Status.NOT_FOUND).build();
		} 

		IMetadataRepository repository = data.getRepository(repo.get().uri);
		return Response.ok(repository).build();
	}

	@GET
	@Path("{id}/children")
	public Response getChildRepositories (@PathParam("id") int repoId) {
		IRepositoryData data = getRepositoryData();
		Optional<RepositoryInfo> repo = data.getRepositoryById(repoId);
		if (!repo.isPresent()) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		
		IMetadataRepository repository = data.getRepository(repo.get().uri);
		List<RepositoryInfo> result = new ArrayList<>();
		if (repository instanceof ICompositeRepository<?>) {
			for (URI childRepoUri: ((ICompositeRepository<?>)repository).getChildren()) {
				data.getRepositoryByUri(childRepoUri)
					.ifPresent(r -> {
						result.add(r);
					});
			}
		}
		
		return Response.ok(result).build();
	}
}