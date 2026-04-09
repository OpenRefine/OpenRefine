
package org.openrefine.wikibase.exporters;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import com.google.refine.browsing.Engine;
import com.google.refine.exporters.WriterExporter;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

import org.openrefine.wikibase.schema.WikibaseSchema;

public class SchemaExporter implements WriterExporter {

    @Override
    public String getContentType() {
        return "application/octet-stream";
    }

    @Override
    public void export(Project project, Map<String, String> options, Engine engine, Writer writer) throws IOException {
        WikibaseSchema schema = (WikibaseSchema) project.overlayModels.get("wikibaseSchema");
        if (schema == null) {
            schema = new WikibaseSchema();
        }
        ParsingUtilities.mapper.writeValue(writer, schema);
    }

}
