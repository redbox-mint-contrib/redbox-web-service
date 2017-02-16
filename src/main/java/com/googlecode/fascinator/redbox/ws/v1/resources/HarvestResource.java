package com.googlecode.fascinator.redbox.ws.v1.resources;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.json.simple.JSONArray;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Post;

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
import com.googlecode.fascinator.redbox.ws.HarvestFileMapService;
import com.googlecode.fascinator.spring.ApplicationContextProvider;
import com.hp.hpl.jena.query.function.library.not;
import java.io.OutputStream;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiImplicitParam;
import com.wordnik.swagger.annotations.ApiImplicitParams;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Api(value = "harvest", description = "Operations on ReDBox Objects")
public class HarvestResource extends RedboxServerResource {

	private Storage storage;
	private Map<String, DigitalObject> harvestFileMap = new HashMap<String, DigitalObject>();
	private HarvestFileMapService harvestFileMapService;

	public HarvestResource() {
		storage = (Storage) ApplicationContextProvider.getApplicationContext().getBean("fascinatorStorage");	
		harvestFileMapService = (HarvestFileMapService) ApplicationContextProvider.getApplicationContext().getBean("harvestFileMapService");
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
			responses.add(processResult);
		}
		responseJsonObject.put("results", responses);
		return new JsonSimple(responseJsonObject).toString();
	}

	private JsonObject processRecord(JsonObject record, String packageType)
			throws IOException, PluginException, MessagingException {

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
		String harvestPath = "harvest/workflows/";
		String repositoryName = "ReDBox";
		String payloadId = "metadata.tfpackage";
		String rulesConfig = config.getString(null, "portal", "packageTypes", packageType, "jsonconfig");

		if ("mint".equals(config.getString(null, "system"))) {
			harvestPath = "harvest/";
			repositoryName = "Mint";
			payloadId = "metadata.json";
			rulesConfig = packageType + ".json";
		} else {
			if (!created) {
				payloadId = findTFPackageId(storageId);
			}
		}

		

		DigitalObject recordObject = StorageUtils.getDigitalObject(storage, storageId);
		Properties objectMetadata = recordObject.getMetadata();
		if (created) {
			File rulesConfigFile = FascinatorHome.getPathFile(harvestPath + rulesConfig);
			JsonSimple rulesConfigJson = new JsonSimple(rulesConfigFile);
			String rulesScript = rulesConfigJson.getString(null, "indexer", "script", "rules");
			String scriptType = rulesConfigJson.getString(null, "indexer", "script", "type");

			File rulesScriptFile = FascinatorHome.getPathFile(harvestPath + rulesScript);

			DigitalObject rulesConfigObject = harvestFileMapService.get(rulesConfigFile);
			DigitalObject rulesObject = harvestFileMapService.get(rulesScriptFile);
			objectMetadata.put("objectId", storageId);
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
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			objectMetadata.store(outStream, "Written by Harvest API");
			StorageUtils.createOrUpdatePayload(recordObject, "TF-OBJ-META",
					IOUtils.toInputStream(new String(outStream.toByteArray()), "utf-8"));
		}
		JsonObject recordMetadata = (JsonObject)record.get("metadata");
		if(StringUtils.isNotBlank(harvestId)) {
			recordMetadata.put("harvestId", harvestId);
		}
		recordMetadata.put("packageType", packageType);
		
		StorageUtils.createOrUpdatePayload(recordObject, payloadId,
				IOUtils.toInputStream(recordMetadata.toString(), "utf-8"));
		
		reindex(storageId);
		JsonObject responseObject = new JsonObject();
		responseObject.put(storageId, "success");
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
		SearchRequest request = new SearchRequest("harvestId:\"" + harvestId+"\"");
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

//	private DigitalObject checkHarvestFile(Storage storage, File file) throws StorageException {
//		String path = file.getPath();
//
//		DigitalObject fileObj = harvestFileMap.get(path);
//
//		if (fileObj == null) {
//			fileObj = StorageUtils.checkHarvestFile(storage, file);
//			if (fileObj == null) {
//				String fileoid = StorageUtils.generateOid(file);
//				fileObj = StorageUtils.getDigitalObject(storage, fileoid);
//			}
//			harvestFileMap.put(path, fileObj);
//		}
//		return fileObj;
//	}

}
