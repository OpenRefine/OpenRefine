package com.google.refine.model.meta;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;
import com.google.refine.commands.importing.ImportJob;
import com.google.refine.commands.importing.ImportManager;
import com.google.refine.util.ParsingUtilities;

abstract public class ImportSource implements Jsonizable {
    public Date accessTime;
    public long size;
    public boolean isArchive = false;
    
    public String contentType;
    public String encoding;
    
    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        writer.key("type"); writer.value(ImportManager.getImportSourceClassName(this.getClass()));
        writer.key("accessTime"); writer.value(ParsingUtilities.dateToString(accessTime));
        writer.key("size"); writer.value(size);
        writer.key("isArchive"); writer.value(isArchive);
        writer.key("contentType"); writer.value(contentType);
        writer.key("encoding"); writer.value(encoding);
        writer.endObject();
    }
    
    public void reconstruct(JSONObject obj) throws JSONException {
        if (obj.has("accessTime")) {
            accessTime = ParsingUtilities.stringToDate(obj.getString("accessTime"));
        }
        if (obj.has("size")) {
            size = obj.getLong("size");
        }
        if (obj.has("isArchive")) {
            isArchive = obj.getBoolean("isArchive");
        }
        if (obj.has("contentType")) {
            contentType = obj.getString("contentType");
        }
        if (obj.has("encoding")) {
            encoding = obj.getString("encoding");
        }
        customReconstruct(obj);
    }
    
    abstract public void retrieveContent(HttpServletRequest request, Properties options, ImportJob job)
        throws Exception;
    
    abstract protected void customWrite(JSONWriter writer, Properties options) throws JSONException;
    abstract protected void customReconstruct(JSONObject obj) throws JSONException;
    
    static protected long saveStreamToFileOrDir(
        InputStream is,
        File file,
        String contentType,
        String fileNameOrUrl,
        ImportJob job,
        long expectedSize
    ) throws IOException {
        InputStream archiveIS = null;
        if (fileNameOrUrl != null) {
            try {
                if (fileNameOrUrl.endsWith(".tar.gz") ||
                    fileNameOrUrl.endsWith(".tar.gz.gz") ||
                    fileNameOrUrl.endsWith(".tgz")) {
                    archiveIS = new TarInputStream(new GZIPInputStream(is));
                } else if (fileNameOrUrl.endsWith(".tar.bz2")) {
                    archiveIS = new TarInputStream(new CBZip2InputStream(is));
                } else if (fileNameOrUrl.endsWith(".tar")) {
                    archiveIS = new TarInputStream(is);
                } else if (fileNameOrUrl.endsWith(".zip")) {
                    archiveIS = new ZipInputStream(is);
                }
            } catch (IOException e) {
                archiveIS = null;
            }
        }
        
        job.bytesSaved = 0;
        if (archiveIS == null) {
            saveStreamToFile(is, file, job, true, expectedSize);
        } else {
            job.retrievingProgress = -1;
            
            // NOTE(SM): unfortunately, java.io does not provide any generalized class for
            // archive-like input streams so while both TarInputStream and ZipInputStream
            // behave precisely the same, there is no polymorphic behavior so we have
            // to treat each instance explicitly... one of those times you wish you had
            // closures
            
            if (archiveIS instanceof TarInputStream) {
                TarInputStream tis = (TarInputStream) archiveIS;
                TarEntry te;
                while ((te = tis.getNextEntry()) != null) {
                    if (!te.isDirectory()) {
                        saveStreamToFile(tis, new File(file, te.getName()), job, false, 0);
                    }
                }
            } else if (archiveIS instanceof ZipInputStream) {
                ZipInputStream zis = (ZipInputStream) archiveIS;
                ZipEntry ze;
                long compressedSize = 0;
                while ((ze = zis.getNextEntry()) != null) {
                    if (!ze.isDirectory()) {
                        saveStreamToFile(zis, new File(file, ze.getName()), job, false, 0);
                        
                        compressedSize += ze.getCompressedSize(); // this might be negative if not known
                        if (compressedSize > 0) {
                            job.retrievingProgress = (int) (compressedSize * 100 / expectedSize);
                        }
                    }
                }
            }
        }
        return job.bytesSaved;
    }
    
    static private void saveStreamToFile(
        InputStream is,
        File file,
        ImportJob job,
        boolean updateProgress,
        long expectedSize
    ) throws IOException {
        byte data[] = new byte[4096];
        
        file.getParentFile().mkdirs();
        
        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(fos, data.length);
        
        int count;
        while ((count = is.read(data, 0, data.length)) != -1) {
           bos.write(data, 0, count);
           
           job.bytesSaved += count;
           if (updateProgress) {
               job.retrievingProgress = (int) (job.bytesSaved * 100 / expectedSize);
           }
        }
        
        bos.flush();
        bos.close();
    }
}
