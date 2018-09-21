package com.itemis.p2m.backend.controller;

import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import com.itemis.p2m.backend.QueryServiceHandler;
import com.itemis.p2m.backend.rest.repository.RepositoryController;
import com.itemis.p2m.backend.rest.repository.RepositoryHandler;
import com.itemis.p2m.backend.services.ShoppingCartOptimizerService;

/**
 * Unit tests for the RepositoryController class.
 */
public class RepositoryControllerTest {
	
	RepositoryController repositoryController;
			
	@Before
	public void setup() {
		RepositoryHandler repoHandler = new RepositoryHandler();
		QueryServiceHandler handler = mock(QueryServiceHandler.class);
		ShoppingCartOptimizerService optimizer = mock(ShoppingCartOptimizerService.class);
		RestTemplate neoRestTemplate = mock(RestTemplate.class);
		
		repositoryController = new RepositoryController(repoHandler, handler, optimizer, neoRestTemplate);
	}
	
	@Test
	public void bla() {
		
	}
	
	@Test
	public void someTestForRepoController() {
		//TODO: write tests
//		final String repositoryURI = "http://www.fakeURI.com";
		
//		when(methods.postRepositoriesQueryService(URI.create(repositoryURI), anyString())).thenReturn(URI.create("http://www.fakeURI.com/repos/42"));
		//TODO: how to mock the query service
		//TODO: how to mock the neo4j database
//		mockMvc.perform(post("/repositories").param("uri", repositoryURI)).andDo(print()).andExpect(status().isOk()).andExpect(things);
	}
}
