package com.googlecode.fascinator.redbox.ws.security;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.springframework.stereotype.Component;

import com.googlecode.fascinator.common.FascinatorHome;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;


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
	
	/**
	 * Initial load of the authorized key map
	 * 
	 * @throws IOException
	 */
	public ApiKeyTokenService() throws IOException{
		File apiKeysFile = FascinatorHome.getPathFile(SECURITY_APIKEYS_JSON_PATH);
		
		if(!apiKeysFile.exists()) {
			return;
		}
		JsonSimple apiKeyJson = new JsonSimple(apiKeysFile);
		JSONArray clients = apiKeyJson.getArray("api", "clients");

		Map authorizedKeyMap = new HashMap();
		if (clients != null) {
			for (Object client : clients) {
				JsonObject clientObject = (JsonObject) client;
				authorizedKeyMap.put((String) clientObject.get("apiKey"), (String) clientObject.get("username"));
			}
		}
		this.authorizedKeyMap =  Collections.synchronizedMap(authorizedKeyMap);
	}

	@SuppressWarnings("rawtypes")
	public Map getAuthorizedKeyMap() {
		return authorizedKeyMap;
	}

	@SuppressWarnings("unchecked")
	public void updateAndSaveKeyMap(@SuppressWarnings("rawtypes") Map keyMap) throws IOException {
		File apiKeysFile = FascinatorHome.getPathFile(SECURITY_APIKEYS_JSON_PATH);
		FileUtils.writeStringToFile(apiKeysFile, new JsonSimple(new JsonObject(keyMap)).toString(true));
		authorizedKeyMap = Collections.synchronizedMap(keyMap);
	}
	
	
}
