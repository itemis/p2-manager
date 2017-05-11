package com.itemis.p2queryservice.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/ping")
public class PingService {
	@Path("/")
	@GET
	public String ping() {
		return "pong";
	}
}
