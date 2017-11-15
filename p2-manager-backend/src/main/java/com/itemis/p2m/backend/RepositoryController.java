package com.itemis.p2m.backend;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itemis.p2m.backend.constants.RepositoryStatus;
import com.itemis.p2m.backend.model.InstallableUnit;
import com.itemis.p2m.backend.model.Repository;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/repositories")
public class RepositoryController {
	@Value("${url.queryservice}")
	private String queryserviceUrl;	
	@Value("${url.neo4j.cypher}")
	private String neo4jUrl;	
	@Value("${neo4j.username}")
	private String neo4jUsername;
	@Value("${neo4j.password}")
	private String neo4jPassword;
	
	private Methods methods;
	
	public RepositoryController() {
		this.methods = new Methods();
	}

	@ApiOperation(value = "List all repositories")
	@RequestMapping(method=RequestMethod.GET)
	List<Repository> listRepositories() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(
				  new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		Map<String,Object> params = Collections.singletonMap("query", "MATCH (r:Repository) RETURN r.serviceId,r.uri");
		
		ObjectNode _result = restTemplate.postForObject(neo4jUrl, params, ObjectNode.class);
		ArrayNode dataNode = (ArrayNode) _result.get("data");
		
		List<Repository> result = new ArrayList<>();
		dataNode.forEach((d) -> result.add(methods.toRepository((ArrayNode) d)));
		return result;
	}
	
	/*@RequestMapping(method=RequestMethod.POST, value="/repositories")
	URI addRepository(@RequestParam URI uri) throws Exception {
		URI queryLocation = methods.postRepositoriesQueryService(uri, queryserviceUrl);
		int repoDBId = methods.postRepositoriesNeoDB(neo4jUsername, neo4jPassword, neo4jUrl, queryLocation);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		
		Future<List<URI>> waitForChildren = executor.submit(()->{
			while (methods.getRepositoryStatusQueryService(queryLocation, RepositoryStatus.CHILD) == null) {
				Thread.sleep(1000);
			}
			return methods.getChildrenQueryService(queryLocation);
		});
		waitForChildren.get().forEach(childLocation -> methods.addChildRepository(neo4jUsername, neo4jPassword, neo4jUrl, childLocation, repoDBId));
		
		Future<Boolean> waitForUnits = executor.submit(()->{
			while (methods.getRepositoryStatusQueryService(queryLocation, RepositoryStatus.UNIT) == null) {
				Thread.sleep(1000);
			}
			return (methods.getUnitsCountQueryService(queryLocation)>0);
		});
		if(waitForUnits.get()) methods.postUnitsNeoDB(neo4jUsername, neo4jPassword, neo4jUrl, repoDBId, queryLocation);
		
		return new URI("http://localhost"); //TODO: return statement
	}*/

	@ApiOperation(value = "Add a new repository")
	@RequestMapping(method=RequestMethod.POST)
	URI addRepositoryWithCF(@RequestParam URI uri) {
		Executor executor = Executors.newCachedThreadPool();
		
		CompletableFuture<URI> createRepoQueryService = CompletableFuture.supplyAsync(() -> methods.postRepositoriesQueryService(uri, queryserviceUrl), executor);
		
		CompletableFuture<Integer> createRepoNeo = createRepoQueryService.thenApplyAsync((queryLocation) -> methods.postRepositoriesNeoDB(neo4jUsername, neo4jPassword, neo4jUrl, queryLocation), executor);
		CompletableFuture<URI> iUsAreLoaded = createRepoQueryService.thenApplyAsync((queryLocation) -> methods.getRepositoryStatusQueryService(queryLocation, RepositoryStatus.UNIT), executor); //TODO: retry if not find maybe HandleAsync
		CompletableFuture<URI> childrenAreLoaded = createRepoQueryService.thenApplyAsync((queryLocation) -> methods.getRepositoryStatusQueryService(queryLocation, RepositoryStatus.CHILD), executor); //TODO: retry if not find maybe HandleAsync
		
		CompletableFuture<List<URI>> selectChildrenQueryService = childrenAreLoaded.thenApplyAsync((queryLocation) -> methods.getChildrenQueryService(queryserviceUrl+"/repositories", queryLocation.toString()), executor);
		
		iUsAreLoaded.thenAcceptBothAsync(createRepoNeo, (queryLocation, repoDBId) -> methods.postUnitsNeoDB(neo4jUsername, neo4jPassword, neo4jUrl, repoDBId, queryLocation), executor).isDone();
		selectChildrenQueryService.thenAcceptBothAsync(createRepoNeo, (childrenLocations, repoDBId) -> methods.addChildrenRepositories(neo4jUsername, neo4jPassword, neo4jUrl, childrenLocations, repoDBId), executor).isDone();
		
		try {
			return new URI("http://localhost");
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@ApiOperation(value = "Get the uri of a repository")
	@RequestMapping(method=RequestMethod.GET, value="/{id}")
	Repository getRepositoryURI(@PathVariable Integer id) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(
				  new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		Map<String,Object> params = Collections.singletonMap("query", "MATCH (r:Repository)WHERE r.serviceId = '"+id+"' RETURN r.serviceId, r.uri");
		
		ObjectNode _result = restTemplate.postForObject(neo4jUrl, params, ObjectNode.class);

		ArrayNode dataNode = (ArrayNode) _result.get("data");
		return methods.toRepository((ArrayNode)dataNode.get(0));
	}

	@ApiOperation(value = "List all installable units available in the repository")
	@RequestMapping(method=RequestMethod.GET, value="/{id}/units")
	List<InstallableUnit> listUnitsInRepository(@PathVariable Integer id) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(
				  new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		List<InstallableUnit> result = new ArrayList<>();
		Map<String,Object> params = Collections.singletonMap("query", "MATCH (r:Repository)-[p:PROVIDES]->(iu:IU) WHERE r.serviceId = '"+id+"' RETURN iu.serviceId,p.version");
		
		ObjectNode _result = restTemplate.postForObject(neo4jUrl, params, ObjectNode.class);
				
		ArrayNode dataNode = (ArrayNode) _result.get("data");
		dataNode.forEach((d) -> result.add(methods.toUnit((ArrayNode)d)));
		return result;
	}
}
