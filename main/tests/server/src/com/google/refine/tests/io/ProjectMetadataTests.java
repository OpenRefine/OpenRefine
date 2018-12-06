package com.google.refine.tests.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.ParsingUtilities;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.refine.ProjectMetadata;

public class ProjectMetadataTests {
	  
    private String jsonSaveMode = null;
    private String jsonNonSaveMode = null;
    
    @BeforeSuite
    public void setUpJson() throws IOException {
    	InputStream f = ProjectMetadataTests.class.getClassLoader().getResourceAsStream("example_project_metadata.json");
		jsonNonSaveMode = IOUtils.toString(f);
		f = ProjectMetadataTests.class.getClassLoader().getResourceAsStream("example_project_metadata_save_mode.json");
		jsonSaveMode = IOUtils.toString(f);
    }
	
	@Test
	public void serializeProjectMetadata() throws IOException {
        ProjectMetadata metadata = ParsingUtilities.mapper.readValue(jsonSaveMode, ProjectMetadata.class);
        TestUtils.isSerializedTo(metadata, jsonNonSaveMode);
        TestUtils.isSerializedTo(metadata, jsonSaveMode, true);
	}
	
	@Test
	public void serializeProjectMetadataInDifferentTimezone() throws JsonParseException, JsonMappingException, IOException {
    	TimeZone.setDefault(TimeZone.getTimeZone("JST"));
    	try {
	        ProjectMetadata metadata = ParsingUtilities.mapper.readValue(jsonSaveMode, ProjectMetadata.class);
	        TestUtils.isSerializedTo(metadata, jsonNonSaveMode);
	        TestUtils.isSerializedTo(metadata, jsonSaveMode, true);
    	} finally {
    		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    	}
	}
}
