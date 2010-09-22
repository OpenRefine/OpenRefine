package com.google.refine.commands.freebase;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.refine.commands.Command;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.SaveProtographOperation;
import com.google.refine.process.Process;
import com.google.refine.protograph.Protograph;
import com.google.refine.util.ParsingUtilities;

public class SaveProtographCommand extends Command {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Project project = getProject(request);
            
            String jsonString = request.getParameter("protograph");
            JSONObject json = ParsingUtilities.evaluateJsonStringToObject(jsonString);
            Protograph protograph = Protograph.reconstruct(json);
            
            AbstractOperation op = new SaveProtographOperation(protograph);
            Process process = op.createProcess(project, new Properties());
            
            performProcessAndRespond(request, response, project, process);
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
