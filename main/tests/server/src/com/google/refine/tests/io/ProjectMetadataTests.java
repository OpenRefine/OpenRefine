package com.google.refine.tests.io;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.testng.annotations.Test;

import com.google.refine.model.metadata.ProjectMetadata;
import com.google.refine.tests.util.TestUtils;

public class ProjectMetadataTests {
	@Test
	public void serializeProjectMetadata() throws IOException {
		ProjectMetadata metadata = new ProjectMetadata();
		InputStream f = ProjectMetadataTests.class.getClassLoader().getResourceAsStream("example_project_metadata.json");
		String json = IOUtils.toString(f);
		f = ProjectMetadataTests.class.getClassLoader().getResourceAsStream("example_project_metadata.json");
        metadata.loadFromStream(f);
        TestUtils.isSerializedTo(metadata, json);
	}
}
