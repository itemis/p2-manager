package com.itemis.p2m.backend;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itemis.p2m.backend.exceptions.NothingToLoadException;
import com.itemis.p2m.backend.model.InstallableUnit;
import com.itemis.p2m.backend.model.Repository;
import com.itemis.p2m.backend.model.RepositoryProvidesVersion;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/units")
public class InstallableUnitController {
	@Value("${url.queryservice}")
	private String queryserviceUrl;	
	@Value("${url.neo4j.cypher}")
	private String neo4jUrl;
	
	private Methods methods;
	
	@Qualifier("neoRestTemplateBean")
	private RestTemplate neoRestTemplate;
	
	
	public InstallableUnitController(Methods methods, @Qualifier("neoRestTemplateBean") RestTemplate neoRestTemplate) {
		this.methods = methods;
		this.neoRestTemplate = neoRestTemplate;
	}

	@ApiOperation(value = "List all installable units whose ids match the search terms")
	@RequestMapping(method=RequestMethod.GET)
	List<InstallableUnit> listInstallableUnits(@RequestParam(required = false) String[] searchTerm,
											   @RequestParam(defaultValue = "0") String limit,
											   @RequestParam(defaultValue = "0") String offset) {
		String filter = searchTerm == null ? "" : Arrays.asList(searchTerm).parallelStream()
														.map((term) -> "iu.serviceId CONTAINS '"+term+"' ")
														.reduce((term1, term2) -> term1+"AND "+term2)
														.map((terms) -> "WHERE "+terms)
														.orElse("");
		
		Map<String,Object> params = Collections.singletonMap("query", "MATCH ()-[p:PROVIDES]->(iu:IU) "
																	+ filter
																	+ "RETURN DISTINCT iu.serviceId, p.version "
																	+ "ORDER BY iu.serviceId"
																	+ methods.neoResultLimit(limit,  offset));
		
		ObjectNode _result = neoRestTemplate.postForObject(neo4jUrl, params, ObjectNode.class);
		ArrayNode dataNode = (ArrayNode) _result.get("data");
		
		List<InstallableUnit> result = new ArrayList<>();
		dataNode.forEach((d) -> {
			InstallableUnit unit = methods.toUnit((ArrayNode) d);
			unit.add(linkTo(methodOn(InstallableUnitController.class).listVersionsForInstallableUnit(unit.getUnitId())).withRel("versions"));
			result.add(unit);
		});
		
		if (result.size() == 0)
			throw new NothingToLoadException();

		return result;
	}


	@ApiOperation(value = "List all available versions of the installable unit")
	@RequestMapping(method=RequestMethod.GET, value="/{id}/versions")
	List<InstallableUnit> listVersionsForInstallableUnit(@PathVariable String id) {
		Map<String,Object> params = Collections.singletonMap("query", "MATCH (r:Repository)-[p:PROVIDES]->(iu:IU) WHERE iu.serviceId = '"+id+"' RETURN DISTINCT iu.serviceId, p.version");
		
		ObjectNode _result = neoRestTemplate.postForObject(neo4jUrl, params, ObjectNode.class);
		ArrayNode dataNode = (ArrayNode) _result.get("data");
		
		List<InstallableUnit> result = new ArrayList<>();
		dataNode.forEach((d) -> result.add(methods.toUnit((ArrayNode) d)));
		
		return result;
	}

	@ApiOperation(value = "List all repositories that contain the installable unit in this version")
	@RequestMapping(method=RequestMethod.GET, value="/{id}/versions/{version}/repositories")
	List<Repository> listRepositoriesForUnitVersion(@PathVariable String id, @PathVariable String version) {
		Map<String,Object> params = Collections.singletonMap("query", "MATCH (r:Repository)-[p:PROVIDES]->(iu:IU) WHERE iu.serviceId = '"+id+"' AND p.version = '"+version+"' RETURN DISTINCT r.serviceId, r.uri");
		
		ObjectNode _result = neoRestTemplate.postForObject(neo4jUrl, params, ObjectNode.class);
		ArrayNode dataNode = (ArrayNode) _result.get("data");
		
		List<Repository> result = new ArrayList<>();
		dataNode.forEach((d) -> result.add(methods.toRepository((ArrayNode) d)));
		return result;
	}

	@ApiOperation(value = "List all repositories that contain the installable unit in this version")
	@RequestMapping(method=RequestMethod.GET, value="/{id}/versions/repositories")
	List<RepositoryProvidesVersion> listRepositoriesForUnitVersionRange(@PathVariable String id, @RequestParam(value="minVersion", defaultValue="") String versionLow, @RequestParam(value="maxVersion", defaultValue="") String versionHigh) {
		StringBuilder bodyBuilder = new StringBuilder("MATCH (r:Repository)-[p:PROVIDES]->(iu:IU) WHERE iu.serviceId = '");
		bodyBuilder.append(id);
		if (!versionLow.isEmpty())
			bodyBuilder.append("' AND p.version > '").append(versionLow);
		if (!versionHigh.isEmpty())
			bodyBuilder.append("' AND p.version < '").append(versionHigh);
		bodyBuilder.append("' RETURN DISTINCT r.serviceId, r.uri, p.version");
		Map<String,Object> params = Collections.singletonMap("query", bodyBuilder.toString());
		
		ObjectNode _result = neoRestTemplate.postForObject(neo4jUrl, params, ObjectNode.class);
		ArrayNode dataNode = (ArrayNode) _result.get("data");
		
		List<RepositoryProvidesVersion> result = new ArrayList<>();
		dataNode.forEach((d) -> result.add(methods.toRepositoryProvidesVersion((ArrayNode) d)));
		return result;
	}
}