package com.itemis.p2queryservice.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.ICompositeRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

import com.itemis.p2.service.IRepositoryData;
import com.itemis.p2.service.P2ResourcesActivator;
import com.itemis.p2.service.model.IUMasterInfo;
import com.itemis.p2.service.model.IUMetaInfo;
import com.itemis.p2.service.model.RepositoryInfo;

import copied.com.ifedorenko.p2browser.model.IGroupedInstallableUnits;

@Path("/repositories")
public class RepositoryService {
	public RepositoryService() {
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
			RepositoryInfo r = data.addLocation(uri, true, false);
			URI location = uriInfo.getRequestUriBuilder().path(r.getId() + "/").build();
			return Response.accepted().location(location).build(); // We have to use accepted, because the repository will be created asynch
					//status(Status.SEE_OTHER).location(location).build();//created(location).build();
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

		return Response.ok(repo.get()).build();
	}

	@DELETE
	@Path("{id}")
	public Response removeRepo(@PathParam("id") int repoId) {
		IRepositoryData data = getRepositoryData();
		Optional<RepositoryInfo> repo = data.getRepositoryById(repoId);
		if (!repo.isPresent()) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		data.removeLocation(repo.get().getUri());
		return Response.ok().build();
	}
	
	@GET
	@Path("{id}/children")
	public Response getChildRepositories (@PathParam("id") int repoId, @QueryParam("reload") boolean reload) {
		IRepositoryData data = getRepositoryData();
		Optional<RepositoryInfo> repo = data.getRepositoryById(repoId);
		if (!repo.isPresent()) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		
		IMetadataRepository repository = data.getRepository(repo.get().getUri(), reload); //getReository returns null
		List<RepositoryInfo> result = new ArrayList<>();
		if (repository instanceof ICompositeRepository<?>) {
			((ICompositeRepository<?>)repository).getChildren().forEach(childRepoUri -> {
				data.getRepositoryByUri(childRepoUri)
				.ifPresent(r -> {
					result.add(r);
				});
			});
		}
		
		return Response.ok(result).build();
	}

	@GET
	@Path("{id}/units")
//	@Produces("text/csv")
	public Response getUnits (@PathParam("id") int repoId, @QueryParam("reload") boolean reload) {
		IRepositoryData data = getRepositoryData();
		Optional<RepositoryInfo> repo = data.getRepositoryById(repoId);
		if (!repo.isPresent()) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		
		IGroupedInstallableUnits groupedIUs = data.getRepositoryContent(repo.get().getUri(), reload);
		
		List<IUMasterInfo> result = new ArrayList<>();
		if (groupedIUs != null) {
			groupedIUs.getRootIncludedInstallableUnits().forEach(unit -> result.add(new IUMasterInfo(unit)));
		}
		
		return Response.ok(result).type("text/csv").build();
	}

	@GET
	@Path("{id}/units/{unitname}")
	public Response getUnitMetadata (@PathParam("id") int repoId, @PathParam("unitname") String unitname) {
		if (unitname == null)
			return Response.status(Response.Status.NOT_FOUND).build();
		IRepositoryData data = getRepositoryData();
		Optional<RepositoryInfo> repo = data.getRepositoryById(repoId);
		if (!repo.isPresent()) {
			return Response.status(Response.Status.NOT_FOUND).entity("Repoitory not found").entity("Unit not found").build();
		}
		
		IGroupedInstallableUnits groupedIUs = data.getRepositoryContent(repo.get().getUri(), false);

		Optional<IInstallableUnit> iUnit = groupedIUs.getRootIncludedInstallableUnits().parallelStream().filter(unit -> unit.getId().equals(unitname)).findFirst();
		
		if(iUnit.isPresent()){
			return Response.ok(new IUMetaInfo(iUnit.get())).build();
		}
		else{
			return Response.status(Response.Status.NOT_FOUND).entity("Unit not found").build();
		}
	}
	
	@GET
	@Path("{id}/status")
	public Response getRepositoryStatus(@PathParam("id") int repoId) {
		IRepositoryData data = getRepositoryData();
		Optional<RepositoryInfo> repo = data.getRepositoryById(repoId);
		if (!repo.isPresent()) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		return Response.ok(repo.get().getStatus()).build();
	}
}