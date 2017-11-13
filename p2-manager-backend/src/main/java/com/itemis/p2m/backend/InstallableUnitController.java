package com.itemis.p2m.backend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itemis.p2m.backend.model.InstallableUnit;
import com.itemis.p2m.backend.model.Repository;

@RestController
@RequestMapping("/units")
public class InstallableUnitController {
	@Value("${url.queryservice}")
	private String queryserviceUrl;	
	@Value("${url.neo4j.cypher}")
	private String neo4jUrl;	
	@Value("${neo4j.username}")
	private String neo4jUsername;
	@Value("${neo4j.password}")
	private String neo4jPassword;
	
	private Methods methods;
	
	public InstallableUnitController() {
		this.methods = new Methods();
	}

	@RequestMapping(method=RequestMethod.GET)
	List<InstallableUnit> listInstallableUnits() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(
				  new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		Map<String,Object> params = Collections.singletonMap("query", "MATCH (iu:IU) RETURN iu.serviceId");
		
		ObjectNode _result = restTemplate.postForObject(neo4jUrl, params, ObjectNode.class);
		ArrayNode dataNode = (ArrayNode) _result.get("data");
		
		List<InstallableUnit> result = new ArrayList<>();
		dataNode.forEach((d) -> result.add(methods.toUnit((ArrayNode) d)));
		return result;
	}


	@RequestMapping(method=RequestMethod.GET, value="/{id}") //TODO give IUs an integer ID so that they can be encoded in a URI.
	List<InstallableUnit> listVersionsForInstallableUnit(@PathVariable String id, @RequestParam(defaultValue = "false") boolean showRepositories) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(
				  new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		Map<String,Object> params = Collections.singletonMap("query", "MATCH (r:Repository)-[p:PROVIDES]->(iu:IU) WHERE iu.serviceId = '"+id+"' RETURN iu.serviceId, p.version");
		
		ObjectNode _result = restTemplate.postForObject(neo4jUrl, params, ObjectNode.class);
		ArrayNode dataNode = (ArrayNode) _result.get("data");
		
		List<InstallableUnit> result = new ArrayList<>();
		dataNode.forEach((d) -> result.add(methods.toUnit((ArrayNode) d)));
		return result;
	}
	

}
