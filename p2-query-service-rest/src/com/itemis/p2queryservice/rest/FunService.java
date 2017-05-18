package com.itemis.p2queryservice.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

import com.itemis.p2.service.IRepositoryData;
import com.itemis.p2.service.P2ResourcesActivator;
import com.itemis.p2.service.model.IUsMetaInfo;
import com.itemis.p2.service.model.RepositoryInfo;

import copied.com.ifedorenko.p2browser.model.IGroupedInstallableUnits;

@Path("/units")
public class FunService {
	
	private IRepositoryData getRepositoryData() {
		return P2ResourcesActivator.getDefault().getRepositoryData();
	}

	@GET
	@Path("/{unitname}")
	public Response getUnitMetadata (@PathParam("unitname") String unitname) {
		if (unitname == null)
			return Response.status(Response.Status.NOT_FOUND).build();
		ExecutorService executorService = Executors.newCachedThreadPool();
		List<IUsMetaInfo> iUnits = new ArrayList<>();
		int[] ids = IntStream.rangeClosed(1, getRepositoryData().getIdCounter()).toArray();
		for (int id : ids){
			executorService.execute(new Runnable() {				
				@Override
				public void run() {
					IUsMetaInfo iUnit = getUnitMetadata(id, unitname);
					if (iUnit != null) iUnits.add(iUnit);
					System.out.println(id + " is ready");
				}
			});
		}
		executorService.shutdown();
		while(!executorService.isTerminated()){
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return Response.ok(iUnits).build();
	}
	
	private IUsMetaInfo getUnitMetadata (int repoId, String unitname) {
		IRepositoryData data = getRepositoryData();
		Optional<RepositoryInfo> repo = data.getRepositoryById(repoId);
		if (!repo.isPresent()) {
			return null;
		}
		
		IGroupedInstallableUnits groupedIUs = data.getRepositoryContent(repo.get().uri);
		if (groupedIUs == null)
			return null;
		
		Optional<IInstallableUnit> iUnit = groupedIUs.getRootIncludedInstallableUnits().parallelStream().filter(unit -> unit.getId().equals(unitname)).findFirst();
		
		if(iUnit.isPresent()){
			return new IUsMetaInfo(iUnit.get(), repo.get());
		}
		else{
			return null;
		}
	}
}
