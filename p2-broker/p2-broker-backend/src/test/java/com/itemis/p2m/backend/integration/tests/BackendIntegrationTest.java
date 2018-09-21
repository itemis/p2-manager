package com.itemis.p2m.backend.integration.tests;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class BackendIntegrationTest {
	@Value("${url.queryservice}")
	private String queryserviceUrl;	
	@Value("${url.neo4j.cypher}")
	private String neo4jUrl;
	
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    @Qualifier("queryServiceRestTemplateBean")
    private RestTemplate queryServiceRestTemplate;
    
    @MockBean
    @Qualifier("neoRestTemplateBean")
    private RestTemplate neoRestTemplate;
    
    private ObjectMapper mapper;
    
    @Before
    public void init() {
    	mapper = new ObjectMapper();
    }
    
    private void setupResponse(String response) throws Exception {
    	ObjectNode responseObject = mapper.readValue(response, ObjectNode.class);
    	when(neoRestTemplate.postForObject(eq(neo4jUrl), isA(Map.class), eq(ObjectNode.class))).thenReturn(responseObject);
	}

    //-----------------------RepositoryController-----------------------------
    
    @Test
    public void returnEmptyArrayIfNeoHasNoRepositories() throws Exception {
    	this.setupResponse("{\"data\":[]}");
		
        this.mockMvc.perform(get("/repositories")).andDo(print()).andExpect(status().isNoContent()).andExpect(content().string(""));
    }

    @Test
    public void returnRepositoriesIfNeoHasRepositories() throws Exception {
    	this.setupResponse("{\"data\":[[16, \"http://www.allTheRepositories.com/theBestRepository\"],[42, \"http://www.justOneRepository.org\"]]}");
    	
        this.mockMvc.perform(get("/repositories"))
        			.andDo(print())
        			.andExpect(status().isOk())
        			.andExpect(content().string(containsString("16")))
        			.andExpect(content().string(containsString("http://www.allTheRepositories.com/theBestRepository")))
        			.andExpect(content().string(containsString("42")))
        			.andExpect(content().string(containsString("http://www.justOneRepository.org")));
    }
    
    // TODO: check with limits
    // TODO: check shopping cart
    
    @Test
    public void returnTheQueriedRepository() throws Exception {
    	this.setupResponse("{\"data\":[[16, \"http://www.allTheRepositories.com/theBestRepository\"],[42, \"http://www.justOneRepository.org\"]]}");
		
        this.mockMvc.perform(get("/repositories/16"))
        			.andDo(print())
        			.andExpect(status().isOk())
        			.andExpect(content().string(containsString("16")))
        			.andExpect(content().string(containsString("http://www.allTheRepositories.com/theBestRepository")));
    }
    
    @Test
    public void returnOnlyTheQueriedRepository() throws Exception {
    	this.setupResponse("{\"data\":[[16, \"http://www.allTheRepositories.com/theBestRepository\"],[42, \"http://www.justOneRepository.org\"]]}");
		
        String result = this.mockMvc.perform(get("/repositories/16"))
        			.andDo(print())
        			.andReturn().getResponse().getContentAsString();
        
        assertFalse(result.contains("42"));
        assertFalse(result.contains("http://www.justOneRepository.org"));
    }
    
    @Test
    public void returnIUsIfRepositoryHasIUs() throws Exception {
    	this.setupResponse("{\"data\":[[\"com.company.framework.unit\", \"1.2.3\"], [\"org.organization.tool\", \"9.0.1\"]]}");
		
        this.mockMvc.perform(get("/repositories/16/units"))
        			.andDo(print())
        			.andExpect(status().isOk())
        			.andExpect(content().string(containsString("com.company.framework.unit")))
        			.andExpect(content().string(containsString("1.2.3")))
					.andExpect(content().string(containsString("org.organization.tool")))
					.andExpect(content().string(containsString("9.0.1")));
    }

    //-----------------------InstallableUnitController-----------------------------

    @Test
    public void returnIUsIfNeoHasIUs() throws Exception {
    	this.setupResponse("{\"data\":[[\"com.company.framework.unit\", \"1.2.3\"], [\"com.company.framework.unit\", \"1.5.2\"], [\"org.organization.tool\", \"9.0.1\"]]}");
		
        this.mockMvc.perform(get("/units"))
        			.andDo(print())
        			.andExpect(status().isOk())
        			.andExpect(content().string(containsString("com.company.framework.unit")))
        			.andExpect(content().string(containsString("1.2.3")))
        			.andExpect(content().string(containsString("1.5.2")))
					.andExpect(content().string(containsString("org.organization.tool")))
					.andExpect(content().string(containsString("9.0.1")));
    }
    
    @Test
    public void returnAllVersionsOfTheQueriedIU() throws Exception {
    	this.setupResponse("{\"data\":[[\"com.company.framework.unit\", \"1.2.3\"], [\"com.company.framework.unit\", \"1.5.2\"], [\"org.organization.tool\", \"9.0.1\"]]}");

    	assertEquals("", "");
        this.mockMvc.perform(get("/units/com.company.framework.unit/versions"))
        			.andDo(print())
        			.andExpect(status().isOk())
        			.andExpect(content().string(containsString("com.company.framework.unit")))
        			.andExpect(content().string(containsString("1.2.3")))
					.andExpect(content().string(containsString("1.5.2")));
    }
    
    @Test
    public void returnOnlyVersionsOfTheQueriedIU() throws Exception {
    	this.setupResponse("{\"data\":[[\"com.company.framework.unit\", \"1.2.3\"], [\"com.company.framework.unit\", \"1.5.2\"], [\"org.organization.tool\", \"9.0.1\"]]}");
		
    	String result = this.mockMvc.perform(get("/units/com.company.framework.unit"))
        			.andDo(print())        
        			.andReturn().getResponse().getContentAsString();
        
    	assertFalse(result.contains("org.organization.tool"));
    	assertFalse(result.contains("9.0.1"));
    }
    
    @Test
    public void returnRepositoriesWhenQueryingForUIVersions() throws Exception {
    	this.setupResponse("{\"data\":[[16, \"http://www.allTheRepositories.com/theBestRepository\"],[42, \"http://www.justOneRepository.org\"]]}");
		
        this.mockMvc.perform(get("/units/com.company.framework.unit/versions/1.0.0/repositories"))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(content().string(containsString("16")))
		.andExpect(content().string(containsString("http://www.allTheRepositories.com/theBestRepository")))
		.andExpect(content().string(containsString("42")))
		.andExpect(content().string(containsString("http://www.justOneRepository.org")));
    }
}
