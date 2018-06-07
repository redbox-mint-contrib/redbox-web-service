package com.googlecode.fascinator.redbox.ws;

import java.io.IOException;

import org.restlet.Restlet;
import org.restlet.data.ChallengeScheme;
import org.restlet.ext.swagger.Swagger2SpecificationRestlet;
import org.restlet.ext.swagger.SwaggerApplication;
import org.restlet.routing.Router;
import org.restlet.security.ChallengeAuthenticator;

import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.redbox.ws.security.TokenBasedVerifier;
import com.googlecode.fascinator.redbox.ws.v1.resources.DatastreamResource;
import com.googlecode.fascinator.redbox.ws.v1.resources.DeleteObjectResource;
import com.googlecode.fascinator.redbox.ws.v1.resources.HarvestResource;
import com.googlecode.fascinator.redbox.ws.v1.resources.InfoResource;
import com.googlecode.fascinator.redbox.ws.v1.resources.ListDatastreamResource;
import com.googlecode.fascinator.redbox.ws.v1.resources.ObjectMetadataResource;
import com.googlecode.fascinator.redbox.ws.v1.resources.ObjectResource;
import com.googlecode.fascinator.redbox.ws.v1.resources.QueueMessageResource;
import com.googlecode.fascinator.redbox.ws.v1.resources.RecordMetadataResource;
import com.googlecode.fascinator.redbox.ws.v1.resources.SearchByIndexResource;
import com.googlecode.fascinator.redbox.ws.v1.resources.SearchResource;

public class ReDBoxWebServiceApplication extends SwaggerApplication {
	@Override
	public synchronized Restlet createInboundRoot() {
		ChallengeAuthenticator guard = new ChallengeAuthenticator(getContext(), ChallengeScheme.HTTP_OAUTH_BEARER,
				"redboxRealm");
		TokenBasedVerifier verifier = new TokenBasedVerifier();
		guard.setVerifier(verifier);
		Router baseRouter = new Router(getContext());

		Router privateRouter = new Router(getContext());
		// Define the v1 API routes
		defineV1Routes(privateRouter);
		defineV1_1Routes(privateRouter);
		defineV2Routes(privateRouter);
		
		String apiPath = "http://localhost:9000/redbox/api/v1";
		try {
			JsonSimpleConfig config = new JsonSimpleConfig();
			String urlBase = config.getString("http://localhost:9000/redbox", "urlBase");
			apiPath = urlBase + "/api/v1";
		} catch (IOException e) {
		}
		Swagger2SpecificationRestlet swagger2SpecificationRestlet = new Swagger2SpecificationRestlet(this);
		swagger2SpecificationRestlet.setBasePath(apiPath);
		swagger2SpecificationRestlet.attach(baseRouter);

		guard.setNext(privateRouter);
		baseRouter.attach(guard);
		return baseRouter;
	}

	private void defineV1_1Routes(Router router) {

		router.attach("/v1.1/recordmetadata/{oid}", RecordMetadataResource.class);
		router.attach("/v1.1/objectmetadata/{oid}", ObjectMetadataResource.class);
		router.attach("/v1.1/datastream/{oid}/list", ListDatastreamResource.class);
		router.attach("/v1.1/datastream/{oid}", DatastreamResource.class);
		router.attach("/v1.1/object/{packageType}", ObjectResource.class);
		router.attach("/v1.1/object/{oid}/delete", DeleteObjectResource.class);
		router.attach("/v1.1/info", InfoResource.class);
		router.attach("/v1.1/search", SearchResource.class);
		router.attach("/v1.1/search/{index}", SearchByIndexResource.class);
		router.attach("/v1.1/messaging/{messageQueue}", QueueMessageResource.class);
		router.attach("/v1.1/harvest/{packageType}", HarvestResource.class);
	}

	private void defineV1Routes(Router router) {
		router.attach("/v1/recordmetadata/{oid}", RecordMetadataResource.class);
		router.attach("/v1/objectmetadata/{oid}", ObjectMetadataResource.class);
		router.attach("/v1/datastream/{oid}/list", ListDatastreamResource.class);
		router.attach("/v1/datastream/{oid}", DatastreamResource.class);
		router.attach("/v1/object/{packageType}", ObjectResource.class);
		router.attach("/v1/object/{oid}/delete", DeleteObjectResource.class);
		router.attach("/v1/info", InfoResource.class);
		router.attach("/v1/search", SearchResource.class);
		router.attach("/v1/search/{index}", SearchByIndexResource.class);
		router.attach("/v1/messaging/{messageQueue}", QueueMessageResource.class);
	}
	
	private void defineV2Routes(Router router) {
		router.attach("/v2/recordmetadata/{oid}", RecordMetadataResource.class);
		router.attach("/v2/objectmetadata/{oid}", ObjectMetadataResource.class);
		router.attach("/v2/datastream/{oid}/list", com.googlecode.fascinator.redbox.ws.v2.resources.ListDatastreamResource.class);
		router.attach("/v2/datastream/{oid}", DatastreamResource.class);
		router.attach("/v2/object/{packageType}", ObjectResource.class);
		router.attach("/v2/object/{oid}/delete", DeleteObjectResource.class);
		router.attach("/v2/info", InfoResource.class);
		router.attach("/v2/search", SearchResource.class);
		router.attach("/v2/search/{index}", SearchByIndexResource.class);
		router.attach("/v2/query", com.googlecode.fascinator.redbox.ws.v2.resources.QueryResource.class);
		router.attach("/v2/messaging/{messageQueue}", QueueMessageResource.class);
		router.attach("/v2/harvest/{packageType}", HarvestResource.class);
	}

	@Override
	public String getName() {
		return "ReDBox Web Service";
	}

	@Override
	public String getAuthor() {
		return "QCIF";
	}

	@Override
	public String getDescription() {
		return "Web Service that allows interaction with ReDBox records";
	}
}
