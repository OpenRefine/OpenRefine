
package org.openrefine.wikibase.exporters;

import static org.testng.Assert.assertThrows;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Properties;

import org.testng.annotations.Test;

import com.google.refine.browsing.Engine;
import com.google.refine.exporters.ExporterException;
import com.google.refine.model.Project;

import org.openrefine.wikibase.testing.WikidataRefineTest;

public class SchemaExporterTest extends WikidataRefineTest {

    private SchemaExporter exporter = new SchemaExporter();

    @Test
    public void testNoSchema() throws IOException {
        Project project = this.createProject(
                new String[] { "a", "b" },
                new Serializable[][] {
                        { "c", "d" }
                });
        Engine engine = new Engine(project);
        StringWriter writer = new StringWriter();
        Properties properties = new Properties();
        assertThrows(ExporterException.class, () -> exporter.export(project, properties, engine, writer));
    }

}
