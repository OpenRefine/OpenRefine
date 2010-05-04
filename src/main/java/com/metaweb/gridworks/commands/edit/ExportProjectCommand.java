package com.metaweb.gridworks.commands.edit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;

import com.metaweb.gridworks.ProjectManager;
import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.model.Project;

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
                gzipTarToOutputStream(
                    ProjectManager.singleton.getProjectDir(project.id),
                    os
                );
            } finally {
                os.close();
            }
        } catch (Exception e) {
            respondException(response, e);
        }
    }
    
    protected void gzipTarToOutputStream(File dir, OutputStream os) throws IOException {
        GZIPOutputStream gos = new GZIPOutputStream(os);
        try {
            tarToOutputStream(dir, gos);
        } finally {
            gos.close();
        }
    }
    
    protected void tarToOutputStream(File dir, OutputStream os) throws IOException {
        TarOutputStream tos = new TarOutputStream(os);
        try {
            tarDir("", dir, tos);
        } finally {
            tos.close();
        }
    }
    
    protected void tarDir(String relative, File dir, TarOutputStream tos) throws IOException {
        File[] files = dir.listFiles();
        for (File file : files) {
            if (!file.isHidden()) {
                String path = relative + file.getName();
                
                if (file.isDirectory()) {
                    tarDir(path + File.separator, file, tos);
                } else {
                    TarEntry entry = new TarEntry(path);
                    
                    entry.setMode(TarEntry.DEFAULT_FILE_MODE);
                    entry.setSize(file.length());
                    entry.setModTime(file.lastModified());
                    
                    tos.putNextEntry(entry);
                    
                    copyFile(file, tos);
                    
                    tos.closeEntry();
                }
            }
        }
    }
    
    protected void copyFile(File file, OutputStream os) throws IOException {
        final int buffersize = 4096;
        
        FileInputStream fis = new FileInputStream(file);
        try {
            byte[] buf = new byte[buffersize];
            int count;
            
            while((count = fis.read(buf, 0, buffersize)) != -1) {
                os.write(buf, 0, count);      
            }  
        } finally {
            fis.close();
        }
    }
}
