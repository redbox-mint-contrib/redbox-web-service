package com.googlecode.fascinator.redbox.ws.v2.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.BsonDocument;
import org.json.simple.JSONArray;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.googlecode.fascinator.api.indexer.IndexerException;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Payload;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.storage.impl.SpringStorageWrapper;
import com.googlecode.fascinator.redbox.ws.v1.resources.RedboxServerResource;
import com.googlecode.fascinator.spring.ApplicationContextProvider;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import au.com.redboxresearchdata.fascinator.storage.mongo.MongoStorage;

@Api(value = "metadata", description = "Display the related records for the given record")
public class RecordRelationshipsResource extends RedboxServerResource {

	private Gson gson;

	public RecordRelationshipsResource() {
		gson = new GsonBuilder().create();
	}

	@ApiOperation(value = "Display the related records for the given record", tags = "metadata")
	@ApiResponses({ @ApiResponse(code = 200, message = ""),
			@ApiResponse(code = 500, message = "General Error", response = Exception.class) })
	@Post("json")
	public Representation queryMongo(JsonRepresentation data) throws IndexerException, IOException {
		SpringStorageWrapper storageWrapper = (SpringStorageWrapper) ApplicationContextProvider.getApplicationContext()
				.getBean("fascinatorStorage");
		MongoStorage storage = (MongoStorage) storageWrapper.getStoragePlugin();
		JsonSimple dataObject = new JsonSimple(data.getText());
		String oid = getAttribute("oid");
		if (oid == null) {
			throw new RuntimeException("oid must be provided");
		}

		DigitalObject object;
		try {
			object = storage.getObject(oid);
			if (object == null) {
				throw new RuntimeException("No record found for oid: " + oid);
			}

			Payload payload = object.getPayload("metadata.tfpackage");
			JsonSimple metadata = new JsonSimple(payload.open());

			String collection = metadata.getString(null, "metaMetadata", "type");

			List<BsonDocument> pipeline = new ArrayList<BsonDocument>();
			pipeline.add(BsonDocument.parse("  { $match: { 'redboxOid': '" + oid + "'}}"));
			JSONArray relationships = dataObject.getArray("relationships");
			List<String> relatedCollections = new ArrayList<String>();
			if (relationships != null) {
				for (Object relationshipObject : relationships) {
					JsonObject relationship = (JsonObject) relationshipObject;

					String relatedCollection = (String) relationship.get("collection");
					String localField = (String) relationship.get("localField");
					if (localField == null) {
						localField = "redboxOid";
					}
					String foreignField = (String) relationship.get("foreignField");

					pipeline.add(
							BsonDocument.parse("{$lookup: {from: '" + relatedCollection + "',localField: '" + localField
									+ "',foreignField: '" + foreignField + "', as: '" + relatedCollection + "'}}"));
					pipeline.add(BsonDocument
							.parse("{$unwind: {path: '$" + relatedCollection + "',preserveNullAndEmptyArrays:true}}"));
					relatedCollections.add(relatedCollection);
				}
			}
			// Bring related collections back together and return to client
			String groupingString = "{$group: {";
			for (String relatedCollection : relatedCollections) {
				groupingString += relatedCollection + ": {'$push':'$" + relatedCollection + "' },";
			}
			groupingString += "_id: '$redboxOid',";
			groupingString += "redboxOid:  { '$first': '$redboxOid'}}}";
			pipeline.add(BsonDocument.parse(groupingString));

			JsonObject result = new JsonSimple(storage.aggregate(collection, pipeline).first().toJson()).getJsonObject();
			return new JsonRepresentation(gson.toJson(result));
		} catch (StorageException e) {
			throw new RuntimeException(e);
		}

	}

}
