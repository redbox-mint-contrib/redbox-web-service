package com.googlecode.fascinator.redbox.ws.v2.resources;

import java.io.IOException;

import org.bson.Document;
import org.json.simple.JSONArray;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Get;

import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.PayloadType;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.storage.StorageUtils;
import com.googlecode.fascinator.common.storage.impl.SpringStorageWrapper;
import com.googlecode.fascinator.redbox.ws.v1.resources.RedboxServerResource;
import com.googlecode.fascinator.spring.ApplicationContextProvider;
import com.mongodb.client.FindIterable;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import au.com.redboxresearchdata.fascinator.storage.mongo.MongoStorage;

@Api(value = "listDatastream", description = "List datastreams in an object")
public class ListDatastreamResource extends RedboxServerResource {

	@SuppressWarnings("unchecked")
	@ApiOperation(value = "List datastreams in an object", tags = "datastream")
	@ApiResponses({
        @ApiResponse(code = 200, message = "The datastreams are listed"),
        @ApiResponse(code = 500, message = "Oid does not exist in storage", response = StorageException.class)
	})
	@Get("json")
	public JsonRepresentation getDatastreamList() throws StorageException, IOException {
		SpringStorageWrapper storageWrapper = (SpringStorageWrapper) ApplicationContextProvider.getApplicationContext()
				.getBean("fascinatorStorage");
		MongoStorage storage = (MongoStorage) storageWrapper.getStoragePlugin();
		String collection = getQueryValue("collection");
		if(collection == null) {
			collection = "default";
		}
		String oid = getAttribute("oid");
		
		String filterString = "{redboxOid: '"+oid+"'}";
		FindIterable<Document> results = storage.query(collection, filterString);
		Document doc = results.first();
		if(doc != null) {
			JsonSimple document = new JsonSimple(doc.toJson());
			JSONArray dataStreams = new JSONArray();
			
			JSONArray files = document.getArray("files");
			for (Object object : files) {
				JsonObject file = (JsonObject)object;
				if(file.get("payloadType") != null && file.get("payloadType").equals(PayloadType.Annotation.toString()) ) {
					dataStreams.add(file);
				}
			}
			JsonObject responseObject = getSuccessResponse(oid);
			
			responseObject.put("datastreams", dataStreams);
			return new JsonRepresentation(new JsonSimple(responseObject).toString(true));
		}
		
		JsonObject responseObject = getSuccessResponse(oid);
		responseObject.put("code", 404);
		responseObject.put("reason", "No record found");

		return new JsonRepresentation(new JsonSimple(responseObject).toString(true));
	}
	
	


}
