package com.googlecode.fascinator.redbox.ws.v1.resources;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.restlet.resource.Get;

import com.googlecode.fascinator.common.FascinatorHome;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Api(value = "info", description = "Information about the ReDBox instance")
public class InfoResource extends RedboxServerResource {

	@SuppressWarnings("unchecked")
	@ApiOperation(value = "get information about the ReDBox instance", tags = "info")
	@ApiResponses({
        @ApiResponse(code = 200, message = "The datastreams are listed"),
        @ApiResponse(code = 500, message = "Server configuration not found", response = IOException.class)
	})
	@Get("json")
	public String getServerInformation() throws IOException{
		JsonObject responseObject = getSuccessResponse(null);
		JsonSimpleConfig config = new JsonSimpleConfig();
		responseObject.put("institution", config.getString(null, "identity","institution"));
		responseObject.put("applicationVersion", config.getString(null, "redbox.version.string"));

		JSONArray packageTypes = new JSONArray();
		if ("mint".equals(config.getString(null, "system"))) {
			packageTypes.addAll(getPackageTypesFromFileSystem());
		} else {
			JsonObject packageTypesObject = config.getObject("portal", "packageTypes");
			packageTypes.addAll(packageTypesObject.keySet());
		}
		
		
		responseObject.put("packageTypes", packageTypes);
		return new JsonSimple(responseObject).toString(true);

	}

	private List<String> getPackageTypesFromFileSystem() {
		List<String> packageTypes = new ArrayList<String>();
		File harvestDir = FascinatorHome.getPathFile("harvest");
		@SuppressWarnings("unchecked")
		Collection<File> jsonFiles = FileUtils.listFiles(harvestDir, new String[]{"json"}, false);
		for (File file : jsonFiles) {
			packageTypes.add(file.getName().substring(0,file.getName().length()-5));
		}
		return packageTypes;
	}
}
