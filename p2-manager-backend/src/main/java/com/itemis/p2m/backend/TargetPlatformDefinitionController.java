package com.itemis.p2m.backend;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.io.IOException;
import java.net.URI;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itemis.p2m.backend.model.TargetPlatformDefinition;
import com.itemis.p2m.backend.targetplatform.TargetPlatformDefinitionGenerator;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/tpd")
public class TargetPlatformDefinitionController {
	@Value("${url.neo4j.cypher}")
	private String neo4jUrl;

	private RestTemplate neoRestTemplate;	

	public TargetPlatformDefinitionController(@Qualifier("neoRestTemplateBean") RestTemplate neoRestTemplate) {
		this.neoRestTemplate = neoRestTemplate;
	}

	@ApiOperation(value = "Create a target platform definition")
	@RequestMapping(method=RequestMethod.POST)
	public ResponseEntity<String> addTargetPlatformDefinition(@RequestParam String tpdInfo) throws IOException {
		
		Neo4JQueryBuilder query = new Neo4JQueryBuilder().create("(tpd:TPD)")
														 .result("ID(tpd)");
		ObjectNode result = neoRestTemplate.postForObject(neo4jUrl, query.buildMap(), ObjectNode.class);
		int id = result.get("data").get(0).get(0).asInt();
		
		//TODO: add tpd contents to neo4j
		ArrayNode parsedTpdInfo = (ArrayNode)new ObjectMapper().readValue(tpdInfo, ArrayNode.class);
		for (int i = 0; i < parsedTpdInfo.size(); i++) {
			ArrayNode unitVersion = (ArrayNode) parsedTpdInfo.get(i);
			String repositoryUri = unitVersion.get(0).asText();
			String unit = unitVersion.get(1).asText();
			String version = unitVersion.get(2).asText();
			query.reset()
				 .match("(tpd:TPD),(r:Repository)-[p:PROVIDES]->(iu:IU)")
				 .filter("ID(tpd)="+id)
				 .filter("r.uri=\""+repositoryUri+"\"")
				 .filter("iu.serviceId=\""+unit+"\"")
				 .filter("p.version=\""+version+"\"")
				 .create("(iu)<-[c:CONSISTS_OF {version: \""+version+"\"}]-(tpd)-[u:USES]->(r)");
			System.out.println(query.buildMap());
			neoRestTemplate.postForObject(neo4jUrl, query.buildMap(), ObjectNode.class);
		}
		
		String tpdAddress = MvcUriComponentsBuilder.fromMethodName(TargetPlatformDefinitionController.class, "getTargetPlatformDefinition", id, null)
													.build().toUriString();
		HttpHeaders headers = new HttpHeaders();
		headers.add("location", tpdAddress);
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
}
