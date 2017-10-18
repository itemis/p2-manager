package com.itemis.p2m.backend;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.itemis.p2m.backend.constants.RepositoryStatus;
import com.itemis.p2m.backend.model.InstallableUnit;
import com.itemis.p2m.backend.model.Repository;

public class Methods {
	
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
	
	int postRepositoriesNeoDB(String neo4jUsername, String neo4jPassword, String neo4jUrl, URI queryLocation) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		StringBuilder queryBuilder = new StringBuilder("LOAD CSV FROM '").append(queryLocation.toString()).append("?csv=true' AS line ");
		queryBuilder.append("MERGE (r:Repository {serviceId : line[0], uri : line[2]}) RETURN ID(r)");
		Map<String,Object> body = Collections.singletonMap("query", queryBuilder.toString());
		
		ObjectNode jsonResult = restTemplate.postForObject(neo4jUrl, body, ObjectNode.class);
		ArrayNode dataNode = (ArrayNode) jsonResult.get("data");
		return dataNode.get(1).get(0).asInt();
	}
	
	int postRepositoriesNeoDB(String neo4jUsername, String neo4jPassword, String neo4jUrl, URI queryLocation, int parentId) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		StringBuilder queryBuilder = new StringBuilder("LOAD CSV FROM '").append(queryLocation.toString()).append("?csv=true' AS line ");
		queryBuilder.append("MATCH (p:Repository) WHERE ID(r)=").append(parentId).append(" ");
		queryBuilder.append("MERGE (r:Repository {serviceId : line[0], uri : line[2]}) ");
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
		StringBuilder queryBuilder = new StringBuilder("LOAD CSV FROM '").append(queryLocation.toString()).append("/units?csv=true' AS line ");
		queryBuilder.append("MATCH (r:Repository) WHERE ID(r)=").append(repoId).append(" ");
		queryBuilder.append("MERGE (iu:IU { id: line[0]}) ");
		queryBuilder.append("MERGE (r)-[p:PROVIDES { version: line[1]}]->(iu)");
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
