package com.itemis.p2.service.model;

import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;

@SuppressWarnings("restriction")
public class IURequirementsInfo {

	private String name;
	private String nameSpace;
	private String description;
	private String versionRange;
//	private int maxCardinality;
//	private int minCardinality;
	private boolean greedy;
	
	public IURequirementsInfo(RequiredCapability req) {
		name = req.getName();
		nameSpace = req.getNamespace();
		description = req.getDescription();
		versionRange = req.getRange().toString();
//		maxCardinality = req.getMax();
//		minCardinality = req.getMin();
		greedy = req.isGreedy();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNameSpace() {
		return nameSpace;
	}

	public void setNameSpace(String nameSpace) {
		this.nameSpace = nameSpace;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getVersionRange() {
		return versionRange;
	}

	public void setVersionRange(String versionRange) {
		this.versionRange = versionRange;
	}

//	public int getMaxCardinality() {
//		return maxCardinality;
//	}
//
//	public void setMaxCardinality(int maxCardinality) {
//		this.maxCardinality = maxCardinality;
//	}
//
//	public int getMinCardinality() {
//		return minCardinality;
//	}
//
//	public void setMinCardinality(int minCardinality) {
//		this.minCardinality = minCardinality;
//	}

	public boolean isGreedy() {
		return greedy;
	}

	public void setGreedy(boolean greedy) {
		this.greedy = greedy;
	}

}
