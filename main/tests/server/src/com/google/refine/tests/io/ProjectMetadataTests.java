package com.google.refine.tests.io;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.testng.annotations.Test;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.ProjectMetadata;

public class ProjectMetadataTests {
	@Test
	public void serializeProjectMetadata() throws IOException {
		InputStream f = ProjectMetadataTests.class.getClassLoader().getResourceAsStream("example_project_metadata.json");
		String json = IOUtils.toString(f);
		f = ProjectMetadataTests.class.getClassLoader().getResourceAsStream("example_project_metadata_save_mode.json");
		String fullJson = IOUtils.toString(f);
		
		f = ProjectMetadataTests.class.getClassLoader().getResourceAsStream("example_project_metadata_save_mode.json");
        ProjectMetadata metadata = ParsingUtilities.mapper.readValue(f, ProjectMetadata.class);
        TestUtils.isSerializedTo(metadata, json);
        TestUtils.isSerializedTo(metadata, fullJson, true);
	}
}
