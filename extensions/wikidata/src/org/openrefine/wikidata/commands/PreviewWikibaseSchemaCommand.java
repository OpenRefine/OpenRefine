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
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.openrefine.wikidata.qa.EditInspector;
import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.qa.QAWarningStore;
import org.openrefine.wikidata.schema.WikibaseSchema;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.scheduler.WikibaseAPIUpdateScheduler;

import static org.openrefine.wikidata.commands.CommandUtilities.respondError;

import com.fasterxml.jackson.databind.ObjectMapper;

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
                    respondError(response, "Wikibase schema could not be parsed.");
                    return;
                }
            } else {
                schema = (WikibaseSchema) project.overlayModels.get("wikibaseSchema");
            }
            if (schema == null) {
                respondError(response, "No Wikibase schema provided.");
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

                // Dump the first 10 edits, scheduled with the default scheduler
                WikibaseAPIUpdateScheduler scheduler = new WikibaseAPIUpdateScheduler();
                List<ItemUpdate> nonNullEdits = scheduler.schedule(editBatch).stream()
                        .filter(e -> !e.isNull())
                        .collect(Collectors.toList());
                writer.key("edit_count");
                writer.value(nonNullEdits.size());
                List<ItemUpdate> firstEdits = nonNullEdits.stream()
                        .limit(10)
                        .collect(Collectors.toList());
                ObjectMapper mapper = new ObjectMapper();
                String firstEditsJson = mapper.writeValueAsString(firstEdits);

                writer.key("edits_preview");
                writer.value(new JSONArray(firstEditsJson));
            }

            writer.endObject();

            respond(response, sb.toString());
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
