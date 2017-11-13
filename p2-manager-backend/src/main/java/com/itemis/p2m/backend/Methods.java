package com.itemis.p2m.backend;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.itemis.p2m.backend.constants.RepositoryStatus;
import com.itemis.p2m.backend.model.InstallableUnit;
import com.itemis.p2m.backend.model.Repository;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;

public class Methods {
	
	/**
	 * Adds a repository to the p2 query service.
	 * 
	 * @param uri The URI of the repository to be added.
	 * @param queryserviceUrl The URL under which the p2 query service can be reached.
	 * @return The URI under which the repository has been added by the p2 query service.
	 * */
	URI postRepositoriesQueryService(URI uri, String queryserviceUrl) {
		RestTemplate restTemplate = new RestTemplate();
		HttpMessageConverter<?> formHttpMessageConverter = new FormHttpMessageConverter();
		HttpMessageConverter<?> stringHttpMessageConverternew = new StringHttpMessageConverter();
		restTemplate.setMessageConverters(Lists.newArrayList(formHttpMessageConverter, stringHttpMessageConverternew));
		MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
		formParams.add("uri", uri.toString());
		URI location = null;
		try {
			location = restTemplate.postForLocation(queryserviceUrl+"/repositories", formParams);
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
	 * @param queryLocation The {@link URI} of the repository under the p2 query service.
	 * @return The id assigned to the repository by the neo4j database.
	 */
	int postRepositoriesNeoDB(String neo4jUsername, String neo4jPassword, String neo4jUrl, URI queryLocation) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		StringBuilder queryBuilder = new StringBuilder("LOAD CSV WITH HEADERS FROM '").append(queryLocation.toString()).append("?csv=true' AS line ");
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
	 * @param queryLocation The {@link URI} of the repository under the p2 query service.
	 * @param parentId The id of the parent of the repository that is to be added.
	 * @return The id assigned to the repository by the neo4j database.
	 */
	int postRepositoriesNeoDB(String neo4jUsername, String neo4jPassword, String neo4jUrl, URI queryLocation, int parentId) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		StringBuilder queryBuilder = new StringBuilder("LOAD CSV WITH HEADERS FROM '").append(queryLocation.toString()).append("?csv=true' AS line ");
		queryBuilder.append("MATCH (p:Repository) WHERE ID(p)=").append(parentId).append(" ");
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
	 * @param repoId The id of the repository in the neo4j database.
	 * @param queryLocation The {@link URI} of the repository under the p2 query service.
	 */
	void postUnitsNeoDB(String neo4jUsername, String neo4jPassword, String neo4jUrl, int repoId, URI queryLocation){
		Date startTimeOfThisMethod = new Date();
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		StringBuilder queryBuilder = new StringBuilder("LOAD CSV WITH HEADERS FROM '").append(queryLocation.toString()).append("/units?csv=true' AS line ");
		queryBuilder.append("MATCH (r:Repository) WHERE ID(r)=").append(repoId).append(" ");
		queryBuilder.append("MERGE (iu:IU { serviceId: line.id}) ");
		queryBuilder.append("MERGE (r)-[p:PROVIDES { version: line.version}]->(iu)");
		Map<String,Object> body = Collections.singletonMap("query", queryBuilder.toString());
		ObjectNode jsonResult = restTemplate.postForObject(neo4jUrl, body, ObjectNode.class);
		System.out.println("needed Time for Units: " + ((new Date()).getTime() - startTimeOfThisMethod.getTime()));
		//TODO: maybe return number of new IUs?
	//	return 1;
	}
	
	void addChildRepository(String neo4jUsername, String neo4jPassword, String neo4jUrl, URI childLocation, int parentId) {
		int repoDBId = postRepositoriesNeoDB(neo4jUsername, neo4jPassword, neo4jUrl, childLocation, parentId);
		//Wait for Units are Loaded
		postUnitsNeoDB(neo4jUsername, neo4jPassword, neo4jUrl, repoDBId, childLocation);		
	}
	
	void addChildrenRepositories(String neo4jUsername, String neo4jPassword, String neo4jUrl, List<URI> childrenLocations, int parentId) {
		childrenLocations.forEach(childLocation -> addChildRepository(neo4jUsername, neo4jPassword, neo4jUrl, childLocation, parentId));
		//return childrenLocations.size();
	}
	
	/**
	 * Retrieves the current status of a resource from the p2 query service. 
	 * 
	 * @param location The {@link URI} of the resource under the p2 query service.
	 * @param wantedStatus The desired {@link RepositoryStatus} of the resource.
	 * @return The {@link URI} of the resource if it is already loaded or has the wantedStatus, null otherwise.
	 */
	URI getRepositoryStatusQueryService(URI location, String wantedStatus) {
		RestTemplate restTemplate = new RestTemplate();
		String status = restTemplate.getForObject(location+"/status", String.class);
		if (RepositoryStatus.LOADED.equals(status) || wantedStatus.equals(status))
			return location;
		else
			return null;
	}
	
	/**
	 * Retrieves all child repositories of a given repository from the p2 query service.
	 * 
	 * @param queryLocation The uri for accessing all repositories under the p2 query service.
	 * @param parentLocation The uri of the parent repository under the p2 query service.
	 * @return The URIs of all children of the repository.
	 */
	List<URI> getChildrenQueryService(String queryLocation, String parentLocation) {
		System.out.println(queryLocation);
		RestTemplate restTemplate = new RestTemplate();
		ArrayNode arrayNode = restTemplate.getForObject(parentLocation+"/children?csv=false", ArrayNode.class);
		List<URI> children = new ArrayList<>();
		arrayNode.forEach(jsonNode -> {
			try {
				children.add(new URI(queryLocation + "/" + ((ObjectNode)jsonNode).get("id").asText()));
			} catch (URISyntaxException e) {
				e.printStackTrace();
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
	int getUnitsCountQueryService(URI queryLocation) {
		RestTemplate restTemplate = new RestTemplate();
		int count = restTemplate.getForObject(queryLocation+"/units/count", Integer.class);
		return count;
	}

	Repository toRepository(ArrayNode repoData) {
		Repository r = new Repository();
		r.setRepoId(repoData.get(0).asInt());
		r.setUri(repoData.get(1).asText());
		
		// HATEOAS links
		r.add(linkTo(methodOn(RepositoryController.class).listUnitsInRepository(r.getRepoId())).withRel("installableUnits"));
		
		return r;
	}
	
	InstallableUnit toUnit(ArrayNode unitData) {
		InstallableUnit iu = new InstallableUnit();
		iu.setUnitId(unitData.get(0).asText());
		iu.setVersion(unitData.get(1).asText());
		
		return iu;
	}

}
