package com.itemis.p2m.backend;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.itemis.p2m.backend.constants.RepositoryStatus;
import com.itemis.p2m.backend.model.InstallableUnit;
import com.itemis.p2m.backend.model.Repository;

@Component
public class Methods {
	
	private RestTemplate queryServiceRestTemplate;
	
	private RestTemplate neoRestTemplate;
	
	public Methods(@Qualifier("queryServiceRestTemplateBean") RestTemplate queryServiceRestTemplate,
								@Qualifier("neoRestTemplateBean") RestTemplate neoRestTemplate) {
		this.queryServiceRestTemplate = queryServiceRestTemplate;
		this.neoRestTemplate = neoRestTemplate;
	}
	
	/**
	 * Adds a repository to the p2 query service.
	 * 
	 * @param repositoryUri The URI of the repository to be added.
	 * @param queryLocation The URL under which the p2 query service can be reached.
	 * @return The URI under which the repository has been added by the p2 query service.
	 * */
	public URI postRepositoriesQueryService(URI repositoryUri, URI queryLocation) {
		MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
		formParams.add("uri", repositoryUri.toString());
		URI location = null;
		try {
			location = queryServiceRestTemplate.postForLocation(queryLocation+"/repositories", formParams);
		} catch (HttpClientErrorException hcee) {
			if (hcee.getRawStatusCode() == 409) {
				location = hcee.getResponseHeaders().getLocation();
			} else {
				throw hcee;
			}
		}
		return location;
	}
	
	/**
	 * Adds a repository to the neo4j database.
	 * 
	 * @param neo4jUrl The URL under which the neo4j database can be reached.
	 * @param repoQueryLocation The {@link URI} of the repository under the p2 query service.
	 * @return The id assigned to the repository by the neo4j database.
	 */
	public int postRepositoriesNeoDB(String neo4jUrl, URI repoQueryLocation) {
		StringBuilder queryBuilder = new StringBuilder("LOAD CSV WITH HEADERS FROM '").append(repoQueryLocation.toString()).append("?csv=true' AS line ");
		queryBuilder.append("MERGE (r:Repository {serviceId : line.id, uri : line.uri}) RETURN ID(r)");
		Map<String,Object> body = Collections.singletonMap("query", queryBuilder.toString());
		
		ObjectNode jsonResult = neoRestTemplate.postForObject(neo4jUrl, body, ObjectNode.class);
		ArrayNode dataNode = (ArrayNode) jsonResult.get("data");
		return dataNode.get(0).get(0).asInt();
	}
	
	/**
	 * Adds a repository to the neo4j database as child of an existing repository.
	 * 
	 * @param neo4jUrl The URL under which the neo4j database can be reached.
	 * @param repoQueryLocation The {@link URI} of the repository under the p2 query service.
	 * @param parentNeoId The id of the parent of the repository that is to be added.
	 * @return The id assigned to the repository by the neo4j database.
	 */
	public int postRepositoriesNeoDB(String neo4jUrl, URI repoQueryLocation, int parentNeoId) {
		StringBuilder queryBuilder = new StringBuilder("LOAD CSV WITH HEADERS FROM '").append(repoQueryLocation.toString()).append("?csv=true' AS line ");
		queryBuilder.append("MATCH (p:Repository) WHERE ID(p)=").append(parentNeoId).append(" ");
		queryBuilder.append("MERGE (r:Repository {serviceId : line.id, uri : line.uri}) ");
		queryBuilder.append("MERGE (p)-[po:PARENT_OF]->(r) RETURN ID(r)");
		Map<String,Object> body = Collections.singletonMap("query", queryBuilder.toString());
		
		ObjectNode jsonResult = neoRestTemplate.postForObject(neo4jUrl, body, ObjectNode.class);
		ArrayNode dataNode = (ArrayNode) jsonResult.get("data");
		
		return dataNode.get(0).get(0).asInt();
	}

