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

import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
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

public class Methods {
	
	/**
	 * Adds a repository to the p2 query service.
	 * 
	 * @param repositoryUri The URI of the repository to be added.
	 * @param queryLocation The URL under which the p2 query service can be reached.
	 * @return The URI under which the repository has been added by the p2 query service.
	 * */
	URI postRepositoriesQueryService(URI repositoryUri, URI queryLocation) {
		RestTemplate restTemplate = new RestTemplate();
		HttpMessageConverter<?> formHttpMessageConverter = new FormHttpMessageConverter();
		HttpMessageConverter<?> stringHttpMessageConverternew = new StringHttpMessageConverter();
		restTemplate.setMessageConverters(Lists.newArrayList(formHttpMessageConverter, stringHttpMessageConverternew));
		MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
		formParams.add("uri", repositoryUri.toString());
		URI location = null;
		try {
			location = restTemplate.postForLocation(queryLocation+"/repositories", formParams);
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
	 * @param neo4jUsername The username used as login for the neo4j database.
	 * @param neo4jPassword The password used as login for the neo4j database.
	 * @param neo4jUrl The URL under which the neo4j database can be reached.
	 * @param repoQueryLocation The {@link URI} of the repository under the p2 query service.
	 * @return The id assigned to the repository by the neo4j database.
	 */
	int postRepositoriesNeoDB(String neo4jUsername, String neo4jPassword, String neo4jUrl, URI repoQueryLocation) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		StringBuilder queryBuilder = new StringBuilder("LOAD CSV WITH HEADERS FROM '").append(repoQueryLocation.toString()).append("?csv=true' AS line ");
		queryBuilder.append("MERGE (r:Repository {serviceId : line.id, uri : line.uri}) RETURN ID(r)");
		Map<String,Object> body = Collections.singletonMap("query", queryBuilder.toString());
		
		ObjectNode jsonResult = restTemplate.postForObject(neo4jUrl, body, ObjectNode.class);
		ArrayNode dataNode = (ArrayNode) jsonResult.get("data");
		return dataNode.get(0).get(0).asInt();
	}
	
	/**
	 * Adds a repository to the neo4j database as child of an existing repository.
	 * 
	 * @param neo4jUsername The username used as login for the neo4j database.
	 * @param neo4jPassword The password used as login for the neo4j database.
	 * @param neo4jUrl The URL under which the neo4j database can be reached.
	 * @param repoQueryLocation The {@link URI} of the repository under the p2 query service.
	 * @param parentNeoId The id of the parent of the repository that is to be added.
	 * @return The id assigned to the repository by the neo4j database.
	 */
	int postRepositoriesNeoDB(String neo4jUsername, String neo4jPassword, String neo4jUrl, URI repoQueryLocation, int parentNeoId) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		StringBuilder queryBuilder = new StringBuilder("LOAD CSV WITH HEADERS FROM '").append(repoQueryLocation.toString()).append("?csv=true' AS line ");
		queryBuilder.append("MATCH (p:Repository) WHERE ID(p)=").append(parentNeoId).append(" ");
		queryBuilder.append("MERGE (r:Repository {serviceId : line.id, uri : line.uri}) ");
		queryBuilder.append("MERGE (p)-[po:PARENT_OF]->(r) RETURN ID(r)");
		Map<String,Object> body = Collections.singletonMap("query", queryBuilder.toString());
		
		ObjectNode jsonResult = restTemplate.postForObject(neo4jUrl, body, ObjectNode.class);
		ArrayNode dataNode = (ArrayNode) jsonResult.get("data");
		
		return dataNode.get(0).get(0).asInt();
	}
	
	/*
	 * LOAD CSV FROM 'http://localhost:8888/repositories/3/units' AS line
	 * MATCH (r:Repository) WHERE r.uri="http://download.eclipse.org/releases/neon/201705151400"
	 * MERGE (iu:IU { id: line[0]})
	 * MERGE (r)-[p:PROVIDES { version: line[1]}]->(iu)
	 */

