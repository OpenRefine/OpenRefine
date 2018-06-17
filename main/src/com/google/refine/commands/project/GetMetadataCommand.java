package com.google.refine.commands.project;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.everit.json.schema.ValidationException;
import org.json.JSONException;

import com.google.refine.commands.Command;
import com.google.refine.model.Project;
import com.google.refine.model.metadata.IMetadata;
import com.google.refine.model.metadata.MetadataFactory;
import com.google.refine.model.metadata.MetadataFormat;

public class GetMetadataCommand extends Command {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Project project;
            MetadataFormat metadataFormat;
            try {
                project = getProject(request);
                metadataFormat = MetadataFormat.valueOf(request.getParameter("metadataFormat"));
            } catch (ServletException e) {
                respond(response, "error", e.getLocalizedMessage());
                return;
            }
            
            // for now, only the data package metadata is supported.
            if (metadataFormat != MetadataFormat.DATAPACKAGE_METADATA) {
                respond(response, "error", "metadata format is not supported");
                return;
            }
            
            IMetadata metadata = MetadataFactory.buildDataPackageMetadata(project);
            respondJSONObject(response, metadata.getJSON());
        } catch (JSONException e) {
            respondException(response, e);
        } catch (ValidationException e) {
            respondException(response, e);
        } 
    }
}
