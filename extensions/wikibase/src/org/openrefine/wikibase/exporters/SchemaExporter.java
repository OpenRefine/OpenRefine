
package org.openrefine.wikibase.exporters;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.openrefine.ProjectMetadata;
import org.openrefine.browsing.Engine;
import org.openrefine.exporters.WriterExporter;
import org.openrefine.model.Grid;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.wikibase.schema.WikibaseSchema;

public class SchemaExporter implements WriterExporter {

    @Override
    public String getContentType() {
        return "application/octet-stream";
    }

    @Override
    public void export(Grid grid, ProjectMetadata projectMetadata, long projectId, Map<String, String> options,
            Engine engine, Writer writer) throws IOException {
        WikibaseSchema schema = (WikibaseSchema) grid.getOverlayModels().get("wikibaseSchema");
        if (schema == null) {
            schema = new WikibaseSchema();
        }
        ParsingUtilities.mapper.writeValue(writer, schema);
    }

}