	/**
	 * Adds the installable units of a repository to the neo4j database.
	 * 
	 * @param neo4jUsername The username used as login for the neo4j database.
	 * @param neo4jPassword The password used as login for the neo4j database.
	 * @param neo4jUrl The URL under which the neo4j database can be reached.
	 * @param neoId The id of the repository in the neo4j database.
	 * @param repoQueryLocation The {@link URI} of the repository under the p2 query service.
	 */
	void postUnitsNeoDB(String neo4jUsername, String neo4jPassword, String neo4jUrl, int neoId, URI repoQueryLocation){
		if (getUnitCountQueryService(repoQueryLocation) == 0) {
			return;
		}
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		StringBuilder queryBuilder = new StringBuilder("LOAD CSV WITH HEADERS FROM '").append(repoQueryLocation.toString()).append("/units?csv=true' AS line ");
		queryBuilder.append("MATCH (r:Repository) WHERE ID(r)=").append(neoId).append(" ");
		queryBuilder.append("MERGE (iu:IU { serviceId: line.id}) ");
		queryBuilder.append("MERGE (r)-[p:PROVIDES { version: line.version}]->(iu)");
		Map<String,Object> body = Collections.singletonMap("query", queryBuilder.toString());
		ObjectNode jsonResult = restTemplate.postForObject(neo4jUrl, body, ObjectNode.class);
		
	}
	
	//repo to neo: String neo4jUsername, String neo4jPassword, String neo4jUrl, URI repoQueryLocation, int parentNeoId
	//units to neo: String neo4jUsername, String neo4jPassword, String neo4jUrl, int neoId, URI repoQueryLocation 
	private void addChildRepository(String neo4jUsername, String neo4jPassword, String neo4jUrl, URI childQueryLocation, int parentNeoId) {
		int neoId = postRepositoriesNeoDB(neo4jUsername, neo4jPassword, neo4jUrl, childQueryLocation, parentNeoId);
		postUnitsNeoDB(neo4jUsername, neo4jPassword, neo4jUrl, neoId, childQueryLocation);
		addChildrenRepositories(neo4jUsername, neo4jPassword, neo4jUrl, getChildrenQueryService(childQueryLocation), neoId);
		
	}
	
	void addChildrenRepositories(String neo4jUsername, String neo4jPassword, String neo4jUrl, List<URI> childrenLocations, int parentId) {
		for (URI childLocation : childrenLocations) {
			Date start = new Date();
			addChildRepository(neo4jUsername, neo4jPassword, neo4jUrl, childLocation, parentId);
//			System.out.println("Child: " + childLocation.toString() + "in " + ((new Date().getTime()-start.getTime())/1000) + " seconds loaded");
		}
	}
	
	/**
	 * Retrieves all child repositories of a given repository from the p2 query service.
	 * 
	 * @param parentQueryLocation The uri of the parent repository under the p2 query service.
	 * @return The URIs of all children of the repository.
	 */
	List<URI> getChildrenQueryService(URI parentQueryLocation) {
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<ArrayNode> response = restTemplate.getForEntity(parentQueryLocation+"/children?csv=false", ArrayNode.class);
		while (response.getStatusCodeValue() == 204) {
			try {
				Thread.sleep(1000);
				response = restTemplate.getForEntity(parentQueryLocation+"/children?csv=false", ArrayNode.class);
			} catch (InterruptedException e) {
				e.printStackTrace();
				response = restTemplate.getForEntity(parentQueryLocation+"/children?csv=false", ArrayNode.class);
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
	Integer getUnitCountQueryService(URI repoQueryLocation) {
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<Integer> response = restTemplate.getForEntity(repoQueryLocation+"/units/count", Integer.class);
		while (response.getStatusCodeValue() == 204) {
			try {
				Thread.sleep(1000);
				response = restTemplate.getForEntity(repoQueryLocation+"/units/count", Integer.class);
			} catch (InterruptedException e) {
				e.printStackTrace();
				response = restTemplate.getForEntity(repoQueryLocation+"/units/count", Integer.class);
			}
		}
		Integer count = response.getBody();
		
		return count;
	}

	Repository toRepository(ArrayNode repoData) {
		Repository r = new Repository();
		r.setRepoId(repoData.get(0).asInt());
		r.setUri(repoData.get(1).asText());
		
		// HATEOAS links
		r.add(linkTo(methodOn(RepositoryController.class).getRepositoryURI(r.getRepoId())).withSelfRel());
		r.add(linkTo(methodOn(RepositoryController.class).listUnitsInRepository(r.getRepoId())).withRel("installableUnits"));
		return r;
	}
	
	InstallableUnit toUnit(ArrayNode unitData) {
		InstallableUnit iu = new InstallableUnit();
		iu.setUnitId(unitData.get(0).asText());
		iu.setVersion(unitData.get(1).asText());
		
		// HATEOAS links
		//iu.add(linkTo(methodOn(InstallableUnitController.class).listVersionsForInstallableUnit(iu.getUnitId(), false)).withRel("versions"));
		//TODO: add link where available repositories for this version are shown
		return iu;
	}

}
