package com.googlecode.fascinator.redbox.ws;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.springframework.stereotype.Component;

import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.FascinatorHome;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.storage.StorageUtils;
import com.googlecode.fascinator.common.storage.impl.SpringStorageWrapper;
import com.googlecode.fascinator.spring.ApplicationContextProvider;

/**
 * Spring service to manage the authorized keys for the API
 * 
 * @author abrazzatti
 *
 */
@Component(value = "harvestFileMapService")
public class HarvestFileMapService {

	
	@SuppressWarnings("rawtypes")
	Map<String,DigitalObject> harvestFileMap = new HashMap<String,DigitalObject>();
	
	
	private Storage storage;

	/**
	 * Initial load of the authorized key map
	 * 
	 * @throws IOException
	 */
	public HarvestFileMapService() throws IOException {
		storage = (SpringStorageWrapper)ApplicationContextProvider.getApplicationContext().getBean("fascinatorStorage");
		
	}

	public DigitalObject get(File harvestFile) throws StorageException {
		String path = harvestFile.getPath();

		DigitalObject fileObj = harvestFileMap.get(path);

		if (fileObj == null) {
			fileObj = StorageUtils.checkHarvestFile(storage, harvestFile);
			if (fileObj == null) {
				String fileoid = StorageUtils.generateOid(harvestFile);
				fileObj = StorageUtils.getDigitalObject(storage, fileoid);
			}
			harvestFileMap.put(path, fileObj);
		}
		return fileObj;
	}


}
