package com.googlecode.fascinator.redbox.ws.v1.resources;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Post;

import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.FascinatorHome;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.messaging.MessagingException;
import com.googlecode.fascinator.common.storage.StorageUtils;
import com.googlecode.fascinator.spring.ApplicationContextProvider;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Api(value = "object", description = "Operations on ReDBox Objects")
public class ObjectResource extends RedboxServerResource {

	@ApiOperation(value = "create a new ReDBox Object", tags = "object")
	@ApiResponses({ @ApiResponse(code = 200, message = "An object is created"),
			@ApiResponse(code = 500, message = "General Error", response = Exception.class) })
	@Post("json")
	public String createObjectResource(JsonRepresentation data)
			throws IOException, PluginException, MessagingException {
		Storage storage = (Storage) ApplicationContextProvider.getApplicationContext().getBean("fascinatorStorage");
		JsonSimpleConfig config = new JsonSimpleConfig();

		String packageType = getAttribute("packageType");
		String harvestPath = "harvest/workflows/";
		String repositoryName = "ReDBox";
		String payloadId = "metadata.tfpackage";
		String rulesConfig = config.getString(null, "portal", "packageTypes", packageType, "jsonconfig");
		
		
		if ("mint".equals(config.getString(null, "system"))) {
			harvestPath = "harvest/";
			repositoryName = "Mint";
			payloadId = "metadata.json";
			rulesConfig = packageType+".json";
		}
		File rulesConfigFile = FascinatorHome.getPathFile(harvestPath + rulesConfig);

		JsonSimple rulesConfigJson = new JsonSimple(rulesConfigFile);
		String rulesScript = rulesConfigJson.getString(null, "indexer", "script", "rules");
		String scriptType = rulesConfigJson.getString(null, "indexer", "script", "type");
		File rulesScriptFile = FascinatorHome.getPathFile(harvestPath + rulesScript);

		DigitalObject rulesConfigObject = checkHarvestFile(storage, rulesConfigFile);
		DigitalObject rulesObject = checkHarvestFile(storage, rulesScriptFile);

		String oid = getOid();
		DigitalObject recordObject = StorageUtils.getDigitalObject(storage, oid);
		Properties objectMetadata = recordObject.getMetadata();
		objectMetadata.put("objectId", oid);
		objectMetadata.put("render-pending", "true");
		objectMetadata.put("owner", "admin");
		objectMetadata.put("repository.name", repositoryName);

		objectMetadata.put("repository.type", "Metadata Registry");
		objectMetadata.put("metaPid", "TF-OBJ-META");
		objectMetadata.setProperty("scriptType", scriptType);
		// Set our config and rules data as properties on the object
		objectMetadata.setProperty("rulesOid", rulesObject.getId());
		objectMetadata.setProperty("rulesPid", rulesObject.getSourceId());
		objectMetadata.setProperty("jsonConfigOid", rulesConfigObject.getId());
		objectMetadata.setProperty("jsonConfigPid", rulesConfigObject.getSourceId());

		JsonObject params = rulesConfigJson.getObject("indexer", "params");
		if (params != null) {
			for (Object key : params.keySet()) {
				objectMetadata.setProperty(key.toString(), params.get(key).toString());
			}
		}

		JsonSimple metadataJson = new JsonSimple(data.getText());

		StorageUtils.createOrUpdatePayload(recordObject, payloadId,
				IOUtils.toInputStream(metadataJson.toString(true), "utf-8"));

		recordObject.close();

		reindex(oid);
		return getSuccessResponseString(oid);

	}

	@ApiOperation(value = "Delete an existing ReDBox object", tags = "object")
	@ApiResponses({ @ApiResponse(code = 200, message = "The object is deleted"),
			@ApiResponse(code = 500, message = "General Error", response = Exception.class) })
	@Delete
	public String deleteObjectResource() throws IOException, PluginException, MessagingException {
		Storage storage = (Storage) ApplicationContextProvider.getApplicationContext().getBean("fascinatorStorage");
		Indexer indexer = (Indexer) ApplicationContextProvider.getApplicationContext().getBean("fascinatorIndexer");
		String oid = getAttribute("packageType");
		storage.removeObject(oid);
		indexer.remove(oid);
		return getSuccessResponseString(oid);

	}

	private String getOid() {
		return DigestUtils.md5Hex(
				"SomeRandomPrefix:" + String.valueOf(System.currentTimeMillis()) + String.valueOf(Math.random()));
	}

	private DigitalObject checkHarvestFile(Storage storage, File file) throws StorageException {
		DigitalObject fileObj = StorageUtils.checkHarvestFile(storage, file);
		if (fileObj == null) {
			String fileoid = StorageUtils.generateOid(file);
			fileObj = StorageUtils.getDigitalObject(storage, fileoid);
		}

		return fileObj;
	}

}
