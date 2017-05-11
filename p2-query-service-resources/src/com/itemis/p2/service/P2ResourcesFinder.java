package com.itemis.p2.service;

import java.net.URI;

import org.json.JSONArray;

import com.itemis.p2.service.internal.LoadRepositoryJob;
import com.itemis.p2.service.internal.RepositoryJSONBuilder;

public class P2ResourcesFinder{
	
	private RepositoryJSONBuilder jsonBuilder;
	
	public P2ResourcesFinder() {
		jsonBuilder = new RepositoryJSONBuilder(); //$NON-NLS-1$
	}

	public String find(URI uri) {
		IRepositoryData repositoryData;
		try {
			repositoryData = P2ResourcesActivator.getDefault().getRepositoryData();
			LoadRepositoryJob job = new LoadRepositoryJob(uri, repositoryData);
			job.schedule();
			job.join();
			JSONArray resultJSON = jsonBuilder.buildJSON(repositoryData);
			return resultJSON.toString();
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
//	public List<URI> URIListConverter(String value) {
//		return Arrays.asList(value.split("\\s*,\\s*")).parallelStream().map((String uri) -> {
//			try {
//				return new URI(uri);
//			} catch (URISyntaxException e) {
//				throw new RuntimeException(e);
//			}
//		}).collect(Collectors.toList());
//	}

}
