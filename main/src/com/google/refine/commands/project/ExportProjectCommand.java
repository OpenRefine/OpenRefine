package com.google.refine.commands.project;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tools.tar.TarOutputStream;

import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.model.Project;

public class ExportProjectCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            Project project = getProject(request);
            ProjectManager.singleton.ensureProjectSaved(project.id);

            response.setHeader("Content-Type", "application/x-gzip");

            OutputStream os = response.getOutputStream();
            try {
                gzipTarToOutputStream(project, os);
            } finally {
                os.close();
            }
        } catch (Exception e) {
            respondException(response, e);
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
