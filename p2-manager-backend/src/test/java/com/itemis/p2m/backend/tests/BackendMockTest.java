package com.itemis.p2m.backend.tests;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
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
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class BackendMockTest {
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

    @Test
    public void returnEmptyArrayWhenNeoHasNoRepositories() throws Exception {
    	ObjectNode response = mapper.readValue("{\"data\":[]}", ObjectNode.class);
    	when(neoRestTemplate.postForObject(eq(neo4jUrl), isA(Map.class), eq(ObjectNode.class))).thenReturn(response);
		
        this.mockMvc.perform(get("/repositories")).andDo(print()).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    public void returnRepositoriesWhenNeoHasRepositories() throws Exception {
    	ObjectNode response = mapper.readValue("{\"data\":[[16, \"http://www.allTheRepositories.com/theBestRepository\"],[42, \"http://www.justOneRepository.org\"]]}", ObjectNode.class);
    	when(neoRestTemplate.postForObject(eq(neo4jUrl), isA(Map.class), eq(ObjectNode.class))).thenReturn(response);
		
        this.mockMvc.perform(get("/repositories"))
        			.andDo(print())
        			.andExpect(status().isOk())
        			.andExpect(content().string(containsString("16")))
        			.andExpect(content().string(containsString("16")))
        			.andExpect(content().string(containsString("http://www.allTheRepositories.com/theBestRepository")))
        			.andExpect(content().string(containsString("42")))
        			.andExpect(content().string(containsString("http://www.justOneRepository.org")));
    }
}
