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

import static org.openrefine.wikidata.commands.CommandUtilities.respondError;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrefine.wikidata.qa.EditInspector;
import org.openrefine.wikidata.qa.QAWarningStore;
import org.openrefine.wikidata.schema.WikibaseSchema;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.scheduler.WikibaseAPIUpdateScheduler;

import com.google.refine.browsing.Engine;
import com.google.refine.commands.Command;
import com.google.refine.model.Project;

public class PreviewWikibaseSchemaCommand extends Command {
	
	/**
	 * This command uses POST but is left CSRF-unprotected since it does not
	 * incur a side effect or state change in the backend.
	 * The reason why it uses POST is to make sure large schemas and engines
	 * can be passed as parameters.
	 */
    
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
                } catch (IOException e) {
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

            // Inspect the edits and generate warnings
            EditInspector inspector = new EditInspector(warningStore);
            inspector.inspect(editBatch);
            
            // Dump the first 10 edits, scheduled with the default scheduler
            WikibaseAPIUpdateScheduler scheduler = new WikibaseAPIUpdateScheduler();
            List<ItemUpdate> nonNullEdits = scheduler.schedule(editBatch).stream()
                    .filter(e -> !e.isNull())
                    .collect(Collectors.toList());
            List<ItemUpdate> firstEdits = nonNullEdits.stream()
                    .limit(10)
                    .collect(Collectors.toList());

            PreviewResults previewResults = new PreviewResults(
                    warningStore.getWarnings(),
                    warningStore.getMaxSeverity(),
                    warningStore.getNbWarnings(),
                    nonNullEdits.size(), firstEdits);
            respondJSON(response, previewResults);
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
