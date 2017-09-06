package com.itemis.p2m.backend;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itemis.p2m.backend.constants.RepositoryStatus;
import com.itemis.p2m.backend.model.InstallableUnit;
import com.itemis.p2m.backend.model.Repository;

@Controller
public class RepositoryController {
	@Value("${url.queryservice}")
	private String queryserviceUrl;	
	@Value("${url.neo4j.cypher}")
	private String neo4jUrl;	
	@Value("${neo4j.username}")
	private String neo4jUsername;
	@Value("${neo4j.password}")
	private String neo4jPassword;
	
	private Methods methods;
	
	public RepositoryController() {
		this.methods = new Methods();
	}
	
	@RequestMapping("/repositories")
	@ResponseBody
	List<Repository> listRepositories() throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(
				  new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		List<Repository> result = new ArrayList<>();
		Map<String,Object> params = Collections.singletonMap("query", "MATCH (r:Repository) RETURN r.id,r.url");
		
		ObjectNode _result = restTemplate.postForObject(neo4jUrl, params, ObjectNode.class);
				
		ArrayNode dataNode = (ArrayNode) _result.get("data");
		dataNode.forEach((d) -> {
			ArrayNode node = (ArrayNode) d;
			Repository r = new Repository();
			r.setId(node.get(0).asInt());
			r.setUri(node.get(1).asText());
			result.add(r);
		});
		return result;
	}
	
	@RequestMapping(method=RequestMethod.POST, value="/repositories")
	@ResponseBody
	URI addRepository(@RequestParam URI uri) throws Exception {
		URI queryLocation = methods.postRepositoriesQueryService(uri, queryserviceUrl);
		int repoDBId = methods.postRepositoriesNeoDB(neo4jUsername, neo4jPassword, neo4jUrl, queryLocation);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		
		Future<List<URI>> waitForChildren = executor.submit(()->{
			while (!methods.getRepositoryStatusQueryService(queryLocation, RepositoryStatus.CHILD)) {
				Thread.sleep(1000);
			}
			return methods.getChildrenQueryService(queryLocation);
		});
		waitForChildren.get().forEach(childLocation -> methods.addChildRepositories(neo4jUsername, neo4jPassword, neo4jUrl, childLocation, repoDBId));
		
		Future<Boolean> waitForUnits = executor.submit(()->{
			while (!methods.getRepositoryStatusQueryService(queryLocation, RepositoryStatus.UNIT)) {
				Thread.sleep(1000);
			}
			return (methods.getUnitsCountQueryService(queryLocation)>0);
		});
		if(waitForUnits.get()) methods.postUnitsNeoDB(neo4jUsername, neo4jPassword, neo4jUrl, repoDBId, queryLocation);
		
		return new URI("http://localhost"); //TODO: return statement
	}

	@RequestMapping("/repositories/{id}/units")
	@ResponseBody
	List<InstallableUnit> listUnitsInRepository(@PathVariable Integer id) throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(
				  new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword));
		List<InstallableUnit> result = new ArrayList<>();
		Map<String,Object> params = Collections.singletonMap("query", "MATCH (r:Repository)-[p:PROVIDES]->(iu:IU) WHERE r.id = "+id+" RETURN iu.id,p.version");
		
		ObjectNode _result = restTemplate.postForObject(neo4jUrl, params, ObjectNode.class);
				
		ArrayNode dataNode = (ArrayNode) _result.get("data");
		dataNode.forEach((d) -> {
			ArrayNode node = (ArrayNode) d;
			InstallableUnit iu = new InstallableUnit();
			iu.setId(node.get(0).asText());
			iu.setVersion(node.get(1).asText());
			result.add(iu);
		});
		return result;
	}

//	@RequestMapping("/repositories")
//	@ResponseBody
//	List<Repository> repositories() {
//		RestTemplate restTemplate = new RestTemplate();
//		List<Repository> result = new ArrayList<>();
//		result = restTemplate.getForObject(queryserviceUrl+"/repositories", result.getClass());
//		return result;
//	}

}
