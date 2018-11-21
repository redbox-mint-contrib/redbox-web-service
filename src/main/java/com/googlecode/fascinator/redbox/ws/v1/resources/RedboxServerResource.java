package com.googlecode.fascinator.redbox.ws.v1.resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.HarvestClient;
import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Payload;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.messaging.MessagingException;
import com.googlecode.fascinator.common.messaging.MessagingServices;
import com.googlecode.fascinator.spring.ApplicationContextProvider;

public class RedboxServerResource extends ServerResource {

	private Logger log = LoggerFactory.getLogger(RedboxServerResource.class);
	private HarvestClient client;
	protected void reindex(String oid) throws IOException, PluginException, MessagingException {
		String skipReindex = getQueryValue("skipReindex");

		if (!"true".equals(skipReindex)) {
			if(this.client == null) {
				client = (HarvestClient) ApplicationContextProvider.getApplicationContext().getBean("harvestClient");
			}
			client.reharvest(oid, true);
		}
	}

	protected void reindex(String oid, DigitalObject configObj, Storage storage)
			throws IOException, PluginException, MessagingException {
		String skipReindex = getQueryValue("skipReindex");

		if (!"true".equals(skipReindex)) {
			if(this.client == null) {
				client = (HarvestClient) ApplicationContextProvider.getApplicationContext().getBean("harvestClient");
			}
			client.reharvest(oid, configObj, true);
			client = null;
		}

	}

	protected JsonObject getSuccessResponse(String oid) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.put("code", 200);
		if (oid != null) {
			jsonObject.put("oid", oid);
		}
		return jsonObject;
	}

	protected String getSuccessResponseString(String oid) {
		return new JsonSimple(getSuccessResponse(oid)).toString(true);
	}
}
