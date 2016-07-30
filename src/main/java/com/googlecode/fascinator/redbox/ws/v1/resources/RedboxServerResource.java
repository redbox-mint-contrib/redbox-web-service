package com.googlecode.fascinator.redbox.ws.v1.resources;

import java.io.IOException;

import org.restlet.resource.ServerResource;

import com.googlecode.fascinator.HarvestClient;
import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.messaging.MessagingException;

public class RedboxServerResource extends ServerResource {

	protected void reindex(String oid) throws IOException, PluginException, MessagingException{
		String skipReindex = getQueryValue("skipReindex");
		
		if (!"true".equals(skipReindex)) {
			HarvestClient client = new HarvestClient();
			client.reharvest(oid, true);
		}
	}
	
	protected JsonObject getSuccessResponse(String oid) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.put("code", 200);
		if(oid != null) {
			jsonObject.put("oid", oid);
		}
		return jsonObject;
	}
	protected String getSuccessResponseString(String oid) {
		return new JsonSimple(getSuccessResponse(oid)).toString(true);
	}
}
