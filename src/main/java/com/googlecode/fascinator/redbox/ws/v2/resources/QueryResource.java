package com.googlecode.fascinator.redbox.ws.v2.resources;

import java.io.IOException;

import org.bson.Document;
import org.json.simple.JSONArray;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.googlecode.fascinator.api.indexer.IndexerException;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.storage.impl.SpringStorageWrapper;
import com.googlecode.fascinator.redbox.ws.v1.resources.RedboxServerResource;
import com.googlecode.fascinator.spring.ApplicationContextProvider;
import com.mongodb.client.FindIterable;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import au.com.redboxresearchdata.fascinator.storage.mongo.MongoStorage;

@Api(value = "search", description="Search ReDBox's search index")
public class QueryResource extends RedboxServerResource {

	private Gson gson; 
	
	public QueryResource() {
		gson = new GsonBuilder().create();
	}
	
	@ApiOperation(value = "Query ReDBox's mongo database", tags = "search")
	@ApiResponses({
        @ApiResponse(code = 200, message = "Search results returned"),
        @ApiResponse(code = 500, message = "General Error", response = Exception.class)
	})
	@Post("json")
	public Representation queryMongo(JsonRepresentation data) throws IndexerException, IOException{
		SpringStorageWrapper storageWrapper = (SpringStorageWrapper) ApplicationContextProvider.getApplicationContext().getBean("fascinatorStorage");
		MongoStorage storage = (MongoStorage) storageWrapper.getStoragePlugin();
		
		String collection = getQueryValue("collection");
		
		FindIterable<Document> results = storage.query(collection, data.getText());
		
		JsonObject resultObject = new JsonObject();
		JSONArray resultArray = new JSONArray();
		
		for (Document document : results) {
			resultArray.add(new JsonSimple(document.toJson()));
		}
		resultObject.put("results", resultArray);
		
		return new StringRepresentation(gson.toJson(resultObject));
	}



}
