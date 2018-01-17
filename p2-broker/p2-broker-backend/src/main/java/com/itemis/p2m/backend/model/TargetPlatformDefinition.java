package com.itemis.p2m.backend.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

public class TargetPlatformDefinition {
	
	private int tpdId;
	private String name;
	private Map<String, Set<Pair<String, String>>> repo2containedUnitVersions = new HashMap<>();
	
	public void addUnitVersion(String repositoryUrl, String unitId, String version) {
		if (repo2containedUnitVersions.get(repositoryUrl) == null) {
			repo2containedUnitVersions.put(repositoryUrl, new HashSet<>());
		}
		Set<Pair<String, String>> unitVersions = repo2containedUnitVersions.get(repositoryUrl);
		unitVersions.add(new Pair<String, String>(unitId,  version));
	}
	
	public int getTpdId() {
		return tpdId;
	} 
	
	public void setTpdId(int tpdId) {
		this.tpdId = tpdId;
	}
	
	public String getName() {
		return name;
	} 
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Set<String> getRepositories() {
		return repo2containedUnitVersions.keySet();
	}
	
	public Set<Pair<String, String>> getUnitVersionsForRepository(String repository) {
		return repo2containedUnitVersions.get(repository);
	}
}
