package com.itemis.p2m.backend;

import java.util.Collections;
import java.util.Map;
import java.util.StringJoiner;

public class Neo4JQueryBuilder {
	
	class QueryParts {
		private StringJoiner filters = new StringJoiner(" AND ");
		private String matchPattern = "";
		private String returnPattern = "";
		private String orderByProperty = "";
		private String limit = "";
		private String skipAmount = "";
		private boolean isDistinct = false;
	}
	
	private QueryParts parts = new QueryParts();

	public Neo4JQueryBuilder filterContains(String property, String substring) {
		return this.filter(property+" CONTAINS '"+substring+"'");
	}
	
	public Neo4JQueryBuilder filter(String condition) {
		parts.filters.add(condition);
		return this;
	}
	
	public Neo4JQueryBuilder match(String pattern) {
		parts.matchPattern = pattern;
		return this;
	}
	
	public Neo4JQueryBuilder result(String pattern) {
		parts.returnPattern = pattern;
		return this;
	}
	
	public Neo4JQueryBuilder orderBy(String property) {
		parts.orderByProperty = property;
		return this;
	}
	
	public Neo4JQueryBuilder distinct() {
		parts.isDistinct = true;
		return this;
	}
	
	public Neo4JQueryBuilder limit(String limit, String offset) {
		parts.limit = Integer.parseInt(limit) <= 0 ? "" : " LIMIT "+limit;
		parts.skipAmount = Integer.parseInt(offset) <= 0 ? "" : " SKIP "+offset;
		return this;
	}

	@Override
	public String toString() {		
		return this.matchClause()
			 + this.whereClause()
			 + this.returnClause();
	}
	
	public Map<String, String> buildMap() {
		if (parts.matchPattern == null) {
			throw new IllegalStateException("A match must be set via Neo4JQueryBuilder::match before the query can be built!");
		}
		
		return Collections.singletonMap("query", this.toString());
	}
	
	public Neo4JQueryBuilder reset() {
		parts = new QueryParts();
		return this;
	}
	
	private String matchClause() {
		if (parts.matchPattern == null || parts.matchPattern.equals("")) {
			return "";
		}
		return "MATCH "+parts.matchPattern;
	}
	
	private String whereClause() {
		String filter = parts.filters.toString();
		if (filter.equals("")) {
			return "";
		} else {
			return " WHERE "+filter;
		}
		
	}
	
	private String returnClause() {
		if (parts.returnPattern == null || parts.returnPattern.equals("")) {
			return "";
		} else {
			StringBuilder clause = new StringBuilder();
			clause.append(" RETURN ");
			if (parts.isDistinct) {
				clause.append("DISTINCT ");
			}
			clause.append(parts.returnPattern);
			if (parts.orderByProperty != null && !parts.orderByProperty.equals("")) {
				clause.append(" ORDER BY ");
				clause.append(parts.orderByProperty);
			}
			clause.append(parts.skipAmount);
			clause.append(parts.limit);
			return clause.toString();
		}
	}
}
