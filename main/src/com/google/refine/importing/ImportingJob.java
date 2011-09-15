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
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;
import com.google.refine.ProjectMetadata;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;


public class ImportingJob implements Jsonizable {
    final public long id;
    final public File dir; // Temporary directory where the data about this job is stored
    
    public JSONObject config = null;
    
    public Project project;
    public ProjectMetadata metadata;
    
    public long lastTouched;
    public boolean updating;
    public boolean canceled;
    
    public ImportingJob(long id, File dir) {
        this.id = id;
        this.dir = dir;
        
        dir.mkdirs();
    }
    
    public JSONObject getOrCreateDefaultConfig() {
        if (config == null) {
            config = new JSONObject();
            JSONUtilities.safePut(config, "state", "new");
            JSONUtilities.safePut(config, "hasData", false);
        }
        return config;
    }
    
    public void touch() {
        lastTouched = System.currentTimeMillis();
    }
    
    public void prepareNewProject() {
        if (project != null) {
            project.dispose();
        }
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
        writer.object();
        writer.key("config"); writer.value(config);
        writer.endObject();
    }
}
