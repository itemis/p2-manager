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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itemis.p2m.backend.exceptions.NothingToLoadException;
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
	
	private Methods methods;

	private RestTemplate neoRestTemplate;	
	
	public RepositoryController(Methods methods, @Qualifier("neoRestTemplateBean") RestTemplate neoRestTemplate) {
		this.methods = methods;
		this.neoRestTemplate = neoRestTemplate;
	}

	@ApiOperation(value = "List all repositories")
	@RequestMapping(method=RequestMethod.GET)
	public List<Repository> listRepositories(@RequestParam(defaultValue = "false") boolean topLevelOnly,
											 @RequestParam(required = false) String[] searchTerm,
											 @RequestParam(defaultValue = "0") String limit,
											 @RequestParam(defaultValue = "0") String offset)  {
		
		Neo4JQueryBuilder query = new Neo4JQueryBuilder().match("(r:Repository)")
														 .result("r.serviceId,r.uri")
														 .distinct()
														 .orderBy("r.serviceId")
														 .limit(limit, offset);
		if (searchTerm != null) {
			for (String term : searchTerm) {
				query.filterContains("r.uri", term);
			}
		}
		
		if (topLevelOnly) {
			query.filter("size(()-[:PARENT_OF]->(r)) = 0");
		}

		ObjectNode _result = neoRestTemplate.postForObject(neo4jUrl, query.buildMap(), ObjectNode.class);
		ArrayNode dataNode = (ArrayNode) _result.get("data");
		
		List<Repository> result = new ArrayList<>();
		dataNode.forEach((d) -> result.add(methods.toRepository((ArrayNode) d)));
		
		if (result.size() == 0)
			throw new NothingToLoadException();
		
		return result;
	}
	
	@ApiOperation(value = "Add a new repository")
	@RequestMapping(method=RequestMethod.POST)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void addRepositoryWithCF(@RequestParam URI uri) throws URISyntaxException {
		Executor executor = Executors.newCachedThreadPool();
		URI queryLocation = new URI(queryserviceUrl);
		
		CompletableFuture<URI> createRepoQueryService = CompletableFuture.supplyAsync(() -> methods.postRepositoriesQueryService(uri, queryLocation), executor);

		CompletableFuture<Integer> createRepoNeo = createRepoQueryService.thenApplyAsync((repoQueryLocation) -> methods.postRepositoriesNeoDB(neo4jUrl, repoQueryLocation), executor);
		CompletableFuture<List<URI>> loadChildren = createRepoQueryService.thenApplyAsync((parentQueryLocation) -> methods.getChildrenQueryService(parentQueryLocation), executor);
		
		createRepoNeo.thenAcceptBothAsync(createRepoQueryService, (neoId, repoQueryLocation) -> methods.postUnitsNeoDB(neo4jUrl, neoId, repoQueryLocation), executor);
		loadChildren.thenAcceptBothAsync(createRepoNeo, (childQueryLocations, parentNeoId) -> methods.addChildrenRepositories(neo4jUrl, childQueryLocations, parentNeoId), executor);
	}
	
	@ApiOperation(value = "Get the uri of a repository")
	@RequestMapping(method=RequestMethod.GET, value="/{id}")
	public Repository getRepositoryURI(@PathVariable Integer id) {
		Neo4JQueryBuilder query = new Neo4JQueryBuilder().match("(r:Repository)")
														 .filter("r.serviceId = '"+id+"'")
														 .result("r.serviceId, r.uri")
														 .distinct();
		
		ObjectNode _result = neoRestTemplate.postForObject(neo4jUrl, query.buildMap(), ObjectNode.class);

		ArrayNode dataNode = (ArrayNode) _result.get("data");
		return methods.toRepository((ArrayNode)dataNode.get(0));
	}

	@ApiOperation(value = "List all installable units available in the repository")
	@RequestMapping(method=RequestMethod.GET, value="/{id}/units")
	public List<InstallableUnit> listUnitsInRepository(@PathVariable Integer id) {
		List<InstallableUnit> result = new ArrayList<>();
		Neo4JQueryBuilder query = new Neo4JQueryBuilder().match("(r:Repository)-[p:PROVIDES]->(iu:IU)")
				 										 .filter("r.serviceId = '"+id+"'")
				 										 .result("iu.serviceId,p.version")
				 										 .distinct();
		
		ObjectNode _result = neoRestTemplate.postForObject(neo4jUrl, query.buildMap(), ObjectNode.class);
				
		ArrayNode dataNode = (ArrayNode) _result.get("data");
		dataNode.forEach((d) -> result.add(methods.toUnit((ArrayNode)d)));
		return result;
	}

	@ApiOperation(value = "List all child repositories of this repository")
	@RequestMapping(method=RequestMethod.GET, value="/{id}/children")
	public List<Repository> listChildren(@PathVariable Integer id)  {
		Neo4JQueryBuilder query = new Neo4JQueryBuilder().match("(r:Repository) -[]-> (c:Repository)")
				 										 .filter("r.serviceId = '"+id+"'")
				 										 .result("c.serviceId,c.uri")
				 										 .orderBy("c.serviceId")
				 										 .distinct();

		ObjectNode _result = neoRestTemplate.postForObject(neo4jUrl, query.buildMap(), ObjectNode.class);
		ArrayNode dataNode = (ArrayNode) _result.get("data");
		
		List<Repository> result = new ArrayList<>();
		dataNode.forEach((d) -> result.add(methods.toRepository((ArrayNode) d)));
		
		if (result.size() == 0)
			throw new NothingToLoadException();
		
		return result;
	}
}
