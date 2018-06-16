package com.google.refine.commands.project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.tools.tar.TarOutputStream;

import com.google.refine.ProjectManager;
import com.google.refine.browsing.Engine;
import com.google.refine.commands.Command;
import com.google.refine.exporters.CsvExporter;
import com.google.refine.model.Project;
import com.google.refine.model.metadata.DataPackageMetadata;
import com.google.refine.model.metadata.PackageExtension;

public class PackageProjectCommand extends Command {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ProjectManager.singleton.setBusy(true);
        
        try {
            // get the metadata
            String metadata = request.getParameter("metadata");
            InputStream in = IOUtils.toInputStream(metadata, "UTF-8");
            
            Project project = getProject(request);
            Engine engine = getEngine(request, project);
            
            // ensure project get saved
            DataPackageMetadata dpm = new DataPackageMetadata();
            dpm.loadFromStream(in);
            ProjectManager.singleton.ensureProjectSaved(project.id);
            
            // export project
            CsvExporter exporter = new CsvExporter();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Writer outputStreamWriter = new OutputStreamWriter(baos);
            exporter.export(project, null, engine, outputStreamWriter);
            
            OutputStream os = response.getOutputStream();
            try {
                PackageExtension.saveZip(dpm.getPackage(), baos, os);
                response.setHeader("Content-Type", "application/x-gzip");
            } finally {
                outputStreamWriter.close();
                os.close();
            }
        } catch (Exception e) {
            respondException(response, e);
        } finally {
            ProjectManager.singleton.setBusy(false);
        }
    }

    protected void gzipTarToOutputStream(Project project, OutputStream os) throws IOException {
        GZIPOutputStream gos = new GZIPOutputStream(os);
        try {
            tarToOutputStream(project, gos);
        } finally {
            gos.close();
        }
    }

    protected void tarToOutputStream(Project project, OutputStream os) throws IOException {
        TarOutputStream tos = new TarOutputStream(os);
        try {
            ProjectManager.singleton.exportProject(project.id, tos);
        } finally {
            tos.close();
        }
    }
}
