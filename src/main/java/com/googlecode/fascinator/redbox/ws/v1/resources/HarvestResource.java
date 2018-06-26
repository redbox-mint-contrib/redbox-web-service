package com.googlecode.fascinator.redbox.ws.v1.resources;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.json.simple.JSONArray;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.api.indexer.IndexerException;
import com.googlecode.fascinator.api.indexer.SearchRequest;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.FascinatorHome;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.messaging.MessagingException;
import com.googlecode.fascinator.common.solr.SolrResult;
import com.googlecode.fascinator.common.storage.StorageUtils;
import com.googlecode.fascinator.portal.security.FascinatorWebSecurityExpressionRoot;
import com.googlecode.fascinator.redbox.ws.HarvestFileMapService;
import com.googlecode.fascinator.spring.ApplicationContextProvider;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiImplicitParam;
import com.wordnik.swagger.annotations.ApiImplicitParams;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Api(value = "harvest", description = "Operations on ReDBox Objects")
public class HarvestResource extends RedboxServerResource {

	private Storage storage;
	private HarvestFileMapService harvestFileMapService;
	private Logger log = LoggerFactory.getLogger(HarvestResource.class);
	private Gson gson; 

	

	public HarvestResource() {
		storage = (Storage) ApplicationContextProvider.getApplicationContext().getBean("fascinatorStorage");
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
	public String harvestObjects(JsonRepresentation data) throws IOException, PluginException, MessagingException {
		JsonObject responseJsonObject = new JsonObject();
		String packageType = getAttribute("packageType");
		JSONArray responses = new JSONArray();
		JsonSimple batchJson = new JsonSimple(data.getText());
		JSONArray recordsArray = batchJson.getArray("records");
		for (Object recordObject : recordsArray) {
			JsonObject processResult = processRecord((JsonObject) recordObject, packageType);
			try {
				responses.add(processResult);
			} catch (Exception e) {
				log.error("Error processing record in harvest", e);
				JsonObject responseObject = new JsonObject();
				responseObject.put("status", "error");
				responseObject.put("reason", ExceptionUtils.getStackTrace(e));
				responses.add(responseObject);
			}
		}
		responseJsonObject.put("results", responses);
		return new JsonSimple(responseJsonObject).toString();
	}

	private JsonObject processRecord(JsonObject record, String packageType)
			throws IOException, PluginException, MessagingException {

		JsonObject responseObject = validateRecord(record);
		if (StringUtils.isNotBlank((String) responseObject.get("status"))) {
			return responseObject;
		}

		String storageId = (String) record.get("oid");
		String harvestId = (String) record.get("harvest_id");
		if (StringUtils.isBlank(storageId)) {
			storageId = findStorageId(harvestId, packageType);
		}
		boolean created = false;
		if (StringUtils.isBlank(storageId)) {
			storageId = getOid();
			created = true;
		}

		JsonSimpleConfig config = new JsonSimpleConfig();
		String payloadId = "metadata.tfpackage";

		if ("mint".equals(config.getString(null, "system"))) {
			payloadId = "metadata.json";
		} else {
			if (!created) {
				payloadId = findTFPackageId(storageId);
			}
		}

		DigitalObject recordObject = StorageUtils.getDigitalObject(storage, storageId);

		setObjectMetadata(recordObject, record, packageType, created);

		if (record.get("datastreams") != null) {
			JSONArray datastreams = (JSONArray) record.get("datastreams");
			for (Object object : datastreams) {
				processDatastream(recordObject, (JsonObject) object);
			}
		}

		JsonObject recordMetadata = (JsonObject) record.get("metadata");
		if (StringUtils.isNotBlank(harvestId)) {
			recordMetadata.put("harvestId", harvestId);
		}
		recordMetadata.put("packageType", packageType);
		
		StorageUtils.createOrUpdatePayload(recordObject, payloadId,
				IOUtils.toInputStream(gson.toJson(recordMetadata), "utf-8"));

		reindex(storageId,getRulesConfigObject(getRulesConfigFile(packageType)), storage);

		responseObject.put("status", "success");
		responseObject.put("oid", storageId);
		if (StringUtils.isNotBlank(harvestId)) {
			responseObject.put("harvest_id", harvestId);
		}
		responseObject.put("action", created ? "created" : "updated");
		return responseObject;
	}

	private void processDatastream(DigitalObject recordObject, JsonObject datastream) throws StorageException {
		String datastreamId = (String) datastream.get("datastream_id");
		String contents = (String) datastream.get("contents");
		StorageUtils.createOrUpdatePayload(recordObject, datastreamId, IOUtils.toInputStream(contents), "utf-8");
	}

	private Properties setObjectMetadata(DigitalObject recordObject, JsonObject record, String packageType,
			boolean created) throws IOException, StorageException {
		Properties objectMetadata = recordObject.getMetadata();
		

		if (created) {
			
			File rulesConfigFile = getRulesConfigFile(packageType);
			DigitalObject  rulesConfigObject = getRulesConfigObject(rulesConfigFile);
			JsonSimpleConfig config = new JsonSimpleConfig();
			String repositoryName = "ReDBox";

			

			if ("mint".equals(config.getString(null, "system"))) {
				repositoryName = "Mint";
			}
			
			
			JsonSimple rulesConfigJson = new JsonSimple(rulesConfigFile);
			String scriptType = rulesConfigJson.getString(null, "indexer", "script", "type");

			DigitalObject rulesObject = getRulesObject(rulesConfigFile);
			
			objectMetadata.put("objectId", recordObject.getId());
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

		}

		if (record.get("objectMetadata") != null) {
			JsonObject data = (JsonObject) record.get("objectMetadata");
			for (Object keyObject : data.keySet()) {
				objectMetadata.put(keyObject, data.get(keyObject));
			}
		}

		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		objectMetadata.store(outStream, "Written by Harvest API");
		StorageUtils.createOrUpdatePayload(recordObject, "TF-OBJ-META",
				IOUtils.toInputStream(new String(outStream.toByteArray()), "utf-8"));

		return objectMetadata;
	}

	private DigitalObject getRulesObject(File rulesConfigFile) throws IOException, StorageException {
		JsonSimpleConfig config = new JsonSimpleConfig();
		String harvestPath = "harvest/workflows/";
		

		if ("mint".equals(config.getString(null, "system"))) {
			harvestPath = "harvest/";
		}
		
		JsonSimple rulesConfigJson = new JsonSimple(rulesConfigFile);
		String rulesScript = rulesConfigJson.getString(null, "indexer", "script", "rules");

		File rulesScriptFile = FascinatorHome.getPathFile(harvestPath + rulesScript);
		
		return harvestFileMapService.get(rulesScriptFile);
	}

	private DigitalObject getRulesConfigObject(File rulesConfigFile) throws IOException, StorageException {
		
		return harvestFileMapService.get(rulesConfigFile);
	}

	private File getRulesConfigFile(String packageType) throws IOException {
		JsonSimpleConfig config = new JsonSimpleConfig();
		String harvestPath = "harvest/workflows/";
		

		String rulesConfig = config.getString(null, "portal", "packageTypes", packageType, "jsonconfig");

		if ("mint".equals(config.getString(null, "system"))) {
			harvestPath = "harvest/";
			rulesConfig = packageType + ".json";
		}
		
		
		return FascinatorHome.getPathFile(harvestPath + rulesConfig);
	}

	private JsonObject validateRecord(JsonObject record) {
		JsonObject responseObject = new JsonObject();
		if (StringUtils.isNotBlank((String) record.get("oid"))
				&& StringUtils.isNotBlank((String) record.get("harvest_id"))) {
			responseObject.put("status", "failed");
			responseObject.put("reason", "Record is missing an oid or a harvest_id value");
			return responseObject;
		}

		if (record.get("objectMetadata") != null) {
			if (!(record.get("objectMetadata") instanceof JsonObject)) {
				responseObject.put("status", "failed");
				responseObject.put("reason", "objectMetadata property must be an object");
				return responseObject;
			}
		}
		
		if (record.get("datastreams") != null) {
			if (!(record.get("datastreams") instanceof JSONArray)) {
				responseObject.put("status", "failed");
				responseObject.put("reason", "datastreams property must be an array");
				return responseObject;
			} else {
				for (Object datastreamObject : (JSONArray)record.get("datastreams")) {
					if(!(datastreamObject instanceof JsonObject)) {
						responseObject.put("status", "failed");
						responseObject.put("reason", "All datastreams in the array must be objects");
						return responseObject;
					} else {
						if(((JsonObject)datastreamObject).get("datastream_id") == null) {
							responseObject.put("status", "failed");
							responseObject.put("reason", "All datastreams in the array must have a datastream_id property");
							return responseObject;
						}
						if(((JsonObject)datastreamObject).get("contents") == null) {
							responseObject.put("status", "failed");
							responseObject.put("reason", "All datastreams in the array must have a contents property");
							return responseObject;
						}
					}
				}
			}
		}
		return responseObject;
	}

	private String findTFPackageId(String storageId) throws StorageException {
		DigitalObject object = storage.getObject(storageId);
		if (object.getPayload("metadata.tfpackage") != null) {
			return "metadata.tfpackage";
		}
		for (String payloadId : object.getPayloadIdList()) {
			if (payloadId.endsWith(".tfpackage")) {
				return payloadId;
			}
		}
		return "metadata.tfpackage";
	}

	private String findStorageId(String harvestId, String packageType) throws IndexerException, IOException {
		Indexer indexer = (Indexer) ApplicationContextProvider.getApplicationContext().getBean("fascinatorIndexer");
		SearchRequest request = new SearchRequest("harvestId:\"" + harvestId + "\"");
		request.addParam("fq", "packageType:" + packageType);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		indexer.search(request, byteArrayOutputStream);
		SolrResult results = new SolrResult(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
		int rows = results.getRows();
		if (rows == 0) {
			return null;
		} else {
			return results.getResults().get(0).getString(null, "storage_id");
		}
	}

	private String getOid() {
		return DigestUtils.md5Hex(
				"SomeRandomPrefix:" + String.valueOf(System.currentTimeMillis()) + String.valueOf(Math.random()));
	}
	
	

}
