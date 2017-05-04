package com.itemis.p2queryservice.rest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path("/p2/repository")
public class RestService{

    private static final Logger logger = Logger.getLogger(RestService.class.getName());
	
	public RestService() {
		logger.info("Construct TestRestService");
	}

    @GET
    @Produces("text/plain")
	@Path("/hello/world")
    public String getHelloWorld() {
        return "Hello World";
    }

    @GET
    @Produces("text/plain")
	@Path("/end")
    public String stopService() {
//    	JettyApplication.getDefault().stopRun();
        return "Service is stopped";
    }
	
	@POST
    @Produces("text/plain")
	public String addRepo(@FormParam("uri") String uri) throws IOException{
		if (uri == null)
			throw new IllegalArgumentException("no repository");
		File f = new File("P2RepoId.txt");
		int lineNumber = 0;
		try{
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			while((line = br.readLine()) != null){
				lineNumber++;
				if(uri.equals(line)){
					br.close();
					return "id=" + lineNumber +"\n"; //Maybe an Exception, because the Repository is already existing
				}
			}
			br.close();
			fr.close();
		} catch (FileNotFoundException e) {
			logger.info("File does not Exist");
		}		
		FileWriter fw = new FileWriter(f, true);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.append(uri + "\n");
		lineNumber++;
		bw.close();
		fw.close();
		return "id=" + lineNumber +"\n";
	}
	
	@GET
	@Produces("text/plain")
	@Path("{id}")
	public String getRepo(@PathParam("id") String id){
		int repoId = 0;
		try{
			repoId = Integer.parseInt(id);
		} catch(NumberFormatException nfe){
			throw new IllegalArgumentException("The repository ID have to be a number");
		}
		File f = new File("P2RepoId.txt");
		try{
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			for (int i=0; i<repoId; i++){
				line = br.readLine();
				if (line == null){
					br.close();
					fr.close();					
					throw new IllegalArgumentException("There is no Repository with this Id");
				}
			}
			br.close();
			fr.close();
			return line;
		} catch (IOException e) {
			throw new IllegalArgumentException("There is no Repository with this Id");
		}
	}
}