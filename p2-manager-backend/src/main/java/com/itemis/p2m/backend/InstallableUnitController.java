package com.itemis.p2m.backend;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itemis.p2m.backend.model.InstallableUnit;
import com.itemis.p2m.backend.model.Repository;

import io.swagger.annotations.ApiOperation;

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

	@ApiOperation(value = "List all installable units")
	@RequestMapping(method=RequestMethod.GET)
	List<InstallableUnit> listInstallableUnits() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(
				  new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		Map<String,Object> params = Collections.singletonMap("query", "MATCH ()-[p:PROVIDES]->(iu:IU) RETURN iu.serviceId, p.version");
		
		ObjectNode _result = restTemplate.postForObject(neo4jUrl, params, ObjectNode.class);
		ArrayNode dataNode = (ArrayNode) _result.get("data");
		
		List<InstallableUnit> result = new ArrayList<>();
		dataNode.forEach((d) -> result.add(methods.toUnit((ArrayNode) d)));
		return result;
	}


	@ApiOperation(value = "List all available versions of the installable unit")
	@RequestMapping(method=RequestMethod.GET, value="/{id}/versions")
	List<InstallableUnit> listVersionsForInstallableUnit(@PathVariable String id) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(
				  new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		Map<String,Object> params = Collections.singletonMap("query", "MATCH (r:Repository)-[p:PROVIDES]->(iu:IU) WHERE iu.serviceId = '"+id+"' RETURN iu.serviceId, p.version");
		
		ObjectNode _result = restTemplate.postForObject(neo4jUrl, params, ObjectNode.class);
		ArrayNode dataNode = (ArrayNode) _result.get("data");
		
		List<InstallableUnit> result = new ArrayList<>();
		dataNode.forEach((d) -> {
			InstallableUnit unit = methods.toUnit((ArrayNode) d);
			unit.add(linkTo(methodOn(InstallableUnitController.class).listRepositoriesForUnitVersion(unit.getUnitId(), unit.getVersion())).withRel("repositories"));
			result.add(unit);
		});
		
		return result;
	}

	@ApiOperation(value = "List all repositories that contain the installable unit in this version")
	@RequestMapping(method=RequestMethod.GET, value="/{id}/versions/{version}/repositories")
	List<Repository> listRepositoriesForUnitVersion(@PathVariable String id, @PathVariable String version) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(
				  new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		Map<String,Object> params = Collections.singletonMap("query", "MATCH (r:Repository)-[p:PROVIDES]->(iu:IU) WHERE iu.serviceId = '"+id+"' AND p.version = '"+version+"' RETURN r.serviceId, r.uri");
		
		ObjectNode _result = restTemplate.postForObject(neo4jUrl, params, ObjectNode.class);
		ArrayNode dataNode = (ArrayNode) _result.get("data");
		
		List<Repository> result = new ArrayList<>();
		dataNode.forEach((d) -> result.add(methods.toRepository((ArrayNode) d)));
		return result;
	}
	
	//TODO: method to retrieve all repositories that have a unit in a version between a minimum and maximum


}
