/*

Copyright 2011, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.importing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.RefineServlet;

import edu.mit.simile.butterfly.ButterflyModule;

public class ImportingManager {
    static public class Format {
        final public String id;
        final public String label;
        final public boolean download;
        final public String uiClass;
        final public ImportingParser parser;
        
        private Format(
            String id,
            String label,
            boolean download,
            String uiClass,
            ImportingParser parser
        ) {
            this.id = id;
            this.label = label;
            this.download = download;
            this.uiClass = uiClass;
            this.parser = parser;
        }
    }
    
    final static Logger logger = LoggerFactory.getLogger("importing");
    
    static private RefineServlet servlet;
    static private File importDir;
    
    final static private Map<Long, ImportingJob> jobs = Collections.synchronizedMap(new HashMap<Long, ImportingJob>());
    static private long jobIdCounter = 0;
    final static private Object jobIdLock = new Object();
    
    // Mapping from format to label, e.g., "text" to "Text files", "text/xml" to "XML files"
    final static public Map<String, Format> formatToRecord = new HashMap<String, Format>();
    
    // Mapping from format to guessers
    final static public Map<String, List<FormatGuesser>> formatToGuessers = new HashMap<String, List<FormatGuesser>>();
    
    // Mapping from file extension to format, e.g., ".xml" to "text/xml"
    final static public Map<String, String> extensionToFormat = new HashMap<String, String>();
    
    // Mapping from mime type to format, e.g., "application/json" to "text/json"
    final static public Map<String, String> mimeTypeToFormat = new HashMap<String, String>();
    
    // URL rewriters
    final static public Set<UrlRewriter> urlRewriters = new HashSet<UrlRewriter>();
    
    // Mapping from controller name to controller
    final static public Map<String, ImportingController> controllers = new HashMap<String, ImportingController>();
    
    // timer for periodically deleting stale importing jobs
    static private ScheduledExecutorService service;

    final static private long TIMER_PERIOD = 10; // 10 minutes
    final static private long STALE_PERIOD = 60 * 60 * 1000; // 60 minutes in milliseconds
    
    static private class CleaningTimerTask implements Runnable {
        @Override
        public void run() {
            // An exception here will keep future runs of this task from happening, 
            // but won't affect other timer tasks
            cleanUpStaleJobs();
        }
    }
    
    static public void initialize(RefineServlet servlet) {
        ImportingManager.servlet = servlet;
        
        service =  Executors.newSingleThreadScheduledExecutor();
        service.scheduleWithFixedDelay(new CleaningTimerTask(), TIMER_PERIOD, TIMER_PERIOD, TimeUnit.MINUTES);
    }
    
    static public void registerFormat(String format, String label) {
        registerFormat(format, label, null, null);
    }
    
    static public void registerFormat(String format, String label, String uiClass, ImportingParser parser) {
        formatToRecord.put(format, new Format(format, label, true, uiClass, parser));
    }
    
    static public void registerFormat(
            String format, String label, boolean download, String uiClass, ImportingParser parser) {
        formatToRecord.put(format, new Format(format, label, download, uiClass, parser));
    }
    
    static public void registerFormatGuesser(String format, FormatGuesser guesser) {
        List<FormatGuesser> guessers = formatToGuessers.get(format);
        if (guessers == null) {
            guessers = new LinkedList<FormatGuesser>();
            formatToGuessers.put(format, guessers);
        }
        guessers.add(0, guesser); // prepend so that newer guessers take priority
    }
    
    static public void registerExtension(String extension, String format) {
        extensionToFormat.put(extension.startsWith(".") ? extension : ("." + extension), format);
    }
    
    static public void registerMimeType(String mimeType, String format) {
        mimeTypeToFormat.put(mimeType, format);
    }
    
    static public void registerUrlRewriter(UrlRewriter urlRewriter) {
        urlRewriters.add(urlRewriter);
    }
    
    static public void registerController(ButterflyModule module, String name, ImportingController controller) {
        String key = module.getName() + "/" + name;
        controllers.put(key, controller);
        
        controller.init(servlet);
    }

    static synchronized public File getImportDir() {
        if (importDir == null) {
            File tempDir = servlet.getTempDir();
            importDir = tempDir == null ? new File(".import-temp") : new File(tempDir, "import");
            
            if (importDir.exists()) {
                try {
                    // start fresh
                    FileUtils.deleteDirectory(importDir);
                } catch (IOException e) {
                }
            }
            importDir.mkdirs();
        }
        return importDir;
    }
    
    static public ImportingJob createJob() {
        long id;
        
        synchronized(jobIdLock) {
            ++jobIdCounter;
            
            // Avoid negative job id's when the counter wraps around.
            if (jobIdCounter < 0) {
                jobIdCounter = 1;
            }
            
            id = jobIdCounter;
        }
        
        File jobDir = new File(getImportDir(), Long.toString(id));
        
        ImportingJob job = new ImportingJob(id, jobDir);
        jobs.put(id, job);
        
        return job;
    }
    
    static public ImportingJob getJob(long id) {
        return jobs.get(id);
    }
    
    static public void disposeJob(long id) {
        ImportingJob job = getJob(id);
        if (job != null) {
            job.dispose();
            jobs.remove(id);
        }
    }
    
    static public void writeConfiguration(JSONWriter writer, Properties options) throws JSONException {
        writer.object();
        
        writer.key("formats");
        writer.object();
        for (String format : formatToRecord.keySet()) {
            Format record = formatToRecord.get(format);
            
            writer.key(format);
            writer.object();
            writer.key("id"); writer.value(record.id);
            writer.key("label"); writer.value(record.label);
            writer.key("download"); writer.value(record.download);
            writer.key("uiClass"); writer.value(record.uiClass);
            writer.endObject();
        }
        writer.endObject();
        
        writer.key("mimeTypeToFormat");
        writer.object();
        for (String mimeType : mimeTypeToFormat.keySet()) {
            writer.key(mimeType);
            writer.value(mimeTypeToFormat.get(mimeType));
        }
        writer.endObject();
        
        writer.key("extensionToFormat");
        writer.object();
        for (String extension : extensionToFormat.keySet()) {
            writer.key(extension);
            writer.value(extensionToFormat.get(extension));
        }
        writer.endObject();
        
        writer.endObject();
    }
    
    static public String getFormatFromFileName(String fileName) {
        int start = 0;
        while (true) {
            int dot = fileName.indexOf('.', start);
            if (dot < 0) {
                break;
            }
            
            String extension = fileName.substring(dot);
            String format = extensionToFormat.get(extension);
            if (format != null) {
                return format;
            } else {
                start = dot + 1;
            }
        }
        return null;
    }
    
    static public String getFormatFromMimeType(String mimeType) {
        return mimeTypeToFormat.get(mimeType);
    }
    
    static public String getFormat(String fileName, String mimeType) {
        String fileNameFormat = getFormatFromFileName(fileName);
        if (mimeType != null) {
            mimeType = mimeType.split(";")[0];
        }
        String mimeTypeFormat = mimeType == null ? null : getFormatFromMimeType(mimeType);
        if (mimeTypeFormat == null) {
            return fileNameFormat;
        } else if (fileNameFormat == null) {
            return mimeTypeFormat;
        } else if (mimeTypeFormat.startsWith(fileNameFormat)) {
            // mime type-base format is more specific
            return mimeTypeFormat;
        } else {
            return fileNameFormat;
        }
    }
    
    static private void cleanUpStaleJobs() {
        long now = System.currentTimeMillis();
        Collection<Long> keys;
        synchronized(jobs) {
            keys = new ArrayList<Long>(jobs.keySet());
        }
        for (Long id : keys) {
            ImportingJob job = jobs.get(id);
            if (job != null && !job.updating && now - job.lastTouched > STALE_PERIOD) {
                job.dispose();
                jobs.remove(id);
                logger.info("Removed Stale Import Job ID " + id);
            }
        }
    }
}
