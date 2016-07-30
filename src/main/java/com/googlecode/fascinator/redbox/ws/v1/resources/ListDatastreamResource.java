package com.googlecode.fascinator.redbox.ws.v1.resources;

import java.io.IOException;

import org.json.simple.JSONArray;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Get;

import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.storage.StorageUtils;
import com.googlecode.fascinator.spring.ApplicationContextProvider;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

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
		Storage storage = (Storage) ApplicationContextProvider.getApplicationContext().getBean("fascinatorStorage");
		String oid = getAttribute("oid");
		DigitalObject digitalObject = StorageUtils.getDigitalObject(storage, oid);
		JsonObject responseObject = getSuccessResponse(oid);
		JSONArray dataStreamIds = new JSONArray();
		dataStreamIds.addAll(digitalObject.getPayloadIdList());
		responseObject.put("datastreamIds", dataStreamIds);

		return new JsonRepresentation(new JsonSimple(responseObject).toString(true));
	}
	
	


}
