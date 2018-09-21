package com.itemis.p2m.backend.rest.repository;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itemis.p2m.backend.exceptions.NothingToLoadException;
import com.itemis.p2m.backend.model.InstallableUnit;
import com.itemis.p2m.backend.model.Repository;
import com.itemis.p2m.backend.util.Neo4JQueryBuilder;

/**
 * Unit tests for the QueryServiceHandler class.
 */
public class RepositoryHandlerTest {
	
	public RepositoryHandler repositoryHandler = new RepositoryHandler();
	public JsonNodeFactory jsonNodeFactory = new JsonNodeFactory(false);
	
	@Test
	public void unitsAreParsedFromShoppingCart() {
		List<InstallableUnit> expected = Arrays.asList(
				new InstallableUnit("org.group.unit.id", "1.0.0"),
				new InstallableUnit("com.group.anotherunit", "2.3.5-SNAPSHOT")
		);
		String[] shoppingCart = expected.stream().map(InstallableUnit::toString).collect(Collectors.toList()).toArray(new String[0]);
		
		List<InstallableUnit> result = repositoryHandler.parseInstallableUnitsFromShoppingCart(shoppingCart);
		
		assertArrayEquals(expected.toArray(), result.toArray());
	}
	
	@Test
	public void unitsAreParsedFromEmptyShoppingCart() {
		List<InstallableUnit> expected = Collections.emptyList();
		String[] shoppingCart = new String[0];
		
		List<InstallableUnit> result = repositoryHandler.parseInstallableUnitsFromShoppingCart(shoppingCart);
		
		assertArrayEquals(expected.toArray(), result.toArray());
	}
	
	@Test
	public void basicListRepositoryQueryIsCreated() {
		String expected = "MATCH (r:Repository) RETURN DISTINCT r.serviceId,r.uri ORDER BY r.uri SKIP 5 LIMIT 3";
		
		Neo4JQueryBuilder builder = repositoryHandler.createListRepositoryQuery("3", "5", null, false);
		
		assertEquals(expected, builder.toString());
	}
	
	@Test
	public void listRepositoryQueryWithSearchTermsIsCreated() {
		String expected = "MATCH (r:Repository) WHERE r.uri CONTAINS 'oneSearchTerm' AND r.uri CONTAINS 'anotherOne' RETURN DISTINCT r.serviceId,r.uri ORDER BY r.uri";
		
		String[] searchTerms = {"oneSearchTerm", "anotherOne"};
		Neo4JQueryBuilder builder = repositoryHandler.createListRepositoryQuery("0", "0", searchTerms, false);
		
		assertEquals(expected, builder.toString());
	}
	
	@Test
	public void listRepositoryQueryWithTopLevelOnlyIsCreated() {
		String expected = "MATCH (r:Repository) WHERE size(()-[:PARENT_OF]->(r)) = 0 RETURN DISTINCT r.serviceId,r.uri ORDER BY r.uri";
		
		Neo4JQueryBuilder builder = repositoryHandler.createListRepositoryQuery("0", "0", null, true);
		
		assertEquals(expected, builder.toString());
	}
	
	@Test
	public void getRepositoryURIQueryIsCreated() {
		String expected = "MATCH (r:Repository) WHERE r.serviceId = '5' RETURN DISTINCT r.serviceId,r.uri";
		
		Neo4JQueryBuilder builder = repositoryHandler.createGetRepositoryURIQuery(5);
		
		assertEquals(expected, builder.toString());
	}
	
	@Test
	public void listUnitsInRepositoryQueryIsCreated() {
		String expected = "MATCH (r:Repository)-[p:PROVIDES]->(iu:IU) WHERE r.serviceId = '5' RETURN DISTINCT iu.serviceId,p.version";
		
		Neo4JQueryBuilder builder = repositoryHandler.createListUnitsInRepositoryQuery(5);
		
		assertEquals(expected, builder.toString());
	}
	
