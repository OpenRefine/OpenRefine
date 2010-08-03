package com.google.gridworks.rdf.commands;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.gridworks.commands.Command;
import com.google.gridworks.model.AbstractOperation;
import com.google.gridworks.model.Project;
import com.google.gridworks.process.Process;
import com.google.gridworks.rdf.RdfSchema;
import com.google.gridworks.rdf.operations.SaveRdfSchemaOperation;
import com.google.gridworks.util.ParsingUtilities;

public class SaveRdfSchemaCommand extends Command{

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Project project = getProject(request);
            
            String jsonString = request.getParameter("schema");
            JSONObject json = ParsingUtilities.evaluateJsonStringToObject(jsonString);
            RdfSchema schema = RdfSchema.reconstruct(json);
            
            AbstractOperation op = new SaveRdfSchemaOperation(schema);
            Process process = op.createProcess(project, new Properties());
            
            performProcessAndRespond(request, response, project, process);
            
            /*project.schema = schema;
            
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            Writer w = response.getWriter();
            JSONWriter writer = new JSONWriter(w);
            writer.object();
            writer.key("code"); writer.value("ok");
            writer.key("historyEntry"); 
            
            //dummy history for now
            writer.object();
            writer.key("op"); writer.value("saveRdfSchema");
            writer.key("description"); writer.value("Save RDF schema");
            writer.endObject();
            
            writer.endObject();
            
            w.flush();
            w.close();*/
            
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
