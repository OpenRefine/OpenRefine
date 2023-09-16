
package org.openrefine.wikibase.exporters;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Properties;

import org.testng.annotations.Test;

import org.openrefine.ProjectMetadata;
import org.openrefine.RefineTest;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.GridState;
import org.openrefine.util.TestUtils;

public class SchemaExporterTest extends RefineTest {

    private SchemaExporter exporter = new SchemaExporter();

    @Test
    public void testNoSchema()
            throws IOException {
        // TODO instead of returning an empty (and invalid) schema, we should just return an error
        GridState grid = this.createGrid(
                new String[] { "a", "b" },
                new Serializable[][] { { "c", "d" } });
        Engine engine = new Engine(grid, EngineConfig.ALL_ROWS);

        StringWriter writer = new StringWriter();
        Properties properties = new Properties();
        exporter.export(grid, new ProjectMetadata(), properties, engine, writer);
        TestUtils.assertEqualAsJson("{\"entityEdits\":[],\"siteIri\":null,\"mediaWikiApiEndpoint\":null,\"entityTypeSiteIRI\":{}}",
                writer.toString());
    }

}
