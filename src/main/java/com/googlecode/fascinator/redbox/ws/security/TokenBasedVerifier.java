package com.googlecode.fascinator.redbox.ws.security;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.security.Verifier;

import com.googlecode.fascinator.spring.ApplicationContextProvider;

public class TokenBasedVerifier implements Verifier {
	ApiKeyTokenService apiKeyTokenService = null;

	public TokenBasedVerifier() {
		apiKeyTokenService = (ApiKeyTokenService)ApplicationContextProvider.getApplicationContext().getBean("apiKeyTokenService");
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
		if (apiKeyTokenService.getAuthorizedKeyMap().containsKey(token)) {
			return Verifier.RESULT_VALID;
		}

		return Verifier.RESULT_INVALID;

	}
}
