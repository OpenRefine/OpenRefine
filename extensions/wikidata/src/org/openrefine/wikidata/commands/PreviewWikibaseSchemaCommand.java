/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package org.openrefine.wikidata.commands;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;
import org.openrefine.wikidata.exporters.QuickStatementsExporter;
import org.openrefine.wikidata.qa.EditInspector;
import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.qa.QAWarningStore;
import org.openrefine.wikidata.schema.WikibaseSchema;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.utils.FirstLinesExtractor;

import com.google.refine.browsing.Engine;
import com.google.refine.commands.Command;
import com.google.refine.model.Project;

public class PreviewWikibaseSchemaCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            Project project = getProject(request);

            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");

            String jsonString = request.getParameter("schema");

            WikibaseSchema schema = null;
            if (jsonString != null) {
                try {
                    schema = WikibaseSchema.reconstruct(jsonString);
                } catch (JSONException e) {
                    respond(response, "error", "Wikibase schema could not be parsed.");
                    return;
                }
            } else {
                schema = (WikibaseSchema) project.overlayModels.get("wikibaseSchema");
            }
            if (schema == null) {
                respond(response, "error", "No Wikibase schema provided.");
                return;
            }

            QAWarningStore warningStore = new QAWarningStore();

            // Evaluate project
            Engine engine = getEngine(request, project);
            List<ItemUpdate> editBatch = schema.evaluate(project, engine, warningStore);

            StringWriter sb = new StringWriter(2048);
            JSONWriter writer = new JSONWriter(sb);
            writer.object();

            {
                StringWriter stringWriter = new StringWriter();

                // Inspect the edits and generate warnings
                EditInspector inspector = new EditInspector(warningStore);
                inspector.inspect(editBatch);
                writer.key("warnings");
                writer.array();
                for (QAWarning warning : warningStore.getWarnings()) {
                    warning.write(writer, new Properties());
                }
                writer.endArray();
                
                // Add max warning level
                writer.key("max_severity");
                writer.value(warningStore.getMaxSeverity().toString());

                // this is not the length of the warnings array written before,
                // but the total number of issues raised (before deduplication)
                writer.key("nb_warnings");
                writer.value(warningStore.getNbWarnings());

                // Export to QuickStatements
                QuickStatementsExporter exporter = new QuickStatementsExporter();
                exporter.translateItemList(editBatch, stringWriter);

                writer.key("quickstatements");
                writer.value(FirstLinesExtractor.extractFirstLines(stringWriter.toString(), 50));
            }

            writer.endObject();

            respond(response, sb.toString());
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
