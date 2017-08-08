package com.itemis.p2m.backend;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
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
		
		URI location = restTemplate.postForLocation(queryserviceUrl+"/repositories", formParams);
		return location;
	}
	
	Repository getRepositoryQueryService(URI location) {
		RestTemplate restTemplate = new RestTemplate();
		Repository result = restTemplate.getForObject(location, Repository.class);
		return result;
	}
	
	int postRepositoriesNeoDB(String neo4jUsername, String neo4jPassword, String neo4jUrl, Repository repository) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		Map<String,Object> body = new LinkedHashMap<>(2);
		body.put("query", "MERGE (r:Repository {id : {id}, uri : {uri}}) RETURN r");
		body.put("params", repository);
		
		ObjectNode jsonResult = restTemplate.postForObject(neo4jUrl, body, ObjectNode.class);
		ArrayNode dataNode = (ArrayNode) jsonResult.get("data");
		ObjectNode metadateNode = ((ObjectNode)((ObjectNode)((ArrayNode)dataNode.get(0)).get(0)).get("metadata"));
		return metadateNode.get("id").asInt();
	}

	public List<LinkedHashMap<String, String>> getUnitsQueryService(URI repoLocation) {
		RestTemplate restTemplate = new RestTemplate();
		List<LinkedHashMap<String, String>> ius = new ArrayList<>();
		ius = restTemplate.getForObject(repoLocation+"/units", ius.getClass());
		return ius;
	}
	//TODO: Wrong repoId
	void postUnitsNeoDB(String neo4jUsername, String neo4jPassword, String neo4jUrl, int repoId, List<LinkedHashMap<String, String>> ius){
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		StringBuilder buildMergeIU = new StringBuilder();
		StringBuilder buildProvides = new StringBuilder();
		StringBuilder buildReturn = new StringBuilder("RETURN r.url,");
		int iuid = 1;
		for (LinkedHashMap<String, String> iuMap : ius) {
			InstallableUnit iu = new InstallableUnit(iuMap.get("id"), iuMap.get("version"));
			buildMergeIU.append("MERGE (iu" + iuid + ":IU { id: '" + iu.getId() + "'}) ");
			buildProvides.append("MERGE (r)-[p" + iuid + ":PROVIDES { version: '" + iu.getVersion() + "'}]->(iu" + iuid + ") ");
			buildReturn.append("iu" + iuid + ".id,p" + iuid + ".version,");
			iuid++;
		}
		buildReturn.delete(buildReturn.length()-1, buildReturn.length());
		Map<String,Object> params = Collections.singletonMap("query", "MATCH (r:Repository) WHERE r.id=" + repoId + " " + buildMergeIU.toString() + buildProvides.toString() + buildReturn.toString());
		System.out.println(params.get("query"));
		ObjectNode _result = restTemplate.postForObject(neo4jUrl, params, ObjectNode.class);
	}

}
