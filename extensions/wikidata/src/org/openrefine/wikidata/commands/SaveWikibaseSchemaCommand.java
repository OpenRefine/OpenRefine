package org.openrefine.wikidata.commands;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrefine.wikidata.operations.SaveWikibaseSchemaOperation;
import org.openrefine.wikidata.schema.WikibaseSchema;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.refine.commands.Command;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;

public class SaveWikibaseSchemaCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Project project = getProject(request);
            
            String jsonString = request.getParameter("schema");
            if (jsonString == null) {
                respond(response, "error", "No Wikibase schema provided.");
                return;
            }
            
            JSONObject json = ParsingUtilities.evaluateJsonStringToObject(jsonString);
            WikibaseSchema schema = WikibaseSchema.reconstruct(json);
            
            AbstractOperation op = new SaveWikibaseSchemaOperation(schema);
            Process process = op.createProcess(project, new Properties());
            
            performProcessAndRespond(request, response, project, process);
          
        } catch (JSONException e) {
            respond(response, "error", "Wikibase schema could not be parsed.");
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
