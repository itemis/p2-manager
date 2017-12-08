package com.itemis.p2m.backend;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

		Neo4JQueryBuilder query = new Neo4JQueryBuilder();

		query.match("(r:Repository)-[p:PROVIDES]->(iu:IU)")
			 .result("iu.serviceId, p.version")
			 .orderBy("iu.serviceId")
			 .limit(limit, offset)
			 .distinct();
		
		if (!(searchTerm == null)) {
			List<String> keywords = new ArrayList<>();
			Collections.addAll(keywords, searchTerm);
			List<String> repoKeywords = keywords.parallelStream()
												.filter(keyword -> keyword.startsWith("repo:"))
												.collect(Collectors.toList());

			keywords.parallelStream()
					.filter(k -> !repoKeywords.contains(k))
					.map(k -> "iu.serviceId CONTAINS '"+k+"'")
					.forEach(k -> query.filter(k));
			
			repoKeywords.parallelStream()
						.filter(k -> k.length() > 5)
						.map(k -> k.substring(5))
						.map(k -> "r.uri CONTAINS '"+k+"' ")
						.forEach(k -> query.filter(k));
		}
		
		ObjectNode _result = neoRestTemplate.postForObject(neo4jUrl, query.buildMap(), ObjectNode.class);
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
		Neo4JQueryBuilder query = new Neo4JQueryBuilder().match("(r:Repository)-[p:PROVIDES]->(iu:IU)")
														 .filter("iu.serviceId = '"+id+"'")
														 .result("iu.serviceId, p.version")
														 .distinct();
		
		ObjectNode _result = neoRestTemplate.postForObject(neo4jUrl, query.buildMap(), ObjectNode.class);
		ArrayNode dataNode = (ArrayNode) _result.get("data");
		
		List<InstallableUnit> result = new ArrayList<>();
		dataNode.forEach((d) -> result.add(methods.toUnit((ArrayNode) d)));
		
		return result;
	}

	@ApiOperation(value = "List all repositories that contain the installable unit in this version")
	@RequestMapping(method=RequestMethod.GET, value="/{id}/versions/{version}/repositories")
	List<Repository> listRepositoriesForUnitVersion(@PathVariable String id, @PathVariable String version) {
		Neo4JQueryBuilder query = new Neo4JQueryBuilder().match("(r:Repository)-[p:PROVIDES]->(iu:IU)")
				 										 .filter("iu.serviceId = '"+id+"'")
				 										 .filter("p.version = '"+version+"'")
				 										 .result("r.serviceId, r.uri")
				 										 .distinct();
		
		ObjectNode _result = neoRestTemplate.postForObject(neo4jUrl, query.buildMap(), ObjectNode.class);
		ArrayNode dataNode = (ArrayNode) _result.get("data");
		
		List<Repository> result = new ArrayList<>();
		dataNode.forEach((d) -> result.add(methods.toRepository((ArrayNode) d)));
		return result;
	}

	@ApiOperation(value = "List all repositories that contain the installable unit in this version")
	@RequestMapping(method=RequestMethod.GET, value="/{id}/versions/repositories")
	List<RepositoryProvidesVersion> listRepositoriesForUnitVersionRange(@PathVariable String id, @RequestParam(value="minVersion", defaultValue="") String versionLow, @RequestParam(value="maxVersion", defaultValue="") String versionHigh) {
		Neo4JQueryBuilder query = new Neo4JQueryBuilder().match("(r:Repository)-[p:PROVIDES]->(iu:IU)")
														 .filter("iu.serviceId = '"+id+"'")
														 .result("r.serviceId, r.uri, p.version")
														 .distinct();
		if (!versionLow.isEmpty())
			query.filter("p.version > '"+versionLow+"'");
		if (!versionHigh.isEmpty())
			query.filter("p.version < '"+versionHigh+"'");
		
		ObjectNode _result = neoRestTemplate.postForObject(neo4jUrl, query.buildMap(), ObjectNode.class);
		ArrayNode dataNode = (ArrayNode) _result.get("data");
		
		List<RepositoryProvidesVersion> result = new ArrayList<>();
		dataNode.forEach((d) -> result.add(methods.toRepositoryProvidesVersion((ArrayNode) d)));
		return result;
	}
}