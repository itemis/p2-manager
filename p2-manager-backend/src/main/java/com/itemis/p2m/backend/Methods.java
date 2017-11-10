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
	 * @param queryLocation The URI of the repository under the p2 query service.
	 * @return The id assigned to the repository by the neo4j database.
	 */
	int postRepositoriesNeoDB(String neo4jUsername, String neo4jPassword, String neo4jUrl, URI queryLocation) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		StringBuilder queryBuilder = new StringBuilder("LOAD CSV WITH HEADERS FROM '").append(queryLocation.toString()).append("?csv=true' AS line ");
		queryBuilder.append("MERGE (r:Repository {serviceId : line.id, uri : line.uri}) RETURN ID(r)"); //TODO first line of the .csv file containing column names must be skipped
		Map<String,Object> body = Collections.singletonMap("query", queryBuilder.toString());
		
		ObjectNode jsonResult = restTemplate.postForObject(neo4jUrl, body, ObjectNode.class);
		ArrayNode dataNode = (ArrayNode) jsonResult.get("data");
		return dataNode.get(1).get(0).asInt();
	}
	
	/**
	 * Adds a repository to the neo4j database as child of an existing repository.
	 * 
	 * @param parentId The id of the parent of the repository that is to be added.
	 */
	int postRepositoriesNeoDB(String neo4jUsername, String neo4jPassword, String neo4jUrl, URI queryLocation, int parentId) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		StringBuilder queryBuilder = new StringBuilder("LOAD CSV WITH HEADERS FROM '").append(queryLocation.toString()).append("?csv=true' AS line ");
		queryBuilder.append("MATCH (p:Repository) WHERE ID(r)=").append(parentId).append(" ");
		queryBuilder.append("MERGE (r:Repository {serviceId : line.id, uri : line.uri}) ");
		queryBuilder.append("MERGE (p)-[po:PARENT_OF]->(r) RETURN ID(r)");
		Map<String,Object> body = Collections.singletonMap("query", queryBuilder.toString());
		
		ObjectNode jsonResult = restTemplate.postForObject(neo4jUrl, body, ObjectNode.class);
		ArrayNode dataNode = (ArrayNode) jsonResult.get("data");
		return dataNode.get(1).get(0).asInt();
	}
	
	/*
	 * LOAD CSV FROM 'http://localhost:8888/repositories/3/units' AS line
	 * MATCH (r:Repository) WHERE r.uri="http://download.eclipse.org/releases/neon/201705151400"
	 * MERGE (iu:IU { id: line[0]})
	 * MERGE (r)-[p:PROVIDES { version: line[1]}]->(iu)
	 */

	void postUnitsNeoDB(String neo4jUsername, String neo4jPassword, String neo4jUrl, int repoId, URI queryLocation){
		Date startTimeOfThisMethod = new Date();
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		StringBuilder queryBuilder = new StringBuilder("LOAD CSV WITH HEADERS FROM '").append(queryLocation.toString()).append("/units?csv=true' AS line ");
		queryBuilder.append("MATCH (r:Repository) WHERE ID(r)=").append(repoId).append(" ");
		queryBuilder.append("MERGE (iu:IU { id: line.id}) ");
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
	
	URI getRepositoryStatusQueryService(URI location, String wantedStatus) {
		RestTemplate restTemplate = new RestTemplate();
		String status = restTemplate.getForObject(location+"/status", String.class);
		if (RepositoryStatus.LOADED.equals(status) || wantedStatus.equals(status))
			return location;
		else
			return null;
	}
	
	List<URI> getChildrenQueryService(URI parentLocation) {
		String queryService = parentLocation.toString().substring(0, parentLocation.toString().lastIndexOf("/"));
		RestTemplate restTemplate = new RestTemplate();
		ArrayNode arrayNode = restTemplate.getForObject(parentLocation+"/children?csv=false", ArrayNode.class);
		List<URI> children = new ArrayList<>();
		arrayNode.forEach(jsonNode -> {
			try {
				children.add(new URI(queryService + ((ObjectNode)jsonNode).get("id").asText()));
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		});
		return children;
	}

	int getUnitsCountQueryService(URI queryLocation) {
		RestTemplate restTemplate = new RestTemplate();
		int count = restTemplate.getForObject(queryLocation+"/units/count", Integer.class);
		return count;
	}

}
