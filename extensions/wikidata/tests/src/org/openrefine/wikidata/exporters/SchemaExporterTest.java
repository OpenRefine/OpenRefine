package org.openrefine.wikidata.exporters;

import static org.openrefine.wikidata.testing.JacksonSerializationTest.assertJsonEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import org.testng.annotations.Test;

import com.google.refine.browsing.Engine;
import com.google.refine.model.Project;
import com.google.refine.tests.RefineTest;

public class SchemaExporterTest extends RefineTest {
	
	private SchemaExporter exporter = new SchemaExporter();
	
    @Test
    public void testNoSchema()
            throws IOException {
        Project project = this.createCSVProject("a,b\nc,d");
        Engine engine = new Engine(project);
        StringWriter writer = new StringWriter();
        Properties properties = new Properties();
        exporter.export(project, properties, engine, writer);
        assertJsonEquals("{\"itemDocuments\":[]}", writer.toString());
    }

}
