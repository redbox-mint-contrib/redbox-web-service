package com.googlecode.fascinator.redbox.ws.v1.resources;

import java.io.IOException;

import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Post;

import com.googlecode.fascinator.common.messaging.MessagingException;
import com.googlecode.fascinator.common.messaging.MessagingServices;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Api(value = "messaging", description = "Operations to interact with the asynchronous message queue")
public class QueueMessageResource extends RedboxServerResource {

	

	@ApiOperation(value = "Queues a message on the specified message queue", tags = "messaging")
	@ApiResponses({ @ApiResponse(code = 200, message = "The record's metadata is updated"),
			@ApiResponse(code = 500, message = "General Error", response = Exception.class) })
	@Post("json")
	public String sendMessageToQueue(JsonRepresentation data) throws IOException, MessagingException {
		MessagingServices ms = MessagingServices.getInstance();
		String messageQueue = getAttribute("messageQueue");
		
		String message = data.getText();
		ms.queueMessage(messageQueue, message);

		return getSuccessResponseString(null);
	}

}
