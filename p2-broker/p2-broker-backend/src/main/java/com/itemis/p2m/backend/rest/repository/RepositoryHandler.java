package com.itemis.p2m.backend.rest.repository;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itemis.p2m.backend.exceptions.NothingToLoadException;
import com.itemis.p2m.backend.model.InstallableUnit;
import com.itemis.p2m.backend.model.Repository;
import com.itemis.p2m.backend.util.Neo4JQueryBuilder;

import io.swagger.annotations.ApiOperation;

@Service
public class RepositoryHandler {
	
	public List<InstallableUnit> parseInstallableUnitsFromShoppingCart(String[] shoppingCart) {
		List<InstallableUnit> units = new ArrayList<>();
		
		for(String item : shoppingCart) {
			String[] parsedItem = item.split(" ");
			units.add(new InstallableUnit(parsedItem[0], parsedItem[1]));
		}
		
		return units;
	}
	
	public Neo4JQueryBuilder createListRepositoryQuery(String limit, String offset, String[] searchTerm, boolean topLevelOnly) {
		Neo4JQueryBuilder query = new Neo4JQueryBuilder().match("(r:Repository)")
														 .result("r.serviceId,r.uri")
														 .distinct()
														 .orderBy("r.uri")
														 .limit(limit, offset);
		
		if (searchTerm != null) {
			for (String term : searchTerm) {
				query.filterContains("r.uri", term);
			}
		}
		
		if (topLevelOnly) {
			query.filter("size(()-[:PARENT_OF]->(r)) = 0");
		}
		
		return query;
	}
	
	public Neo4JQueryBuilder createGetRepositoryURIQuery(Integer id) {
		return new Neo4JQueryBuilder().match("(r:Repository)")
									  .filter("r.serviceId = '"+id+"'")
									  .result("r.serviceId,r.uri")
									  .distinct();
	}
	
	public Neo4JQueryBuilder createListUnitsInRepositoryQuery(Integer id) {
		return new Neo4JQueryBuilder().match("(r:Repository)-[p:PROVIDES]->(iu:IU)")
									  .filter("r.serviceId = '"+id+"'")
									  .result("iu.serviceId,p.version")
									  .distinct();
	}
	
	public Neo4JQueryBuilder createListChildrenQuery(Integer id) {
		return new Neo4JQueryBuilder().match("(r:Repository) -[]-> (c:Repository)")
									  .filter("r.serviceId = '"+id+"'")
									  .result("c.serviceId,c.uri")
									  .orderBy("c.uri")
									  .distinct();
	}
	
	public List<Repository> extractRepositoryListFromDatabaseResponse(ObjectNode databaseResponse) {
		List<Repository> result = new ArrayList<>();

		ArrayNode dataNode = extractDataNode(databaseResponse);
		dataNode.forEach(node -> result.add(new Repository((ArrayNode)node)));
		
		if (result.size() == 0)
			throw new NothingToLoadException();
		
		return result;
	}
	
	private ArrayNode extractDataNode(ObjectNode databaseResponse) {
		return (ArrayNode) databaseResponse.get("data");
	}
	
	public Repository extractRepositoryURIFromDatabaseResponse(ObjectNode databaseResponse) {
		ArrayNode dataNode = extractDataNode(databaseResponse);
		return new Repository((ArrayNode)dataNode.get(0));
	} 
	
	public List<InstallableUnit> extractUnitListForRepositoryFromDatabaseResponse(ObjectNode databaseResponse) {
		List<InstallableUnit> result = new ArrayList<>();
		
		ArrayNode dataNode = extractDataNode(databaseResponse);
		dataNode.forEach((d) -> result.add(new InstallableUnit((ArrayNode) d)));
		
		return result;
	} 
	
	public List<Repository> extractChildRepositoriesFromDatabaseResponse(ObjectNode databaseResponse) {
		List<Repository> result = new ArrayList<>();
		
		ArrayNode dataNode = extractDataNode(databaseResponse);
		dataNode.forEach((d) -> result.add(new Repository((ArrayNode) d)));
		
		return result;
	} 
}
