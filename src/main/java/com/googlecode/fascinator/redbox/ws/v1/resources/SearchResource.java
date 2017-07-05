package com.googlecode.fascinator.redbox.ws.v1.resources;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.api.indexer.IndexerException;
import com.googlecode.fascinator.api.indexer.SearchRequest;
import com.googlecode.fascinator.spring.ApplicationContextProvider;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Api(value = "search", description="Search ReDBox's search index")
public class SearchResource extends RedboxServerResource {

	@ApiOperation(value = "Search ReDBox's search index", tags = "search")
	@ApiResponses({
        @ApiResponse(code = 200, message = "Search results returned"),
        @ApiResponse(code = 500, message = "General Error", response = Exception.class)
	})
	@Get
	public Representation searchIndex() throws IndexerException, IOException{
		Indexer indexer = (Indexer) ApplicationContextProvider.getApplicationContext().getBean("fascinatorIndexer");
		String query = getQueryValue("q");
		String responseFormat = getQueryValue("wt");
		//default solr response to json
		if(StringUtils.isBlank(responseFormat)) {
			responseFormat = "json";
		}
		SearchRequest request = new SearchRequest(query);
		request.addParam("wt", responseFormat);
		Form form = getQuery();
		Iterator<Parameter> formInterator = form.iterator();
		while(formInterator.hasNext()) {
			Parameter param = formInterator.next();
			if(!"q".equals(param.getName()) && !"wt".equals(param.getName())){
				request.addParam(param.getName(), param.getValue());
			}
		}
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		indexer.search(request, byteArrayOutputStream);
		return new StringRepresentation(new String(byteArrayOutputStream.toByteArray(),Charset.forName("utf-8")));
	}



}
