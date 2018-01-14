package com.itemis.p2m.backend.controllers;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itemis.p2m.backend.Neo4JQueryBuilder;
import com.itemis.p2m.backend.model.InstallableUnit;
import com.itemis.p2m.backend.model.Repository;
import com.itemis.p2m.backend.model.TargetPlatformDefinition;
import com.itemis.p2m.backend.services.ShoppingCartOptimizerService;
import com.itemis.p2m.backend.targetplatform.TargetPlatformDefinitionGenerator;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/tpd")
public class TargetPlatformDefinitionController {
	@Value("${url.neo4j.cypher}")
	private String neo4jUrl;

	private RestTemplate neoRestTemplate;
	private ShoppingCartOptimizerService cartOptimizer;

	public TargetPlatformDefinitionController(@Qualifier("neoRestTemplateBean") RestTemplate neoRestTemplate, ShoppingCartOptimizerService cartOptimizer) {
		this.neoRestTemplate = neoRestTemplate;
		this.cartOptimizer = cartOptimizer;
	}

	@ApiOperation(value = "Create a target platform definition")
	@RequestMapping(method=RequestMethod.POST)
	public ResponseEntity<String> addTargetPlatformDefinition(@RequestParam String tpdInfo) throws IOException {
		//TODO: check, whether this exact target platform is already saved in the database; if so, return that one
		Map<InstallableUnit, Repository> unitsAndRepositories = cartOptimizer.getOptimizedRepositoryMap(deserializeUnits(tpdInfo));
		
		Neo4JQueryBuilder query = new Neo4JQueryBuilder().create("(tpd:TPD)")
														 .result("ID(tpd)");
		ObjectNode result = neoRestTemplate.postForObject(neo4jUrl, query.buildMap(), ObjectNode.class);
		int id = result.get("data").get(0).get(0).asInt();
		
		//TODO: add tpd contents to neo4j
		for (InstallableUnit unit : unitsAndRepositories.keySet()) {
			String repositoryUri = unitsAndRepositories.get(unit).getUri();
			String unitId = unit.getUnitId();
			String version = unit.getVersion();
			query.reset()
				 .match("(tpd:TPD),(r:Repository)-[p:PROVIDES]->(iu:IU)")
				 .filter("ID(tpd)="+id)
				 .filter("r.uri=\""+repositoryUri+"\"")
				 .filter("iu.serviceId=\""+unitId+"\"")
				 .filter("p.version=\""+version+"\"")
				 .create("(iu)<-[c:CONSISTS_OF {version: \""+version+"\"}]-(tpd)-[u:USES]->(r)");
			System.out.println(query.buildMap());
			neoRestTemplate.postForObject(neo4jUrl, query.buildMap(), ObjectNode.class);
		}
		
		String tpdAddress = fromMethodName(TargetPlatformDefinitionController.class, "getTargetPlatformDefinition", id, null)
													.build().toUriString();
		HttpHeaders headers = new HttpHeaders();
		headers.add("Location", tpdAddress);
		ResponseEntity<String> response = new ResponseEntity<>(headers, HttpStatus.ACCEPTED);
		return response;
	}

	//TODO: if no tpd with given id exists, return error code NOT_FOUND
	@ApiOperation(value = "Get the target platform definition as a file")
	@RequestMapping(method=RequestMethod.GET, value="/{id}", produces = "text/xml")
	public String getTargetPlatformDefinition(@PathVariable Integer id, @RequestParam(required = false) String name) {
		Neo4JQueryBuilder query = new Neo4JQueryBuilder().match("(tpd:TPD)-[:USES]->(r)-[p:PROVIDES]->(iu)<-[c:CONSISTS_OF]-(tpd:TPD)")
				 										 .filter("ID(tpd)="+id)
														 .filter("c.version=p.version")
														 .result("r.uri, iu.serviceId, p.version")
														 .distinct();
		ObjectNode result = neoRestTemplate.postForObject(neo4jUrl, query.buildMap(), ObjectNode.class);
		ArrayNode dataNode = (ArrayNode) result.get("data");
		
		TargetPlatformDefinition tpd = new TargetPlatformDefinition();
		tpd.setTpdId(id);
		tpd.setName(name);

		for (int i = 0; i < dataNode.size(); i++) {
			ArrayNode unitVersion = (ArrayNode) dataNode.get(i);
			tpd.addUnitVersion(unitVersion.get(0).asText(), unitVersion.get(1).asText(), unitVersion.get(2).asText());
		}
		
		return TargetPlatformDefinitionGenerator.generateTPD(tpd);
	}
	
	private List<InstallableUnit> deserializeUnits(String unitString) throws IOException {
		List<InstallableUnit> result = new ArrayList<>();
		ArrayNode parsedUnitString = (ArrayNode)new ObjectMapper().readValue(unitString, ArrayNode.class);
		
		for (JsonNode unit : parsedUnitString) {
			String unitId = unit.get("unitId").asText();
			String version = unit.get("version").asText();
			result.add(new InstallableUnit(unitId, version));
		}
		return result;
	}
}
