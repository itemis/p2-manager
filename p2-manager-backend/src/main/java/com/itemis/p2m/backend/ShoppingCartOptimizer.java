package com.itemis.p2m.backend;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itemis.p2m.backend.model.InstallableUnit;
import com.itemis.p2m.backend.model.Repository;

@Component
public class ShoppingCartOptimizer {
	@Value("${url.queryservice}")
	private String queryserviceUrl;	
	@Value("${url.neo4j.cypher}")
	private String neo4jUrl;	
	
	private RestTemplate neoRestTemplate;
	
	public ShoppingCartOptimizer(@Qualifier("neoRestTemplateBean") RestTemplate neoRestTemplate) {
		this.neoRestTemplate = neoRestTemplate;
	}
	
	/**
	 * Calculates a list of repositories so that all requested units in the requested version
	 * are contained in at least one repository.
	 * 
	 * @param units The units that should be available in the repositories.
	 * @return The list of repositories from which all units can be retrieved.
	 */
	public List<Repository> getRepositoryList(List<InstallableUnit> units) {
		// trivial implementation: for all units -> retrieve list of all repositories that have that unit -> pick one
		//TODO: smarter implementation that compiles a minimal list of repositories
		
		List<Repository> result = new ArrayList<>();
		
		units.forEach(u -> {
			Neo4JQueryBuilder query = new Neo4JQueryBuilder().match("(r)-[p:PROVIDES]->(u)")
					 										 .filter("u.serviceId = '"+u.getUnitId()+"'")
					 										 .filter("p.version = '"+u.getVersion()+"'")
					 										 .result("r.serviceId, r.uri")
					 										 .distinct();
			
			ObjectNode _result = neoRestTemplate.postForObject(neo4jUrl, query.buildMap(), ObjectNode.class);
			ArrayNode dataNode = (ArrayNode) _result.get("data");
			
			result.add(new Repository((ArrayNode)dataNode.get(0)));
		});

		return result;
	}
}
