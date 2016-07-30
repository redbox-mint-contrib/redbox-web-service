package com.googlecode.fascinator.redbox.ws.v1.resources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.messaging.MessagingException;
import com.googlecode.fascinator.common.storage.StorageUtils;
import com.googlecode.fascinator.spring.ApplicationContextProvider;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Api(value = "objectMetadata",description="Operations involving an objects metadata")
public class ObjectMetadataResource extends RedboxServerResource {

	@ApiOperation(value = "gets the record's Object Metadata", tags="objectmeta")
	@ApiResponses({
        @ApiResponse(code = 200, message = "The object metadata is returned"),
        @ApiResponse(code = 500, message = "General Error", response = Exception.class)
	})
	@Get("json")
	public String getMetadataResource() throws StorageException, IOException {
		Storage storage = (Storage) ApplicationContextProvider.getApplicationContext().getBean("fascinatorStorage");
		String oid = getAttribute("oid");
		DigitalObject digitalObject = StorageUtils.getDigitalObject(storage, oid);
		Properties metadata = digitalObject.getMetadata();
		JsonObject jsonObject = new JsonObject();

		for (Object keyObject : metadata.keySet()) {
			jsonObject.put((String) keyObject, metadata.getProperty((String) keyObject));
		}
		return new JsonSimple(jsonObject).toString(true);
	}
	
	@ApiOperation(value = "updates the record's Object Metadata", tags="objectmeta")
	@ApiResponses({
        @ApiResponse(code = 200, message = "The object metadata is updated"),
        @ApiResponse(code = 500, message = "General Error", response = Exception.class)
	})
	@Post("json")
	public String updateMetadataResource(JsonRepresentation data) throws IOException, PluginException, MessagingException {
		Storage storage = (Storage) ApplicationContextProvider.getApplicationContext().getBean("fascinatorStorage");
		String oid = getAttribute("oid");
		
		JsonSimple metadataJson = new JsonSimple(data.getText());
		JsonObject metadataObject = metadataJson.getJsonObject();
		DigitalObject digitalObject = StorageUtils.getDigitalObject(storage, oid);
		Properties metadata = digitalObject.getMetadata();
		for (Object key : metadataObject.keySet()) {
			metadata.setProperty((String) key, (String) metadataObject.get(key));
		}
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		metadata.store(output, null);
		ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
		StorageUtils.createOrUpdatePayload(digitalObject, "TF-OBJ-META", input);
		reindex(oid);
		
		return getSuccessResponseString(oid);
		
	}

}
