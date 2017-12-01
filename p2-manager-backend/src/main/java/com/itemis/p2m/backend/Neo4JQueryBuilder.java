package com.itemis.p2m.backend;

import java.util.StringJoiner;

// TODO write tests for this
public class Neo4JQueryBuilder {
	
	private StringJoiner filters = new StringJoiner(" AND ");
	private String matchPattern;
	private String returnPattern;
	private String orderByProperty;
	private String limit;
	private String skipAmount;
	
	private boolean isDistinct = false;

	public Neo4JQueryBuilder filterContains(String property, String substring) {
		return this.filter(property+" CONTAINS '"+substring+"'");
	}
	
	public Neo4JQueryBuilder filter(String condition) {
		this.filters.add(condition);
		return this;
	}
	
	public Neo4JQueryBuilder match(String pattern) {
		this.matchPattern = pattern;
		return this;
	}
	
	public Neo4JQueryBuilder result(String pattern) {
		this.returnPattern = pattern;
		return this;
	}
	
	public Neo4JQueryBuilder orderBy(String property) {
		this.orderByProperty = property;
		return this;
	}
	
	public Neo4JQueryBuilder distinct() {
		this.isDistinct = true;
		return this;
	}
	
	public Neo4JQueryBuilder limit(String limit, String offset) {
		this.limit = Integer.parseInt(limit) <= 0 ? "" : " LIMIT "+limit;
		this.skipAmount = Integer.parseInt(offset) <= 0 ? "" : " SKIP "+offset;
		return this;
	}

	public String build() {
		if (matchPattern == null) {
			throw new IllegalStateException("A match must be set via Neo4JQueryBuilder::match before the query can be built!");
		}
		
		return this.matchClause()
			 + this.whereClause()
			 + this.returnClause();
	}
	
	private String matchClause() {
		return "MATCH "+this.matchPattern;
	}
	
	private String whereClause() {
		String filter = filters.toString();
		if (filter.equals("")) {
			return "";
		} else {
			return " WHERE "+filter;
		}
		
	}
	
	private String returnClause() {
		if (returnPattern == null || returnPattern.equals("")) {
			return "";
		} else {
			StringBuilder clause = new StringBuilder();
			clause.append(" RETURN ");
			if (isDistinct) {
				clause.append("DISTINCT ");
			}
			clause.append(returnPattern);
			if (orderByProperty != null && !orderByProperty.equals("")) {
				clause.append(" ORDER BY ");
				clause.append(orderByProperty);
			}
			clause.append(skipAmount);
			clause.append(limit);
			return clause.toString();
		}
	}
}
