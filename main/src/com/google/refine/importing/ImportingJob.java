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
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;
import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;


public class ImportingJob implements Jsonizable {
    final public long id;
    final public File dir; // Temporary directory where the data about this job is stored
    
    private JSONObject config;
    
    public Project project;
    public ProjectMetadata metadata;
    
    public long lastTouched;
    public boolean updating;
    public boolean canceled;
    
    final private Object lock = new Object();
    
    public ImportingJob(long id, File dir) {
        this.id = id;
        this.dir = dir;

        JSONObject cfg = new JSONObject();
        JSONUtilities.safePut(cfg, "state", "new");
        JSONUtilities.safePut(cfg, "hasData", false);
        this.config = cfg;
        
        lastTouched = System.currentTimeMillis();
        
        dir.mkdirs();
    }
    
    
    public JSONObject getOrCreateDefaultConfig() {
        return config;
    }
    
    public void setState(String state) {
        synchronized(config) {
            JSONUtilities.safePut(config, "state", state);        
        }
    }

    public void setError(List<Exception> exceptions) {
        synchronized(config) {
            JSONUtilities.safePut(config, "errors", 
                    DefaultImportingController.convertErrorsToJsonArray(exceptions));
            setState("error");
        }
    }
    
    public void setProjectID(long projectID) {
        synchronized (config) {
            JSONUtilities.safePut(config, "projectID", projectID);
        }
    }

    public void setProgress(int percent, String message) {
        synchronized (config) {
            JSONObject progress = JSONUtilities.getObject(config, "progress");
            if (progress == null) {
                progress = new JSONObject();
                JSONUtilities.safePut(config, "progress", progress);
            }
            JSONUtilities.safePut(progress, "message", message);
            JSONUtilities.safePut(progress, "percent", percent);
            JSONUtilities.safePut(progress, "memory", Runtime.getRuntime().totalMemory() / 1000000);
            JSONUtilities.safePut(progress, "maxmemory", Runtime.getRuntime().maxMemory() / 1000000);
        }
    }

    public void setFileSelection(JSONArray fileSelectionArray) {
        synchronized (config) {
            JSONUtilities.safePut(config, "fileSelection", fileSelectionArray);
        }
    }
    
    public void setRankedFormats(JSONArray rankedFormats) {
        synchronized (config) {
            JSONUtilities.safePut(config, "rankedFormats", rankedFormats);
        }
    }


    public JSONObject getRetrievalRecord() {
        synchronized(config) {
            return JSONUtilities.getObject(config,"retrievalRecord");
        }
    }
    
    
    public List<JSONObject> getSelectedFileRecords() {
        List<JSONObject> results = new ArrayList<JSONObject>();
        
        JSONObject retrievalRecord = JSONUtilities.getObject(config,"retrievalRecord");
        if (retrievalRecord != null) {
            JSONArray fileRecordArray = JSONUtilities.getArray(retrievalRecord, "files");
            if (fileRecordArray != null) {
                JSONArray fileSelectionArray = JSONUtilities.getArray(config,"fileSelection");
                if (fileSelectionArray != null) {
                    for (int i = 0; i < fileSelectionArray.length(); i++) {
                        int index = JSONUtilities.getIntElement(fileSelectionArray, i, -1);
                        if (index >= 0 && index < fileRecordArray.length()) {
                            results.add(JSONUtilities.getObjectElement(fileRecordArray, index));
                        }
                    }
                }
            }
        }
        return results;
    }

    
    public void touch() {
        lastTouched = System.currentTimeMillis();
    }
    
    public void prepareNewProject() {
        if (project != null) {
            project.dispose();
        }
        
        // Make sure all projects have been saved in case we run out of memory 
        // or have some other catastrophe on import
        ProjectManager.singleton.save(true);
        
        project = new Project();
        metadata = new ProjectMetadata();
    }
    
    public void dispose() {
        if (project != null) {
            project.dispose();
            project = null;
        }
        metadata = null;
        
        try {
            FileUtils.deleteDirectory(dir);
        } catch (IOException e) {
        }
    }
    
    public File getRawDataDir() {
        File dir2 = new File(dir, "raw-data");
        dir2.mkdirs();
        return dir2;
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        synchronized(lock) {
            writer.object();
            writer.key("config"); writer.value(config);
            writer.endObject();
        }
    }
    
}
