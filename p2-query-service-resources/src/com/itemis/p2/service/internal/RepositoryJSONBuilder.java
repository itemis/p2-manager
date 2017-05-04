package com.itemis.p2.service.internal;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.json.*;

import com.ifedorenko.p2browser.model.IGroupedInstallableUnits;

@SuppressWarnings("restriction")
public class RepositoryJSONBuilder {
	
	RepositoryData repositoryData;
	
	public RepositoryJSONBuilder() {
	}
	
	public JSONArray buildJSON(RepositoryData repositoryData) throws JSONException {
		this.repositoryData = repositoryData;
		return addRepositories(new ArrayList<URI>(repositoryData.getRepositories()));
	}
	
	private JSONArray addRepositories(List<URI> repoUris) throws JSONException {
		JSONArray jArray = new JSONArray();
		for (URI uri: repoUris){
			JSONObject jObject = new JSONObject();
			jObject.put("URI", uri);
			addRepositoryDataToJSON(jObject, uri);
			addRepositoryContentToJSON(jObject, uri);
			jArray.put(jObject);
		}
		return jArray;
	}
	
	private void addRepositoryDataToJSON(JSONObject jObject, URI uri) throws JSONException {
		IMetadataRepository metadata = repositoryData.getAllrepositories().get(uri);
		jObject.put("Name", metadata.getName());
		jObject.put("Version",metadata.getVersion());
		jObject.put("Timestamp", metadata.getProperty("p2.timestamp"));
		jObject.put("Compressed", metadata.getProperty("p2.compressed"));
		if(metadata instanceof CompositeMetadataRepository){
			jObject.put("Children", addRepositories(((CompositeMetadataRepository)metadata).getChildren()));
		}
	}
	
	private void addRepositoryContentToJSON(JSONObject jObject, URI uri) throws JSONException {
		IGroupedInstallableUnits content = repositoryData.getRepositoryContent().get(uri);
		if(content != null){
			addIUsToJSON(jObject, "rootIncludedIUs", content.getRootIncludedInstallableUnits());
//			jObject.put("includedIUs", addIUsToJSON(content.getIncludedInstallableUnits(unit, transitive))); I Don´t know whether rootIncludeIUs contains transitive IUs. If not every rootIU have to b checked with this command
			addIUsToJSON(jObject, "IUs", content.getInstallableUnits());
		}
	}
	
	private void addIUsToJSON(JSONObject toFill, String name, Collection<? extends IInstallableUnit> collection) throws JSONException {
		//TODO omitted attributes: copyright, licenses
		JSONArray iuArray = new JSONArray();
		for (IInstallableUnit installableUnit : collection) {
			JSONObject iuObject = new JSONObject();
			addArtifactKeysToJson(iuObject, "artifactKeys", installableUnit.getArtifacts());
			if (installableUnit.getFilter() != null){
				iuObject.put("filterParameters", new JSONArray(Arrays.asList(installableUnit.getFilter().getParameters())));	
			}
			addIUsToJSON(iuObject, "fragments", installableUnit.getFragments()); //TODO if getHost is needed own subclass have to be implemented
			iuObject.put("id", installableUnit.getId());
			addRequirementsToJson(iuObject, "metaRequires", installableUnit.getMetaRequirements());
			iuObject.put("description", installableUnit.getProperty("org.eclipse.equinox.p2.description"));
			iuObject.put("name", installableUnit.getProperty("org.eclipse.equinox.p2.name"));
			addProvidedCapabilitiesToJson(iuObject, "providedCapabilities", installableUnit.getProvidedCapabilities());
			addRequirementsToJson(iuObject, "requires", installableUnit.getRequirements());
			iuObject.put("singelton", installableUnit.isSingleton());
			iuObject.put("version", installableUnit.getVersion().toString());
			iuArray.put(iuObject);
		}
		if (iuArray.length() > 0){
			toFill.put(name, iuArray);
		}
	}
	
	private void addRequirementsToJson(JSONObject toFill, String name, Collection<IRequirement> requirements) throws JSONException {
		JSONArray reqArray = new JSONArray();
		for (IRequirement requirement : requirements){
			JSONObject reqObject = new JSONObject();
			reqObject.put("min", requirement.getMin());
			reqObject.put("max", requirement.getMax());
			reqObject.put("Greedy", requirement.isGreedy());
			reqObject.put("Description", requirement.getDescription());
		}
		if (reqArray.length() > 0){
			toFill.put(name, reqArray);
		}
	}
	
	private void addProvidedCapabilitiesToJson(JSONObject toFill, String name, Collection<IProvidedCapability> providedCapabilities) throws JSONException {
		JSONArray pcArray = new JSONArray();
		for (IProvidedCapability providedCapability : providedCapabilities){
			JSONObject pcObject = new JSONObject();
			pcObject.put("name", providedCapability.getName());
			pcObject.put("namespace", providedCapability.getNamespace());
			pcObject.put("version", providedCapability.getVersion());
		}
		if (pcArray.length() > 0){
			toFill.put(name, pcArray);
		}
	}
	
	private void addArtifactKeysToJson(JSONObject toFill, String name, Collection<IArtifactKey> artifactKeys) throws JSONException {
		JSONArray artifactArray = new JSONArray();
		for (IArtifactKey artifactKey : artifactKeys){
			JSONObject artifactObject = new JSONObject();
			artifactObject.put("classifier", artifactKey.getClassifier());
			artifactObject.put("ID", artifactKey.getId());
			artifactObject.put("version", artifactKey.getVersion());
		}
		if (artifactArray.length() > 0){
			toFill.put(name, artifactArray);
		};
	}
}
