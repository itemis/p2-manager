package com.itemis.p2m.backend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itemis.p2m.backend.model.InstallableUnit;
import com.itemis.p2m.backend.model.Repository;

//TODO: write tests for this class

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
		return new ArrayList<>(this.greedilyApproximatedOptimization(units));
//		return this.simpleOptimization(units);
	}
	
	public List<Repository> simpleOptimization(List<InstallableUnit> units) {
		// trivial implementation: for all units -> retrieve list of all repositories that have that unit -> pick one
		
		List<Repository> result = new ArrayList<>();
		
		units.forEach(u -> {
			Neo4JQueryBuilder query = new Neo4JQueryBuilder().match("(r)-[p:PROVIDES]->(u)")
					 										 .filter("u.serviceId = '"+u.getUnitId()+"'")
					 										 .filter("p.version = '"+u.getVersion()+"'")
					 										 .result("r.serviceId, r.uri")
					 										 .distinct();
			
			ObjectNode _result = neoRestTemplate.postForObject(neo4jUrl, query.buildMap(), ObjectNode.class);
			ArrayNode dataNode = (ArrayNode) _result.get("data");
			
			boolean unitIsAlreadyCovered = false;
			for (JsonNode n : dataNode) {
				Repository repo = new Repository((ArrayNode)n);
				if (result.stream().anyMatch(r -> r.getUri().equals(repo.getUri()))) {
					unitIsAlreadyCovered = true;
				}
			};
			if (!unitIsAlreadyCovered) {
				result.add(new Repository((ArrayNode)dataNode.get(0)));
			}
		});

		return result;
	}

	//TODO: write tests for this method (!!!)
	public Set<Repository> greedilyApproximatedOptimization(List<InstallableUnit> units) {
		//approximation: greedy, take whatever repo covers most units, iterate until all units are covered
		
		Set<Repository> result = new HashSet<>();
		Map<Repository, List<InstallableUnit>> unitsInRepository = new HashMap<>();
		Set<Repository> allRepositories = new HashSet<>();
		calculateUnitsInRepository(units, unitsInRepository, allRepositories);
		
		// calculate the result list
		int repositoryCount = allRepositories.size();
		for (int i = 0; i < repositoryCount; i++) {
			Repository greedyChoice = chooseRepoWithHighestCoverage(unitsInRepository);
			result.add(greedyChoice);

			cleanupForNextIteration(units, unitsInRepository, greedyChoice);
			
			// stop early if all units are covered
			if (units.size() == 0) {
				break;
			}
		}
		
		if (units.size() != 0) {
			throw new RuntimeException(String.format("Unit %s in version %s is not contained in any known repository.", units.get(0).getUnitId(), units.get(0).getVersion()));
		}
		
		return result;
	}
	
	
	protected void calculateUnitsInRepository(List<InstallableUnit> units, Map<Repository, List<InstallableUnit>> unitsInRepository, Set<Repository> allRepositories) {
		units.forEach(u -> {
			Neo4JQueryBuilder query = new Neo4JQueryBuilder().match("(r)-[p:PROVIDES]->(u)")
					 										 .filter("u.serviceId = '"+u.getUnitId()+"'")
					 										 .filter("p.version = '"+u.getVersion()+"'")
					 										 .result("r.serviceId, r.uri")
					 										 .distinct();
			
			ObjectNode _result = neoRestTemplate.postForObject(neo4jUrl, query.buildMap(), ObjectNode.class);
			ArrayNode dataNode = (ArrayNode) _result.get("data");

			List<Repository> repositoriesForUnit = new ArrayList<>();
			for (JsonNode n : dataNode) {
				Repository repo = new Repository((ArrayNode)n);
				repositoriesForUnit.add(repo);
				allRepositories.add(repo);
				if (unitsInRepository.get(repo) == null) {
					unitsInRepository.put(repo, new ArrayList<>());
				}
				
				unitsInRepository.get(repo).add(u);
			};
		});
	}
	
	protected Repository chooseRepoWithHighestCoverage(Map<Repository, List<InstallableUnit>> unitsInRepository) {
		return unitsInRepository.entrySet().stream()
										   .reduce((entry1, entry2) -> entry1.getValue().size() >= entry2.getValue().size() ? entry1 : entry2)
										   .orElseThrow(() -> new RuntimeException("Error while calculating maximum of list; this should not happen."))
										   .getKey();
	}
	
	protected void cleanupForNextIteration(List<InstallableUnit> units, Map<Repository, List<InstallableUnit>> unitsInRepository, Repository chosenRepository) {
		List<InstallableUnit> coveredUnits = unitsInRepository.get(chosenRepository);
		units.removeAll(coveredUnits);
		unitsInRepository.remove(chosenRepository);
		for (List<InstallableUnit> unitsInRepo : unitsInRepository.values()) {
			unitsInRepo.removeAll(coveredUnits);
		}
	}
}
