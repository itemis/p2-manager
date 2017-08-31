package com.itemis.p2m.backend;

import java.net.URI;
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
import com.itemis.p2m.backend.model.InstallableUnit;
import com.itemis.p2m.backend.model.Repository;

public class Methods {
	
	private int openCalls = 0;

	URI postRepositoriesQueryService(URI uri, String queryserviceUrl) {
		openCalls ++;
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
	
	Repository getRepositoryQueryService(URI location) {
		RestTemplate restTemplate = new RestTemplate();
		Repository result = restTemplate.getForObject(location, Repository.class);
		return result;
	}
	
	int postRepositoriesNeoDB(String neo4jUsername, String neo4jPassword, String neo4jUrl, /*Repository repository*/URI queryLocation) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		StringBuilder queryBuilder = new StringBuilder("LOAD CSV FROM '").append(queryLocation.toString()).append("?csv=true' AS line ");
		queryBuilder.append("MERGE (r:Repository {serviceId : line[0], uri : line[2]}) RETURN ID(r)");
		Map<String,Object> body = Collections.singletonMap("query", queryBuilder.toString());
//		body.put("query", "MERGE (r:Repository {serviceId : {id}, uri : {uri}}) RETURN r");
//		body.put("params", repository);
		
		ObjectNode jsonResult = restTemplate.postForObject(neo4jUrl, body, ObjectNode.class);
		ArrayNode dataNode = (ArrayNode) jsonResult.get("data");
//		ObjectNode metadateNode = ((ObjectNode)((ObjectNode)((ArrayNode)dataNode.get(0)).get(0)).get("metadata"));
//		return metadateNode.get("id").asInt();
		return dataNode.get(1).get(0).asInt();
	}

	public List<LinkedHashMap<String, String>> getUnitsQueryService(URI repoLocation) {
		RestTemplate restTemplate = new RestTemplate();
		List<LinkedHashMap<String, String>> ius = new ArrayList<>();
		ius = restTemplate.getForObject(repoLocation+"/units", ius.getClass());
		return ius;
	}
	
	/*
	 * LOAD CSV FROM 'http://localhost:8888/repositories/3/units' AS line
	 * MATCH (r:Repository) WHERE r.uri="http://download.eclipse.org/releases/neon/201705151400"
	 * MERGE (iu:IU { id: line[0]})
	 * MERGE (r)-[p:PROVIDES { version: line[1]}]->(iu)
	 */

	Integer postUnitsNeoDB(String neo4jUsername, String neo4jPassword, String neo4jUrl, int repoId, URI queryLocation){
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
		
		return 0;
	}
	
	Integer postUnitsNeoDB(String neo4jUsername, String neo4jPassword, String neo4jUrl, int repoId, List<LinkedHashMap<String, String>> ius, URI uri){
		String repouri = uri.toString().toUpperCase();
		Date startTimeOfThisMethod = new Date();
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		StringBuilder buildMergeIU = new StringBuilder();
		StringBuilder buildProvides = new StringBuilder();
		StringBuilder buildReturn = new StringBuilder("RETURN r.url");
		int iuid = 0;
		ExecutorService executor = Executors.newFixedThreadPool(1);
		for (LinkedHashMap<String, String> iuMap : ius) {
			InstallableUnit iu = new InstallableUnit(iuMap.get("id"), iuMap.get("version"));
			buildMergeIU.append("MERGE (iu" + iuid + ":IU { id: '" + iu.getId() + "'}) ");
			buildProvides.append("MERGE (r)-[p" + iuid + ":PROVIDES { version: '" + iu.getVersion() + "'}]->(iu" + iuid + ") ");
			buildReturn.append(",iu" + iuid + ".id,p" + iuid + ".version");
			iuid++;
			if (iuid%50==0) {
				String mergeIU = buildMergeIU.toString();
				String provides = buildProvides.toString();
				String returnQuery = buildReturn.toString();
				final int printID = iuid;
				executor.execute(()->sendPostUnitsNeoDB(restTemplate, repoId, mergeIU, provides, returnQuery, neo4jUrl, repouri, printID));
				buildMergeIU = new StringBuilder();
				buildProvides = new StringBuilder();
				buildReturn = new StringBuilder("RETURN r.url");
			}
		}
		String mergeIU = buildMergeIU.toString();
		String provides = buildProvides.toString();
		String returnQuery = buildReturn.toString();
		executor.execute(()->sendPostUnitsNeoDB(restTemplate, repoId, mergeIU, provides, returnQuery, neo4jUrl, repouri));
		Future<Integer> future = executor.submit(()->{
			System.out.println("[" + repouri + "]: needed Time: " + (new Date().getTime()-startTimeOfThisMethod.getTime()));
			return --openCalls;
		});
		try {
			return future.get();
		} catch (Exception e) {
			return -1;
		}
	}
	
	private void sendPostUnitsNeoDB(RestTemplate restTemplate, int repoId, String mergeIU, String provides, String returnQuery, String neo4jUrl, String repouri, int id) {
		Date startTimeOfThisMethod = new Date();
		Map<String,Object> params = Collections.singletonMap("query", "MATCH (r:Repository) WHERE ID(r)=" + repoId + " " + mergeIU + provides + returnQuery);
//		System.out.println("[" + repouri + "]: REQUEST");
		ObjectNode _result = restTemplate.postForObject(neo4jUrl, params, ObjectNode.class);
//		System.out.println("[" + repouri + "]: RESPONSE");
		System.out.println("[" + repouri + "]: at id " + id + " needed Time: " + (new Date().getTime()-startTimeOfThisMethod.getTime()));
	}
	
	private void sendPostUnitsNeoDB(RestTemplate restTemplate, int repoId, String mergeIU, String provides, String returnQuery, String neo4jUrl, String repouri) {
		Date startTimeOfThisMethod = new Date();
		Map<String,Object> params = Collections.singletonMap("query", "MATCH (r:Repository) WHERE ID(r)=" + repoId + " " + mergeIU + provides + returnQuery);
//		System.out.println("[" + repouri + "]: REQUEST");
		ObjectNode _result = restTemplate.postForObject(neo4jUrl, params, ObjectNode.class);
//		System.out.println("[" + repouri + "]: RESPONSE");
		System.out.println("[" + repouri + "]: at last id needed Time: " + (new Date().getTime()-startTimeOfThisMethod.getTime()));
	}

}
