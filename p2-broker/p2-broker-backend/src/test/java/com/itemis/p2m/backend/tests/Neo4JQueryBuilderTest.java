package com.itemis.p2m.backend.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.itemis.p2m.backend.Neo4JQueryBuilder;

public class Neo4JQueryBuilderTest {
	
	private Neo4JQueryBuilder query;
	
	@Before
	public void init() {
		query = new Neo4JQueryBuilder();
	}
	
	@Test
	public void buildCorrectStrings() {
		String expectation = "";
		
		expectation = "MATCH (x) RETURN x";
		query.toString();
		query.reset().match("(x)").result("x");
		assertEquals(expectation, query.toString());
		
		expectation = "MATCH (x) WHERE x.y = 'a' AND x.z = 'b' RETURN DISTINCT x ORDER BY x.y SKIP 3 LIMIT 6";
		query.reset().match("(x)").filter("x.y = 'a'").filter("x.z = 'b'").result("x").orderBy("x.y").limit("6", "3").distinct();
		assertEquals(expectation, query.toString());
		
		expectation = "MATCH (x),(y) WHERE x.z = y.z CREATE (x)-[:ABC]->(y) RETURN x,y,z";
		query.reset().match("(x),(y)").filter("x.z = y.z").create("(x)-[:ABC]->(y)").result("x,y,z");
		assertEquals(expectation, query.toString());
	}
}
