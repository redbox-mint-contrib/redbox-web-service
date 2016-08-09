package com.googlecode.fascinator.redbox.ws.v1.resources;

import java.io.IOException;

import org.restlet.resource.Delete;

import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.common.messaging.MessagingException;
import com.googlecode.fascinator.spring.ApplicationContextProvider;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Api(value = "object", description = "Operations on ReDBox Objects")
public class DeleteObjectResource extends RedboxServerResource {

	@ApiOperation(value = "Delete an existing ReDBox object", tags = "object")
	@ApiResponses({ @ApiResponse(code = 200, message = "The object is deleted"),
			@ApiResponse(code = 500, message = "General Error", response = Exception.class) })
	@Delete
	public String deleteObjectResource() throws IOException, PluginException, MessagingException {
		Storage storage = (Storage) ApplicationContextProvider.getApplicationContext().getBean("fascinatorStorage");
		Indexer indexer = (Indexer) ApplicationContextProvider.getApplicationContext().getBean("fascinatorIndexer");
		String oid = getAttribute("oid");
		storage.removeObject(oid);
		indexer.remove(oid);
		return getSuccessResponseString(oid);
	}
}
