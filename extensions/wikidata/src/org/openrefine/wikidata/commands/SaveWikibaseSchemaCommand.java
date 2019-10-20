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
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrefine.wikidata.operations.SaveWikibaseSchemaOperation;
import org.openrefine.wikidata.schema.WikibaseSchema;

import com.google.refine.commands.Command;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;

public class SaveWikibaseSchemaCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    	if(!hasValidCSRFToken(request)) {
    		respondCSRFError(response);
    		return;
    	}

        try {
            Project project = getProject(request);

            String jsonString = request.getParameter("schema");
            if (jsonString == null) {
                respondError(response, "No Wikibase schema provided.");
                return;
            }

            WikibaseSchema schema = ParsingUtilities.mapper.readValue(jsonString, WikibaseSchema.class);

            AbstractOperation op = new SaveWikibaseSchemaOperation(schema);
            Process process = op.createProcess(project, new Properties());

            performProcessAndRespond(request, response, project, process);

        } catch (IOException e) {
            // We do not use respondException here because this is an expected
            // exception which happens every time a user tries to save an incomplete
            // schema - the exception should not be logged.
            respondError(response, "Wikibase schema could not be parsed.");
        } catch (Exception e) {
            // This is an unexpected exception, so we log it.
            respondException(response, e);
        }
    }
}
