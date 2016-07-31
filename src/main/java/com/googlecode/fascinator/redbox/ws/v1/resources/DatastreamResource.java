package com.googlecode.fascinator.redbox.ws.v1.resources;

import java.io.IOException;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.IOUtils;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.ByteArrayRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Payload;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.messaging.MessagingException;
import com.googlecode.fascinator.common.storage.StorageUtils;
import com.googlecode.fascinator.spring.ApplicationContextProvider;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiImplicitParam;
import com.wordnik.swagger.annotations.ApiImplicitParams;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Api(value = "datastream", description = "Produces a datastream")
public class DatastreamResource extends RedboxServerResource {

	@ApiOperation(value = "Get a datastream from a ReDBox object", tags = "datastream")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "datastreamId", value="The identifier of the datastream", required = true, allowMultiple = false, dataType = "string") })
	@ApiResponses({ @ApiResponse(code = 200, message = "The datastream is retrieved"),
			@ApiResponse(code = 500, message = "General Error", response = Exception.class) })
	@Get("application/octet-stream")
	public Representation getDatastream() throws IOException {
		try {
			Storage storage = (Storage) ApplicationContextProvider.getApplicationContext().getBean("fascinatorStorage");
			String oid = getAttribute("oid");
			String payloadId = getQueryValue("datastreamId");
			if (payloadId != null) {
				DigitalObject digitalObject = StorageUtils.getDigitalObject(storage, oid);
				Payload payload = digitalObject.getPayload(payloadId);

				return new ByteArrayRepresentation(IOUtils.toByteArray(payload.open()));
			} else {
				throw new ResourceException(400, "Call requires a datastreamId value");
			}
		} catch (StorageException e) {
			throw new ResourceException(500, e, e.getMessage());
		}
	}

	@ApiOperation(value = "Create or update a datastream in a ReDBox object", tags = "datastream")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "skipReindex", value="Skip the reindex process. Useful if you are batching many changes to a ReDBox object at once.", required = false, allowMultiple = false, defaultValue = "false", dataType = "string"),
		@ApiImplicitParam(name = "datastreamId",  value="The identifier of the datastream", required = true, allowMultiple = false, dataType = "string") })
	@ApiResponses({ @ApiResponse(code = 200, message = "The datastream is created or updated"),
			@ApiResponse(code = 500, message = "General Error", response = Exception.class) })
	@Post
	public String updateDatastream(Representation entity)
			throws FileUploadException, IOException, PluginException, MessagingException {

		if (entity != null && MediaType.MULTIPART_FORM_DATA.equals(entity.getMediaType(), true)) {
			Storage storage = (Storage) ApplicationContextProvider.getApplicationContext().getBean("fascinatorStorage");
			String oid = getAttribute("oid");
			String payloadId = getQueryValue("datastreamId");
			DigitalObject digitalObject = StorageUtils.getDigitalObject(storage, oid);

			DiskFileItemFactory factory = new DiskFileItemFactory();
			factory.setSizeThreshold(1000240);
			RestletFileUpload upload = new RestletFileUpload(factory);
			FileItemIterator fileIterator = upload.getItemIterator(entity);
			while (fileIterator.hasNext()) {
				FileItemStream fi = fileIterator.next();
				StorageUtils.createOrUpdatePayload(digitalObject, payloadId, fi.openStream());
			}
			reindex(oid);
			return getSuccessResponseString(oid);
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE);
		}

	}

	@ApiOperation(value = "Delete a datastream in a ReDBox object", tags = "datastream")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "skipReindex", value="Skip the reindex process. Useful if you are batching many changes to a ReDBox object at once.", required = false, allowMultiple = false, defaultValue = "false", dataType = "string"),
		@ApiImplicitParam(name = "datastreamId",  value="The identifier of the datastream", required = true, allowMultiple = false, dataType = "string") })
	@Delete
	public String deleteDatastream() throws FileUploadException, IOException, PluginException, MessagingException {
		Storage storage = (Storage) ApplicationContextProvider.getApplicationContext().getBean("fascinatorStorage");
		String oid = getAttribute("oid");
		String payloadId = getQueryValue("datastreamId");
		DigitalObject digitalObject = StorageUtils.getDigitalObject(storage, oid);

		try {
			@SuppressWarnings("unused")
			Payload payload = digitalObject.getPayload(payloadId);
		} catch (StorageException e) {
			throw new ResourceException(404, e, "Datastream does not exist in the object");
		}
		digitalObject.removePayload(payloadId);
		reindex(oid);
		return getSuccessResponseString(oid);
	}

}
