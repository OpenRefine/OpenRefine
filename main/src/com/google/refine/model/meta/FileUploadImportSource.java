package com.google.refine.model.meta;

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.commands.importing.ImportJob;

public class FileUploadImportSource extends ImportSource {
    public String originalFileName;

    @Override
    protected void customWrite(JSONWriter writer, Properties options)
            throws JSONException {
        writer.key("originalFileName"); writer.value(originalFileName);
    }

    @Override
    protected void customReconstruct(JSONObject obj) throws JSONException {
        if (obj.has("originalFileName")) {
            originalFileName = obj.getString("originalFileName");
        }
    }
    
    @Override
    public void retrieveContent(HttpServletRequest request, Properties options, ImportJob job) throws Exception {
        ServletFileUpload upload = new ServletFileUpload();
        FileItemIterator iter = upload.getItemIterator(request);
        while (iter.hasNext()) {
            FileItemStream item = iter.next();
            if (!item.isFormField()) {
                String fileName = item.getName();
                if (fileName.length() > 0) {
                    InputStream stream = item.openStream();
                    try {
                        File file = new File(job.dir, "data");
                        
                        this.accessTime = new Date();
                        this.contentType = item.getContentType();
                        this.encoding = request.getCharacterEncoding();
                        this.originalFileName = fileName;
                        this.size = saveStreamToFileOrDir(
                            item.openStream(), file, this.contentType, fileName, job, request.getContentLength());
                        this.isArchive = file.isDirectory();
                    } finally {
                        stream.close();
                    }
                }
            }
        }
    }
}