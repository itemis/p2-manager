package com.itemis.p2m.backend;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.itemis.p2m.backend.util.Neo4JQueryBuilder;

public class Neo4JQueryBuilderTest {
	
	private Neo4JQueryBuilder query;
	
	@Before
	public void init() {
		query = new Neo4JQueryBuilder();
	}
	
	@Test(expected=IllegalStateException.class)
	public void buildMapRequiredMatch() {
		query.buildMap();
	}
	
	@Test
	public void builtMapContainsQueryString() {
		query.match("(x)").result("x");
		String expectation = query.toString();
		assertEquals(expectation, query.buildMap().get("query"));
	}
	
	@Test
	public void buildBasicMatchQuery() {
		String expectation = "MATCH (x) RETURN x";
		query.match("(x)").result("x");
		assertEquals(expectation, query.toString());
	}
	
	@Test
	public void buildQueryWithContainsFilter() {
		String expectation = "MATCH (x) WHERE x CONTAINS 'someString' RETURN x";
		query.match("(x)").result("x").filterContains("x", "someString");
		assertEquals(expectation, query.toString());
	}
	
	@Test
	public void buildQueryWithTwoFiltersAndOrderByAndLimitAndDistinct() {
		String expectation = "MATCH (x) WHERE x.y = 'a' AND x.z = 'b' RETURN DISTINCT x ORDER BY x.y SKIP 3 LIMIT 6";
		query.match("(x)").filter("x.y = 'a'").filter("x.z = 'b'").result("x").orderBy("x.y").limit("6", "3").distinct();
		assertEquals(expectation, query.toString());
	}
	
	@Test
	public void buildQueryWithEmptyArguments() {
		String expectation = "";
		query.match("").filter("").result("").orderBy("").limit("0", "0");
		assertEquals(expectation, query.toString());
	}
	
	@Test
	public void queryWithoutResultHasNoOrderByAndLimitAndSkipAndDistinct() {
		String expectation = "MATCH (x)";
		query.match("(x)").orderBy("x.y").limit("6", "3").distinct();
		assertEquals(expectation, query.toString());
	}
	
	@Test
	public void buildQueryWithFilterAndCreate() {
		String expectation = "MATCH (x),(y) WHERE x.z = y.z CREATE (x)-[:ABC]->(y) RETURN x,y,z";
		query.match("(x),(y)").filter("x.z = y.z").create("(x)-[:ABC]->(y)").result("x,y,z");
		assertEquals(expectation, query.toString());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void passFalselyFormattedStringToLimitMethod() {
		query.limit("a", "3");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void passNegativeNumberToLimitMethod() {
		query.limit("3", "-3");
	}
	
	@Test
	public void resetClearsAllPreviousMethodCalls() {
		String expectation = "";
		query.match("(x)").filter("x.y = 'a'").filter("x.z = 'b'").result("x").orderBy("x.y").limit("6", "3").distinct().reset();
		assertEquals(expectation, query.toString());
	}
}
