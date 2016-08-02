package com.googlecode.fascinator.redbox.ws.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.security.Verifier;

import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimpleConfig;

public class TokenBasedVerifier implements Verifier {
	Map<String, String> authorizedKeyMap;

	public TokenBasedVerifier() {
		authorizedKeyMap = new HashMap<String, String>();
		try {
			JsonSimpleConfig config = new JsonSimpleConfig();
			JSONArray clients = config.getArray("api", "clients");

			if (clients != null) {
				for (Object client : clients) {
					JsonObject clientObject = (JsonObject) client;
					authorizedKeyMap.put((String) clientObject.get("apiKey"), (String) clientObject.get("username"));
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Verifies that the token passed is valid.
	 * 
	 * TODO: While this works, this isn't the proper "Restlet" way to do it and
	 * should utilise the framework more.
	 */
	public int verify(Request request, Response response) {
		String authValue = request.getHeaders().getValues("Authorization");
		if (authValue == null) {
			return Verifier.RESULT_MISSING;
		}
		String[] tokenValues = authValue.split(" ");
		if (tokenValues.length < 2) {
			return Verifier.RESULT_MISSING;
		}
		if (!"Bearer".equals(tokenValues[0])) {
			return Verifier.RESULT_INVALID;
		}
		String token = tokenValues[1];

		return checkToken(token);
	}

	private int checkToken(String token) {
		if (authorizedKeyMap.containsKey(token)) {
			return Verifier.RESULT_VALID;
		}

		return Verifier.RESULT_INVALID;

	}
}
