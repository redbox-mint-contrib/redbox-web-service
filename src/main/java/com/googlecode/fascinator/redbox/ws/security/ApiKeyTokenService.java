package com.googlecode.fascinator.redbox.ws.security;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.springframework.stereotype.Component;

import com.googlecode.fascinator.common.FascinatorHome;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;

/**
 * Spring service to manage the authorized keys for the API
 * 
 * @author abrazzatti
 *
 */
@Component(value = "apiKeyTokenService")
public class ApiKeyTokenService {

	private static final String SECURITY_APIKEYS_JSON_PATH = "security/apikeys.json";
	@SuppressWarnings("rawtypes")
	Map authorizedKeyMap = new HashMap();
	List clients = new ArrayList();
	File apiKeysFile;

	/**
	 * Initial load of the authorized key map
	 * 
	 * @throws IOException
	 */
	public ApiKeyTokenService() throws IOException {
		JsonSimpleConfig sysconfig = new JsonSimpleConfig();
		String apiKeyFilePath = sysconfig.getString(FascinatorHome.getPath(SECURITY_APIKEYS_JSON_PATH), "api",
				"apiKeyFile");
		this.apiKeysFile = new File(apiKeyFilePath);

		if (!this.apiKeysFile.exists()) {
			updateAndSaveKeys(new JSONArray());
		}
		JsonSimple apiKeyJson = new JsonSimple(apiKeysFile);
		this.clients = apiKeyJson.getArray("api", "clients");

		initialiseKeyMap();
	}

	public List getClients() {
		return clients;
	}

	private void initialiseKeyMap() {
		Map authorizedKeyMap = new HashMap();
		if (clients != null) {
			for (Object client : clients) {
				JsonObject clientObject = (JsonObject) client;
				authorizedKeyMap.put((String) clientObject.get("apiKey"), (String) clientObject.get("username"));
			}
		}
		this.authorizedKeyMap = Collections.synchronizedMap(authorizedKeyMap);
	}

	@SuppressWarnings("rawtypes")
	public Map getAuthorizedKeyMap() {
		return authorizedKeyMap;
	}

	@SuppressWarnings("unchecked")
	public void updateAndSaveKeys(@SuppressWarnings("rawtypes") JSONArray keys) throws IOException {
		JsonObject jsonObject = new JsonObject();
		JsonObject apiJsonObject = new JsonObject();

		apiJsonObject.put("clients", keys);
		jsonObject.put("api", apiJsonObject);
		FileUtils.writeStringToFile(this.apiKeysFile, new JsonSimple(jsonObject).toString(true));
		this.clients = keys;
		initialiseKeyMap();
	}

}
