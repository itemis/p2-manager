package com.itemis.p2queryservice.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.ICompositeRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

import com.itemis.p2.service.IRepositoryData;
import com.itemis.p2.service.P2ResourcesActivator;
import com.itemis.p2.service.model.IUMasterInfo;
import com.itemis.p2.service.model.IUMetaInfo;
import com.itemis.p2.service.model.RepositoryInfo;
import com.itemis.p2queryservice.rest.dto.RepositoryInfoDTO;

import copied.com.ifedorenko.p2browser.model.IGroupedInstallableUnits;

//TODO: look at Http Statuscodes of responses
@Path("/repositories")
public class RepositoryService {
	
	public RepositoryService() {
	}
	
	private IRepositoryData getRepositoryData() {
		return P2ResourcesActivator.getDefault().getRepositoryData();
	}
	
	@GET
	public Response getAllRepositories (@DefaultValue("false") @QueryParam("csv") boolean csv) {
		ResponseBuilder response = Response.ok(getRepositoryData().getAllRepositories());
		if (csv) {
			response = response.type("text/csv");
		}
		response = response.type(MediaType.APPLICATION_JSON);
		return response.build();
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
			return Response.accepted().location(location).build(); 
		} else {
			URI location = uriInfo.getRequestUriBuilder().path(repo.get().getId() + "/").build();
			return Response.status(Response.Status.CONFLICT).header(HttpHeaders.LOCATION, location)
					.entity("Repository already exists").build();
		}

	}

	@GET
	@Path("{id}")
	public Response getRepo(@PathParam("id") int repoId, @DefaultValue("false") @QueryParam("csv") boolean csv) {
		IRepositoryData data = getRepositoryData();
		Optional<RepositoryInfo> repo = data.getRepositoryById(repoId);
		if (!repo.isPresent()) {
			return Response.status(Response.Status.NOT_FOUND).build();
		} 
		RepositoryInfo repoInfo = repo.get();
		if (!repoInfo.isLoaded()) {
			if (!repoInfo.isLoading()) {
				repoInfo.startLoading();
				data.loadLocation(repoInfo.getUri());
			}
		}
		ResponseBuilder response = Response.ok(Collections.singletonList(new RepositoryInfoDTO(repoInfo)));
		if (csv) {
			response = response.type("text/csv");
		}
		else {
			response = response.type(MediaType.APPLICATION_JSON);
		}
		return response.build();
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
	public Response getChildRepositories (@PathParam("id") int repoId, @QueryParam("reload") boolean reload, @DefaultValue("false") @QueryParam("csv") boolean csv) {
		IRepositoryData data = getRepositoryData();
		Optional<RepositoryInfo> repo = data.getRepositoryById(repoId);
		if (!repo.isPresent()) {
			return Response.status(Response.Status.NOT_FOUND).build();
		} 
		RepositoryInfo repoInfo = repo.get();
		if (!repoInfo.areChildrenLoaded()) {
			if (!repoInfo.isLoading()) {
				repoInfo.startLoading();
				data.loadLocation(repoInfo.getUri());
			}
			return Response.noContent().build();
		}		
		IMetadataRepository repository = data.getRepository(repoInfo.getUri(), reload); //getReository returns null
		List<RepositoryInfo> result = new ArrayList<>();
		if (repository instanceof ICompositeRepository<?>) {
			List<URI> children = ((ICompositeRepository<?>)repository).getChildren();			
			children.forEach(childRepoUri -> {
				if(data.getRepositoryByUri(childRepoUri).isPresent())
					result.add(data.getRepositoryByUri(childRepoUri).get());
				else
					data.addLocation(childRepoUri, true, true);
			});
			if (children.size() != result.size())
				return Response.noContent().build();
		}
		ResponseBuilder response = Response.ok(result);
		if (csv) {
			response = response.type("text/csv");
		}
		else {
			response = response.type(MediaType.APPLICATION_JSON);
		}
		return response.build();
	}

	/**
	 * 
	 * @param repoId Repository id
	 * @param reload Forces a reload when <code>true</code>
	 * @param csv When <code>true</code> result will be in CSV format
	 * @return
	 */
	@GET
	@Path("{id}/units")
	public Response getUnits (@PathParam("id") int repoId, @QueryParam("reload") boolean reload, @DefaultValue("false") @QueryParam("csv") boolean csv) {
		IRepositoryData data = getRepositoryData();
		Optional<RepositoryInfo> repo = data.getRepositoryById(repoId);
		if (!repo.isPresent()) {
			return Response.status(Response.Status.NOT_FOUND).build();
		} 
		RepositoryInfo repoInfo = repo.get();
		if (!repoInfo.areUnitsLoaded()) {
			if (!repoInfo.isLoading()) {
				repoInfo.startLoading();
				data.loadLocation(repoInfo.getUri());
			}
			return Response.noContent().build();
		}			
		IGroupedInstallableUnits groupedIUs = data.getRepositoryContent(repoInfo.getUri(), reload);
		
		List<IUMasterInfo> result = new ArrayList<>();
		if (groupedIUs != null) {
			groupedIUs.getRootIncludedInstallableUnits().forEach(unit -> result.add(new IUMasterInfo(unit)));
		}
		
		ResponseBuilder response = Response.ok(result);
		if (csv) {
			response = response.type("text/csv");
		}
		else {
			response = response.type(MediaType.APPLICATION_JSON);
		}
		return response.build();
	}

	/**
	 * 
	 * @param repoId Repository id
	 * @param reload Forces a reload when <code>true</code>
	 * @return
	 */
	@GET
	@Path("{id}/units/count")
	public Response getNumberOfUnits (@PathParam("id") int repoId, @QueryParam("reload") boolean reload) {
		IRepositoryData data = getRepositoryData();
		Optional<RepositoryInfo> repo = data.getRepositoryById(repoId);
		if (!repo.isPresent()) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		RepositoryInfo repoInfo = repo.get();
		if (!repoInfo.areUnitsLoaded()) {
			if (!repoInfo.isLoading()) {
				repoInfo.startLoading();
				data.loadLocation(repoInfo.getUri());
			}
			return Response.noContent().build();
		}	
		
		IGroupedInstallableUnits groupedIUs = data.getRepositoryContent(repoInfo.getUri(), reload);
		
		List<IUMasterInfo> result = new ArrayList<>();
		if (groupedIUs != null) {
			groupedIUs.getRootIncludedInstallableUnits().forEach(unit -> result.add(new IUMasterInfo(unit)));
		}
		
		return Response.ok(result.size()).build();
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
		RepositoryInfo repoInfo = repo.get();
		if (!repoInfo.areUnitsLoaded()) {
			if (!repoInfo.isLoading()) {
				data.loadLocation(repoInfo.getUri());
				repoInfo.startLoading();
			}
			return Response.noContent().build();
		}	
		IGroupedInstallableUnits groupedIUs = data.getRepositoryContent(repoInfo.getUri(), false);

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