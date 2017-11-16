package com.itemis.p2m.backend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class Application {
	@Value("${neo4j.username}")
	private String neo4jUsername;
	@Value("${neo4j.password}")
	private String neo4jPassword;
	
	public static void main(String[] args) throws Exception {
		SpringApplication.run(Application.class, args);
	}
	
	@Bean
	public RestTemplate queryServiceRestTemplateBean() {
		return new RestTemplate();
	}
	
	@Bean
	public RestTemplate neoRestTemplateBean(BasicAuthorizationInterceptor basicAuth) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(basicAuth);
		return restTemplate;
	}
	
	@Bean
	public BasicAuthorizationInterceptor getBasicAuth() {
		return new BasicAuthorizationInterceptor(neo4jUsername, neo4jPassword);
	}
}
