package com.googlecode.fascinator.redbox.ws.security;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.security.Verifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.common.FascinatorHome;
import com.googlecode.fascinator.spring.ApplicationContextProvider;

public class TokenBasedVerifier implements Verifier {
	ApiKeyTokenService apiKeyTokenService = null;
	private Logger log = LoggerFactory.getLogger(TokenBasedVerifier.class);

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
		log.debug("Auth header value is: "+ authValue);
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

		log.debug("Token: "+ token);
		return checkToken(token);
	}

	private int checkToken(String token) {
		if (apiKeyTokenService.getAuthorizedKeyMap().containsKey(token)) {
			return Verifier.RESULT_VALID;
		}
		log.debug("Not in key service: "+ token);
		return Verifier.RESULT_INVALID;

	}
}
