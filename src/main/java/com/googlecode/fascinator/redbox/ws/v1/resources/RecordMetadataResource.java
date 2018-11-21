package com.googlecode.fascinator.redbox.ws.v1.resources;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Payload;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.messaging.MessagingException;
import com.googlecode.fascinator.common.storage.StorageUtils;
import com.googlecode.fascinator.spring.ApplicationContextProvider;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiImplicitParam;
import com.wordnik.swagger.annotations.ApiImplicitParams;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Api(value = "recordmetadata", description = "Operations that work with an record's metadata")
public class RecordMetadataResource extends RedboxServerResource {

	private Gson gson; 
	
	public RecordMetadataResource() {
		gson = new GsonBuilder().create();
	}
	
	@ApiOperation(value = "gets the record's metadata", tags = "recordmeta")
	@ApiResponses({ @ApiResponse(code = 200, message = "The record's metadata is returned"),
			@ApiResponse(code = 500, message = "General Error", response = Exception.class) })
	@Get("json")
	public String getMetadataResource() throws StorageException, IOException {
		JsonSimpleConfig config = (JsonSimpleConfig)ApplicationContextProvider.getApplicationContext().getBean("fascinatorConfig");
		Storage storage = (Storage) ApplicationContextProvider.getApplicationContext().getBean("fascinatorStorage");
		String oid = getAttribute("oid");
		DigitalObject digitalObject = StorageUtils.getDigitalObject(storage, oid);
		String payloadId;
		if ("mint".equals(config.getString(null, "system"))) {
			payloadId = "metadata.json";
		} else {
			payloadId = findTfPackagePayload(digitalObject);
		}
		Payload payload = digitalObject.getPayload(payloadId);
		JsonSimple metadataObject = new JsonSimple(payload.open());
		
		return gson.toJson(metadataObject.getJsonObject());
	}

	@ApiOperation(value = "updates the record's metadata", tags = "recordmeta")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "skipReindex", value = "Skip the reindex process. Useful if you are batching many changes to a ReDBox object at once.", required = false, allowMultiple = false, defaultValue = "false", dataType = "string") })
	@ApiResponses({ @ApiResponse(code = 200, message = "The record's metadata is updated"),
			@ApiResponse(code = 500, message = "General Error", response = Exception.class) })
	@Post("json")
	public String updateMetadataResource(JsonRepresentation data)
			throws IOException, PluginException, MessagingException {
		JsonSimpleConfig config = new JsonSimpleConfig();
		Storage storage = (Storage) ApplicationContextProvider.getApplicationContext().getBean("fascinatorStorage");
		String oid = getAttribute("oid");

		JsonSimple metadataJson = new JsonSimple(data.getText());

		DigitalObject digitalObject = StorageUtils.getDigitalObject(storage, oid);
		String payloadId;
		if ("mint".equals(config.getString(null, "system"))) {
			payloadId = "metadata.json";
		} else {
			payloadId = findTfPackagePayload(digitalObject);
		}

		StorageUtils.createOrUpdatePayload(digitalObject, payloadId,
				IOUtils.toInputStream(metadataJson.toString(true), "UTF-8"));

		reindex(oid);

		return getSuccessResponseString(oid);
	}

	private String findTfPackagePayload(DigitalObject digitalObject) {
		for (String payloadId : digitalObject.getPayloadIdList()) {
			if (payloadId.endsWith("tfpackage")) {
				return payloadId;
			}
		}
		return null;
	}

}
