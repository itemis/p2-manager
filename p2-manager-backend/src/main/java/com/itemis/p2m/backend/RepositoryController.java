package com.itemis.p2m.backend;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.itemis.p2m.backend.model.InstallableUnit;
import com.itemis.p2m.backend.model.Repository;

@Controller
public class RepositoryController {
	@Value("${url.queryservice}")
	private String queryserviceUrl;	
	@Value("${url.neo4j.cypher}")
	private String neo4jUrl;	
	@Value("${neo4j.username}")
	private String neo4jUsername;
	@Value("${neo4j.password}")
	private String neo4jPassword;
	
	@RequestMapping("/repositories")
	@ResponseBody
	List<Repository> listRepositories() throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(
				  new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		List<Repository> result = new ArrayList<>();
		Map<String,Object> params = Collections.singletonMap("query", "MATCH (r:Repository) RETURN r.id,r.url");
		
		ObjectNode _result = restTemplate.postForObject(neo4jUrl, params, ObjectNode.class);
				
		ArrayNode dataNode = (ArrayNode) _result.get("data");
		dataNode.forEach((d) -> {
			ArrayNode node = (ArrayNode) d;
			Repository r = new Repository();
			r.id = node.get(0).asInt();
			r.url = node.get(1).asText();
			result.add(r);
		});
		return result;
	}
	
	@RequestMapping(method=RequestMethod.POST, value="/repositories")
	@ResponseBody
	URI addRepository(@RequestParam URI uri) throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(
				  new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		
		RestTemplate restTemplateQueryService = new RestTemplate();
		HttpMessageConverter formHttpMessageConverter = new FormHttpMessageConverter();
		HttpMessageConverter stringHttpMessageConverternew = new StringHttpMessageConverter();
		restTemplateQueryService.setMessageConverters(Lists.newArrayList(formHttpMessageConverter, stringHttpMessageConverternew));
		
		Repository r = new Repository();
		r.url = uri.toString();
		URI location = restTemplateQueryService.postForLocation(queryserviceUrl+"/repositories", "uri="+uri);

		return location;
		/*
		List<Repository> result = new ArrayList<>();
		Map<String,Object> params = Collections.singletonMap("query", "MATCH (r:Repository) RETURN r.id,r.url");
		
		ObjectNode _result = restTemplate.postForObject(neo4jUrl, params, ObjectNode.class);
				
		ArrayNode dataNode = (ArrayNode) _result.get("data");
		dataNode.forEach((d) -> {
			ArrayNode node = (ArrayNode) d;
			Repository r = new Repository();
			r.id = node.get(0).asInt();
			r.url = node.get(1).asText();
			result.add(r);
		});
		return result;
		*/
	}

	@RequestMapping("/repositories/{id}/units")
	@ResponseBody
	List<InstallableUnit> listUnitsInRepository(@PathVariable Integer id) throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(
				  new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		List<InstallableUnit> result = new ArrayList<>();
		Map<String,Object> params = Collections.singletonMap("query", "MATCH (r:Repository)-[p:PROVIDES]->(iu:IU) WHERE r.id = "+id+" RETURN iu.id,p.version");
		
		ObjectNode _result = restTemplate.postForObject(neo4jUrl, params, ObjectNode.class);
				
		ArrayNode dataNode = (ArrayNode) _result.get("data");
		dataNode.forEach((d) -> {
			ArrayNode node = (ArrayNode) d;
			InstallableUnit iu = new InstallableUnit();
			iu.id = node.get(0).asText();
			iu.version = node.get(1).asText();
			result.add(iu);
		});
		return result;
	}

//	@RequestMapping("/repositories")
//	@ResponseBody
//	List<Repository> repositories() {
//		RestTemplate restTemplate = new RestTemplate();
//		List<Repository> result = new ArrayList<>();
//		result = restTemplate.getForObject(queryserviceUrl+"/repositories", result.getClass());
//		return result;
//	}

}