	@Test
	public void listChildrenQueryIsCreated() {
		String expected = "MATCH (r:Repository) -[]-> (c:Repository) WHERE r.serviceId = '5' RETURN DISTINCT c.serviceId,c.uri ORDER BY c.uri";
		
		Neo4JQueryBuilder builder = repositoryHandler.createListChildrenQuery(5);
		
		assertEquals(expected, builder.toString());
	}
	
	@Test
	public void repositoryListIsExtractedFromDatabaseResponse() {
		List<Repository> expected = Arrays.asList(
				new Repository(4, "org.some.repo"),
				new Repository(7, "com.repo.another")
		);

		ObjectNode databaseResponse = jsonNodeFactory.objectNode();
		ArrayNode dataContent = jsonNodeFactory.arrayNode();
		databaseResponse.set("data", dataContent);
		
		for (Repository repo : expected) {
			dataContent.add(jsonNodeFactory.arrayNode().add(repo.getRepoId()).add(repo.getUri()));
		};
		
		List<Repository> result = repositoryHandler.extractRepositoryListFromDatabaseResponse(databaseResponse);
		
		assertArrayEquals(expected.toArray(), result.toArray());
	}
	
	@Test(expected=NothingToLoadException.class)
	public void nothingToLoadExceptionOnEmptyDatabaseResponse() {
		ObjectNode databaseResponse = jsonNodeFactory.objectNode();
		ArrayNode dataContent = jsonNodeFactory.arrayNode();
		databaseResponse.set("data", dataContent);
		
		repositoryHandler.extractRepositoryListFromDatabaseResponse(databaseResponse);
	}
	
	@Test
	public void repositoryURIIsExtractedFromDatabaseResponse() {
		Repository expected = new Repository(4, "org.some.repo");

		ObjectNode databaseResponse = jsonNodeFactory.objectNode();
		ArrayNode dataContent = jsonNodeFactory.arrayNode();
		databaseResponse.set("data", dataContent);
		
		dataContent.add(jsonNodeFactory.arrayNode().add(expected.getRepoId()).add(expected.getUri()));
		
		Repository result = repositoryHandler.extractRepositoryURIFromDatabaseResponse(databaseResponse);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void unitListForRepositoryIsExtractedFromDatabaseResponse() {
		List<InstallableUnit> expected = Arrays.asList(
				new InstallableUnit("org.some.unit", "1.2.3"),
				new InstallableUnit("com.another.unit", "5.0.0")
		);

		ObjectNode databaseResponse = jsonNodeFactory.objectNode();
		ArrayNode dataContent = jsonNodeFactory.arrayNode();
		databaseResponse.set("data", dataContent);
		
		for (InstallableUnit iu : expected) {
			dataContent.add(jsonNodeFactory.arrayNode().add(iu.getUnitId()).add(iu.getVersion()));
		};
		
		List<InstallableUnit> result = repositoryHandler.extractUnitListForRepositoryFromDatabaseResponse(databaseResponse);

		assertArrayEquals(expected.toArray(), result.toArray());
	}
	
	@Test
	public void childRepositoriesAreExtractedFromDatabaseResponse() {
		List<Repository> expected = Arrays.asList(
				new Repository(4, "org.some.repo"),
				new Repository(7, "com.repo.another")
		);

		ObjectNode databaseResponse = jsonNodeFactory.objectNode();
		ArrayNode dataContent = jsonNodeFactory.arrayNode();
		databaseResponse.set("data", dataContent);
		
		for (Repository repo : expected) {
			dataContent.add(jsonNodeFactory.arrayNode().add(repo.getRepoId()).add(repo.getUri()));
		};
		
		List<Repository> result = repositoryHandler.extractChildRepositoriesFromDatabaseResponse(databaseResponse);
		
		assertArrayEquals(expected.toArray(), result.toArray());
	}
	
	
}
