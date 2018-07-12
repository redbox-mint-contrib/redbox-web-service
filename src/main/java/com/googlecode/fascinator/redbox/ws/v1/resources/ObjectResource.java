package com.googlecode.fascinator.redbox.ws.v1.resources;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Post;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.JsonDigitalObject;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.FascinatorHome;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.messaging.MessagingException;
import com.googlecode.fascinator.common.storage.StorageUtils;
import com.googlecode.fascinator.redbox.ws.HarvestFileMapService;
import com.googlecode.fascinator.spring.ApplicationContextProvider;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiImplicitParam;
import com.wordnik.swagger.annotations.ApiImplicitParams;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Api(value = "object", description = "Operations on ReDBox Objects")
public class ObjectResource extends RedboxServerResource {

	private HarvestFileMapService harvestFileMapService;
	private Gson gson;
	
	public ObjectResource() {
		harvestFileMapService = (HarvestFileMapService) ApplicationContextProvider.getApplicationContext()
				.getBean("harvestFileMapService");
		 gson = new GsonBuilder().create();
	}

	@ApiOperation(value = "create a new ReDBox Object", tags = "object")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "skipReindex", value = "Skip the reindex process. Useful if you are batching many changes to a ReDBox object at once.", required = false, allowMultiple = false, dataType = "boolean"),
			@ApiImplicitParam(name = "oid", value = "The desired object identifier. If not supplied, one will be randomly generated", required = false, allowMultiple = false, dataType = "string") })
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
			rulesConfig = packageType + ".json";
		}
		File rulesConfigFile = FascinatorHome.getPathFile(harvestPath + rulesConfig);

		JsonSimple rulesConfigJson = new JsonSimple(rulesConfigFile);
		String rulesScript = rulesConfigJson.getString(null, "indexer", "script", "rules");
		String scriptType = rulesConfigJson.getString(null, "indexer", "script", "type");
		File rulesScriptFile = FascinatorHome.getPathFile(harvestPath + rulesScript);

		DigitalObject rulesConfigObject = harvestFileMapService.get(rulesConfigFile);
		DigitalObject rulesObject = harvestFileMapService.get(rulesScriptFile);

		String oid = getQueryValue("oid");
		if (oid == null) {
			oid = getOid();
		}
		DigitalObject recordObject = StorageUtils.getDigitalObject(storage, oid);
		if (recordObject instanceof JsonDigitalObject) {
			JsonDigitalObject jsonObj = (JsonDigitalObject) recordObject;
			jsonObj.getRecordMetadata().put("packageType", packageType);
		}
		Properties objectMetadata = recordObject.getMetadata();
		objectMetadata.put("packageType", packageType);
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
				IOUtils.toInputStream(gson.toJson(metadataJson.getJsonObject()), "utf-8"));

		recordObject.close();

		reindex(oid);
		return getSuccessResponseString(oid);

	}

	private String getOid() {
		return DigestUtils.md5Hex(
				"SomeRandomPrefix:" + String.valueOf(System.currentTimeMillis()) + String.valueOf(Math.random()));
	}

}
