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

package org.openrefine.wikibase.commands;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrefine.browsing.Engine;
import org.openrefine.commands.Command;
import org.openrefine.model.Project;
import org.openrefine.wikibase.manifests.Manifest;
import org.openrefine.wikibase.manifests.ManifestException;
import org.openrefine.wikibase.manifests.ManifestParser;
import org.openrefine.wikibase.qa.EditInspector;
import org.openrefine.wikibase.qa.QAWarningStore;
import org.openrefine.wikibase.schema.WikibaseSchema;
import org.openrefine.wikibase.schema.validation.ValidationError;
import org.openrefine.wikibase.schema.validation.ValidationState;
import org.openrefine.wikibase.updates.EntityEdit;
import org.openrefine.wikibase.updates.scheduler.WikibaseAPIUpdateScheduler;

public class PreviewWikibaseSchemaCommand extends Command {

    /**
     * This command uses POST but is left CSRF-unprotected since it does not incur a side effect or state change in the
     * backend. The reason why it uses POST is to make sure large schemas and engines can be passed as parameters.
     */

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        Project project = getProject(request);

        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");

        String schemaJson = request.getParameter("schema");
        String manifestJson = request.getParameter("manifest");
        boolean slowMode = "true".equals(request.getParameter("slow_mode"));

        WikibaseSchema schema = null;
        if (schemaJson != null) {
            try {
                schema = WikibaseSchema.reconstruct(schemaJson);
            } catch (IOException e) {
                Command.respondError(response, "Wikibase schema could not be parsed. Error message: " + e.getMessage());
                return;
            }
        } else {
            schema = (WikibaseSchema) project.getCurrentGrid().getOverlayModels().get("wikibaseSchema");
        }
        if (schema == null) {
            Command.respondError(response, "No Wikibase schema provided.");
            return;
        }

        ValidationState validation = new ValidationState(project.getCurrentGrid().getColumnModel());
        schema.validate(validation);
        List<ValidationError> errors = validation.getValidationErrors();
        if (!errors.isEmpty()) {
            Map<String, Object> json = new HashMap<>();
            json.put("code", "error");
            json.put("reason", "invalid-schema");
            json.put("message", "Invalid Wikibase schema");
            json.put("errors", errors);
            // HTTP code 200 because it is normal for the user to submit incomplete schemas (for instance as they build
            // them).
            Command.respondJSON(response, 200, json);
            return;
        }

        Manifest manifest = null;
        if (manifestJson != null) {
            try {
                manifest = ManifestParser.parse(manifestJson);
            } catch (ManifestException e) {
                Command.respondError(response, "Wikibase manifest could not be parsed. Error message: " + e.getMessage());
                return;
            }
        }
        if (manifest == null) {
            Command.respondError(response, "No Wikibase manifest provided.");
            return;
        }

        QAWarningStore warningStore = new QAWarningStore();

        // Evaluate project
        Engine engine = getEngine(request, project);
        List<EntityEdit> editBatch = schema.evaluate(project.getCurrentGrid(), engine, warningStore);
        // Inspect the edits and generate warnings
        EditInspector inspector = new EditInspector(warningStore, manifest, slowMode);
        inspector.inspect(editBatch, schema);

        // Dump the first 10 edits, scheduled with the default scheduler
        WikibaseAPIUpdateScheduler scheduler = new WikibaseAPIUpdateScheduler();
        List<EntityEdit> nonNullEdits = scheduler.schedule(editBatch).stream()
                .filter(e -> !e.isNull())
                .collect(Collectors.toList());
        List<EntityEdit> firstEdits = nonNullEdits.stream()
                .limit(10)
                .collect(Collectors.toList());

        PreviewResults previewResults = new PreviewResults(
                warningStore.getWarnings(),
                warningStore.getMaxSeverity(),
                warningStore.getNbWarnings(),
                nonNullEdits.size(), firstEdits);
        respondJSON(response, 200, previewResults);
    }
}
