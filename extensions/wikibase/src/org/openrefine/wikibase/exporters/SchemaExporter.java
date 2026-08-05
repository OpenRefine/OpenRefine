
package org.openrefine.wikibase.exporters;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import com.google.refine.browsing.Engine;
import com.google.refine.exporters.ExporterException;
import com.google.refine.exporters.WriterExporter;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

import org.openrefine.wikibase.schema.WikibaseSchema;

public class SchemaExporter implements WriterExporter {

    public static final String noSchemaErrorMessage = "No schema was provided. You need to align your project with Wikibase first.";

    @Override
    public String getContentType() {
        return "application/octet-stream";
    }

    @Override
    public void export(Project project, Properties options, Engine engine, Writer writer) throws IOException {
        WikibaseSchema schema = (WikibaseSchema) project.overlayModels.get("wikibaseSchema");
        if (schema == null) {
            throw new ExporterException(noSchemaErrorMessage);
        }
        ParsingUtilities.mapper.writeValue(writer, schema);
    }

}