	/**
	 * Adds the installable units of a repository to the neo4j database.
	 * 
	 * @param neo4jUrl The URL under which the neo4j database can be reached.
	 * @param neoId The id of the repository in the neo4j database.
	 * @param repoQueryLocation The {@link URI} of the repository under the p2 query service.
	 */
	//TODO: Very slow!!!!!
	public void postUnitsNeoDB(String neo4jUrl, int neoId, URI repoQueryLocation){
		if (getUnitCountQueryService(repoQueryLocation) == 0) {
			return;
		}
		StringBuilder queryBuilder = new StringBuilder("LOAD CSV WITH HEADERS FROM '").append(repoQueryLocation.toString()).append("/units?csv=true' AS line ");
		queryBuilder.append("MATCH (r:Repository) WHERE ID(r)=").append(neoId).append(" ");
		queryBuilder.append("MERGE (iu:IU { serviceId: line.id}) ");
		queryBuilder.append("MERGE (r)-[p:PROVIDES { version: line.version}]->(iu)");
		System.out.println("Start to fill Neo with Units");
		Map<String,Object> body = Collections.singletonMap("query", queryBuilder.toString());
		ObjectNode jsonResult = neoRestTemplate.postForObject(neo4jUrl, body, ObjectNode.class);
		
	}
	//TODO: Async
	private void addChildRepository(String neo4jUrl, URI childQueryLocation, int parentNeoId) {
		Date start = new Date();
		System.out.println("Child " + childQueryLocation.toString() + " started");
		int neoId = postRepositoriesNeoDB(neo4jUrl, childQueryLocation, parentNeoId);
		System.out.println("Child " + childQueryLocation.toString() + " created in neoDB in " + ((new Date().getTime()-start.getTime())/1000) + " seconds");
		postUnitsNeoDB(neo4jUrl, neoId, childQueryLocation);
		System.out.println("Child " + childQueryLocation.toString() + " units are created in neoDB " + ((new Date().getTime()-start.getTime())/1000) + " seconds");
		addChildrenRepositories(neo4jUrl, getChildrenQueryService(childQueryLocation), neoId);
		System.out.println("Child " + childQueryLocation.toString() + " children are created in neoDB " + ((new Date().getTime()-start.getTime())/1000) + " seconds");
		
	}
	//TODO: Async
	private void addChildRepositoryWithFuture(String neo4jUrl, URI childQueryLocation, int parentNeoId, ExecutorService executor) {
		Date start = new Date();
		System.out.println("Child " + childQueryLocation.toString() + " started");
		
		/* 
		 * Without Debug prints
		 * CompletableFuture<Integer> createRepoNeo = CompletableFuture.supplyAsync(() -> postRepositoriesNeoDB(neo4jUrl, childQueryLocation, parentNeoId), executor);
		 * CompletableFuture<List<URI>> loadChildren = CompletableFuture.supplyAsync(() -> getChildrenQueryService(childQueryLocation), executor);
		 * 
		 * CompletableFuture<Void> createUnitsNeo = createRepoNeo.thenAcceptAsync((neoId) -> postUnitsNeoDB(neo4jUrl, neoId, childQueryLocation), executor);
		 * CompletableFuture<Void> createChildren = loadChildren.thenAcceptBothAsync(createRepoNeo, (childQueryLocations, neoId) -> addChildrenRepositories(neo4jUrl, childQueryLocations, neoId), executor);
		*/
		CompletableFuture<Integer> createRepoNeo = CompletableFuture.supplyAsync(() -> {
			int neoId = postRepositoriesNeoDB(neo4jUrl, childQueryLocation, parentNeoId);
			System.out.println("Child " + childQueryLocation.toString() + " created in neoDB in " + ((new Date().getTime()-start.getTime())/1000) + " seconds");
			return neoId;
		}, executor);
		CompletableFuture<List<URI>> loadChildren = CompletableFuture.supplyAsync(() -> {
			List<URI> children = getChildrenQueryService(childQueryLocation);
			System.out.println("Childs of " + childQueryLocation.toString() + " are loaded from QueryService in " + ((new Date().getTime()-start.getTime())/1000) + " seconds");
			return children;
		}, executor);
		
		CompletableFuture<Void> createUnitsNeo = createRepoNeo.thenAcceptAsync((neoId) -> {
			postUnitsNeoDB(neo4jUrl, neoId, childQueryLocation);
			System.out.println("Child " + childQueryLocation.toString() + " units are created in neoDB " + ((new Date().getTime()-start.getTime())/1000) + " seconds");
		}, executor);
		CompletableFuture<Void> createChildren = loadChildren.thenAcceptBothAsync(createRepoNeo, (childQueryLocations, neoId) -> {
			addChildrenRepositories(neo4jUrl, childQueryLocations, neoId);
			System.out.println("Child " + childQueryLocation.toString() + " children are created in neoDB " + ((new Date().getTime()-start.getTime())/1000) + " seconds");
		}, executor);
		
		createUnitsNeo.thenAcceptBothAsync(createChildren, (void1, void2) -> {
			System.out.println("Child " + childQueryLocation.toString() + " in " + ((new Date().getTime()-start.getTime())/1000) + " seconds");
			}, executor);
	}
	
