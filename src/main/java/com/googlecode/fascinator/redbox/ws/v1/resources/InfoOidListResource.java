package com.googlecode.fascinator.redbox.ws.v1.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Payload;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.storage.StorageUtils;
import com.googlecode.fascinator.spring.ApplicationContextProvider;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.restlet.resource.Get;

import java.io.IOException;
import java.util.Set;

@Api(value = "infooidlist", description = "Lists all the oids in the system")
public class InfoOidListResource extends RedboxServerResource {

    private Gson gson;

    public InfoOidListResource() {
        gson = new GsonBuilder().create();
    }

    @ApiOperation(value = "Lists all the oids in the system", tags = "infooidlist")
    @ApiResponses({ @ApiResponse(code = 200, message = "The list of oids is returned"),
            @ApiResponse(code = 500, message = "General Error", response = Exception.class) })
    @Get("json")
    public String getOidList() throws StorageException, IOException {
        Storage storage = (Storage) ApplicationContextProvider.getApplicationContext().getBean("fascinatorStorage");

        Set<String> oidList = storage.getObjectIdList();
        JsonObject responseObject = new JsonObject();
        responseObject.put("numFound",oidList.size());
        responseObject.put("oids", oidList);

        return gson.toJson(responseObject);
    }

}
