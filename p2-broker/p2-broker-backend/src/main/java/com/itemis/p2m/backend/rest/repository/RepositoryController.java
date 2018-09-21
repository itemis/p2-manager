package com.itemis.p2m.backend.rest.repository;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
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
import com.itemis.p2m.backend.Neo4JQueryBuilder;
import com.itemis.p2m.backend.QueryServiceHandler;
import com.itemis.p2m.backend.exceptions.NothingToLoadException;
import com.itemis.p2m.backend.model.InstallableUnit;
import com.itemis.p2m.backend.model.Repository;
import com.itemis.p2m.backend.services.ShoppingCartOptimizerService;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/repositories")
public class RepositoryController {
	@Value("${url.queryservice}")
	private String queryserviceUrl;	
	@Value("${url.neo4j.cypher}")
	private String neo4jUrl;	

	private RepositoryHandler repositoryHandler;
	private QueryServiceHandler queryServiceHandler;

	private ShoppingCartOptimizerService optimizer;

	private RestTemplate neoRestTemplate;	
	
	public RepositoryController(RepositoryHandler repositoryHandler, QueryServiceHandler handler, ShoppingCartOptimizerService optimizer, @Qualifier("neoRestTemplateBean") RestTemplate neoRestTemplate) {
		this.repositoryHandler = repositoryHandler;
		this.queryServiceHandler = handler;
		this.optimizer = optimizer;
		this.neoRestTemplate = neoRestTemplate;
	}

	@ApiOperation(value = "List all repositories")
	@RequestMapping(method=RequestMethod.GET)
	public List<Repository> listRepositories(@RequestParam(defaultValue = "false") boolean topLevelOnly,
											 @RequestParam(required = false) String[] searchTerm,
											 @RequestParam(required = false) String[] shoppingCart,
											 @RequestParam(defaultValue = "0") String limit,
											 @RequestParam(defaultValue = "0") String offset)  {
		


		if (shoppingCart != null) {
			List<InstallableUnit> unitsInCart = repositoryHandler.parseInstallableUnitsFromShoppingCart(shoppingCart);
			return optimizer.getRepositoryList(unitsInCart);
		}
		
		Neo4JQueryBuilder query = repositoryHandler.createListRepositoryQuery(limit, offset, searchTerm, topLevelOnly);
		
		ObjectNode response = neoRestTemplate.postForObject(neo4jUrl, query.buildMap(), ObjectNode.class);
		
		return repositoryHandler.extractRepositoryListFromDatabaseResponse(response);
	}
	
	@ApiOperation(value = "Add a new repository")
	@RequestMapping(method=RequestMethod.POST)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void addRepositoryWithCF(@RequestParam URI uri) throws URISyntaxException {
		Executor executor = Executors.newCachedThreadPool();
		URI queryLocation = new URI(queryserviceUrl);
		
		CompletableFuture<URI> createRepoQueryService = CompletableFuture.supplyAsync(() -> queryServiceHandler.postRepositoriesQueryService(uri, queryLocation), executor);

		CompletableFuture<Integer> createRepoNeo = createRepoQueryService.thenApplyAsync((repoQueryLocation) -> queryServiceHandler.postRepositoriesNeoDB(neo4jUrl, repoQueryLocation), executor);
		CompletableFuture<List<URI>> loadChildren = createRepoQueryService.thenApplyAsync((parentQueryLocation) -> queryServiceHandler.getChildrenQueryService(parentQueryLocation), executor);
		
		createRepoNeo.thenAcceptBothAsync(createRepoQueryService, (neoId, repoQueryLocation) -> queryServiceHandler.postUnitsNeoDB(neo4jUrl, neoId, repoQueryLocation), executor);
		loadChildren.thenAcceptBothAsync(createRepoNeo, (childQueryLocations, parentNeoId) -> queryServiceHandler.addChildrenRepositories(neo4jUrl, childQueryLocations, parentNeoId), executor);
		
		//TODO: return error if no repository can be found under the URL
	}
	
	@ApiOperation(value = "Get the uri of a repository")
	@RequestMapping(method=RequestMethod.GET, value="/{id}")
	public Repository getRepositoryURI(@PathVariable Integer id) {
		Neo4JQueryBuilder query = repositoryHandler.createGetRepositoryURIQuery(id);
		
		ObjectNode response = neoRestTemplate.postForObject(neo4jUrl, query.buildMap(), ObjectNode.class);

		return repositoryHandler.extractRepositoryURIFromDatabaseResponse(response);
	}

	@ApiOperation(value = "List all installable units available in the repository")
	@RequestMapping(method=RequestMethod.GET, value="/{id}/units")
	public List<InstallableUnit> listUnitsInRepository(@PathVariable Integer id) {
		Neo4JQueryBuilder query = repositoryHandler.createListUnitsInRepositoryQuery(id);
		
		ObjectNode _result = neoRestTemplate.postForObject(neo4jUrl, query.buildMap(), ObjectNode.class);

		return repositoryHandler.extractUnitListForRepositoryFromDatabaseResponse(_result);
	}

	@ApiOperation(value = "List all child repositories of this repository")
	@RequestMapping(method=RequestMethod.GET, value="/{id}/children")
	public List<Repository> listChildren(@PathVariable Integer id)  {
		Neo4JQueryBuilder query = repositoryHandler.createListChildrenQuery(id);

		ObjectNode response = neoRestTemplate.postForObject(neo4jUrl, query.buildMap(), ObjectNode.class);
		
		return repositoryHandler.extractChildRepositoriesFromDatabaseResponse(response);
	}
}
