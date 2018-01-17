package com.itemis.p2m.backend.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import com.itemis.p2m.backend.QueryServiceHandler;
import com.itemis.p2m.backend.controllers.RepositoryController;
import com.itemis.p2m.backend.services.ShoppingCartOptimizerService;

/**
 * Unit tests for the RepositoryController class.
 */
@RunWith(SpringRunner.class)
@WebMvcTest(RepositoryController.class)
public class RepositoryControllerTest {
	
//	@Autowired
//	private MockMvc mockMvc;
	
	@MockBean
	private QueryServiceHandler handler;
	
	@MockBean
	private ShoppingCartOptimizerService optimizer;
	
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