	public void addChildrenRepositories(String neo4jUrl, List<URI> childrenLocations, int parentId) {
		ExecutorService executor = Executors.newCachedThreadPool();
		for (URI childLocation : childrenLocations) {
			executor.execute(() -> {
//				Date start = new Date();
				addChildRepositoryWithFuture(neo4jUrl, childLocation, parentId, executor);
//				System.out.println("Child: " + childLocation.toString() + " in " + ((new Date().getTime()-start.getTime())/1000) + " seconds loaded");
			});
		}
	}
	
	/**
	 * Retrieves all child repositories of a given repository from the p2 query service.
	 * 
	 * @param parentQueryLocation The uri of the parent repository under the p2 query service.
	 * @return The URIs of all children of the repository.
	 */
	public List<URI> getChildrenQueryService(URI parentQueryLocation) {
		ResponseEntity<ArrayNode> response = queryServiceRestTemplate.getForEntity(parentQueryLocation+"/children?csv=false", ArrayNode.class);
		while (response.getStatusCodeValue() == 204) {
			try {
				Thread.sleep(250);
				response = queryServiceRestTemplate.getForEntity(parentQueryLocation+"/children?csv=false", ArrayNode.class);
			} catch (InterruptedException e) {
				e.printStackTrace();
				response = queryServiceRestTemplate.getForEntity(parentQueryLocation+"/children?csv=false", ArrayNode.class);
			}
		}
		ArrayNode arrayNode = response.getBody();
		List<URI> children = new ArrayList<>();
		arrayNode.forEach(jsonNode -> {
			if(jsonNode.isObject()) {
				ObjectNode objectNode = (ObjectNode)jsonNode;
				try {
					String queryLocation =parentQueryLocation.toString().replaceFirst("(.*)(\\/\\d+\\/?)$", "$1/");
					children.add(new URI(queryLocation /*+ "/"*/ + objectNode.get("id").asInt()));
				} catch (URISyntaxException e) {
				}
			}
		});
		
		return children;
	}

	/**
	 * Retrieves the amount of installable units of a repository from the p2 query service.
	 * 
	 * @param queryLocation The {@link URI} of the repository under the p2 query service.
	 * @return The amount of installable units of the repository.
	 */
	public Integer getUnitCountQueryService(URI repoQueryLocation) {
		ResponseEntity<Integer> response = queryServiceRestTemplate.getForEntity(repoQueryLocation+"/units/count", Integer.class);
		int debugCounter = 1;
		while (response.getStatusCodeValue() == 204) {
			try {
				Thread.sleep(250);
				response = queryServiceRestTemplate.getForEntity(repoQueryLocation+"/units/count", Integer.class);
			} catch (InterruptedException e) {
				e.printStackTrace();
				response = queryServiceRestTemplate.getForEntity(repoQueryLocation+"/units/count", Integer.class);
			}
		}
		Integer count = response.getBody();
		
		return count;
	}

	public Repository toRepository(ArrayNode repoData) {
		Repository r = new Repository();
		r.setRepoId(repoData.get(0).asInt());
		r.setUri(repoData.get(1).asText());
		
		// HATEOAS links
		r.add(linkTo(methodOn(RepositoryController.class).getRepositoryURI(r.getRepoId())).withSelfRel());
		r.add(linkTo(methodOn(RepositoryController.class).listUnitsInRepository(r.getRepoId())).withRel("installableUnits"));
		
		return r;
	}
	
	public InstallableUnit toUnit(ArrayNode unitData) {
		InstallableUnit iu = new InstallableUnit();
		iu.setUnitId(unitData.get(0).asText());
		iu.setVersion(unitData.get(1).asText());
		
		// HATEOAS links
		iu.add(linkTo(methodOn(InstallableUnitController.class).listRepositoriesForUnitVersion(iu.getUnitId(), iu.getVersion())).withRel("repositories"));
		
		return iu;
	}
	
	public String neoResultLimit(String limit, String offset) {
		return (Integer.parseInt(offset) <= 0 ? "" : " SKIP "+offset)
			 + (Integer.parseInt(limit) <= 0 ? "" : " LIMIT "+limit);
	}

}
