package com.itemis.p2m.backend.util;

import java.util.Collections;
import java.util.Map;
import java.util.StringJoiner;

import com.google.common.base.Preconditions;

public class Neo4JQueryBuilder {
	
	class QueryParts {
		private StringJoiner filters = new StringJoiner(" AND ");
		private String matchPattern = "";
		private String createPattern = "";
		private String returnPattern = "";
		private String orderByProperty = "";
		private String limit = "";
		private String skipAmount = "";
		private boolean isDistinct = false;
	}
	
	private QueryParts parts = new QueryParts();

	public Neo4JQueryBuilder filterContains(String property, String substring) {
		Preconditions.checkNotNull(property);
		Preconditions.checkNotNull(substring);
		return this.filter(property+" CONTAINS '"+substring+"'");
	}
	
	public Neo4JQueryBuilder filter(String condition) {
		Preconditions.checkNotNull(condition);
		parts.filters.add(condition);
		return this;
	}
	
	public Neo4JQueryBuilder match(String pattern) {
		Preconditions.checkNotNull(pattern);
		parts.matchPattern = pattern;
		return this;
	}
	
	public Neo4JQueryBuilder create(String pattern) {
		Preconditions.checkNotNull(pattern);
		parts.createPattern = pattern;
		return this;
	}
	
	public Neo4JQueryBuilder result(String pattern) {
		Preconditions.checkNotNull(pattern);
		parts.returnPattern = pattern;
		return this;
	}
	
	public Neo4JQueryBuilder orderBy(String property) {
		Preconditions.checkNotNull(property);
		parts.orderByProperty = property;
		return this;
	}
	
	public Neo4JQueryBuilder distinct() {
		parts.isDistinct = true;
		return this;
	}
	
	public Neo4JQueryBuilder limit(String limit, String offset) {
		int parsedLimit = parseLimitArgument(limit);
		int parsedOffset = parseLimitArgument(offset);
		parts.limit = parsedLimit == 0 ? "" : " LIMIT "+limit;
		parts.skipAmount = parsedOffset == 0 ? "" : " SKIP "+offset;
		return this;
	}

	private int parseLimitArgument(String input) {
		Preconditions.checkNotNull(input);
		int result;
		try {
			result = Integer.parseInt(input);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(String.format("{} is not a valid integer number!", input));
		}
		if (result < 0) {
			throw new IllegalArgumentException(String.format("{} is not a positive integer number!", input));
		}
		return result;
	}

	@Override
	public String toString() {		
		return this.matchClause()
			 + this.whereClause()
			 + this.createClause()
			 + this.returnClause();
	}
	
	public Map<String, String> buildMap() {
		if (parts.matchPattern.equals("")) {
			throw new IllegalStateException("A match must be set via Neo4JQueryBuilder::match before the query can be built!");
		}
		
		return Collections.singletonMap("query", this.toString());
	}
	
	public Neo4JQueryBuilder reset() {
		parts = new QueryParts();
		return this;
	}
	
	private String matchClause() {
		if (parts.matchPattern.equals("")) {
			return "";
		}
		return "MATCH "+parts.matchPattern;
	}
	
	private String createClause() {
		if (parts.createPattern.equals("")) {
			return "";
		}
		return " CREATE "+parts.createPattern;
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
		if (parts.returnPattern.equals("")) {
			return "";
		} else {
			StringBuilder clause = new StringBuilder();
			clause.append(" RETURN ");
			if (parts.isDistinct) {
				clause.append("DISTINCT ");
			}
			clause.append(parts.returnPattern);
			if (!parts.orderByProperty.equals("")) {
				clause.append(" ORDER BY ");
				clause.append(parts.orderByProperty);
			}
			clause.append(parts.skipAmount);
			clause.append(parts.limit);
			return clause.toString();
		}
	}
}
