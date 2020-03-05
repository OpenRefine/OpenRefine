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

package org.openrefine.importing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;

import org.openrefine.ProjectManager;
import org.openrefine.ProjectMetadata;
import org.openrefine.model.Project;
import org.openrefine.util.JSONUtilities;
import org.openrefine.util.ParsingUtilities;

public class ImportingJob {

    final public long id;
    final public File dir; // Temporary directory where the data about this job is stored

    private ImportingJobConfig config;

    private Project project;
    public ProjectMetadata metadata;

    public long lastTouched;
    public boolean updating;
    public boolean canceled;

    final private Object lock = new Object();

    public static class ImportingJobConfig {

        @JsonProperty("retrievalRecord")
        public RetrievalRecord retrievalRecord;
        @JsonProperty("fileSelection")
        public List<Integer> fileSelection;
        @JsonProperty("state")
        public String state = "new";
        @JsonProperty("hasData")
        public boolean hasData = false;
        @JsonProperty("errors")
        public ArrayNode errors; // to be converted
        @JsonProperty("projectID")
        public long projectID;
        @JsonProperty("progress")
        public ObjectNode progress;
        @JsonProperty("rankedFormats")
        public List<String> rankedFormats;
        @JsonProperty("error")
        public String error; // use errors instead?
        @JsonProperty("errorDetails")
        public String errorDetails;
    }

    public static class RetrievalRecord {

        @JsonProperty("files")
        List<ImportingFileRecord> files = new ArrayList<>();
        @JsonProperty("archiveCount")
        public int archiveCount;
        @JsonProperty("uploadCount")
        public int uploadCount;
        @JsonProperty("downloadCount")
        public int downloadCount;
        @JsonProperty("clipboardCount")
        public int clipboardCount;
    }

    public ImportingJob(long id, File dir) {
        this.id = id;
        this.dir = dir;

        this.config = new ImportingJobConfig();

        lastTouched = System.currentTimeMillis();

        dir.mkdirs();
    }

    @JsonProperty("config")
    public ImportingJobConfig getJsonConfig() {
        return config;
    }

    public void setState(String state) {
        synchronized (config) {
            config.state = state;
        }
    }

    public void setError(List<Exception> exceptions) {
        synchronized (config) {
            config.errors = DefaultImportingController.convertErrorsToJsonArray(exceptions);
            config.state = "error";
        }
    }

    public void setProjectID(long projectID) {
        synchronized (config) {
            config.projectID = projectID;
        }
    }

    public void setProgress(int percent, String message) {
        synchronized (config) {
            ObjectNode progress = config.progress;
            if (progress == null) {
                progress = ParsingUtilities.mapper.createObjectNode();
                config.progress = progress;
            }
            JSONUtilities.safePut(progress, "message", message);
            JSONUtilities.safePut(progress, "percent", percent);
            JSONUtilities.safePut(progress, "memory", Runtime.getRuntime().totalMemory() / 1000000);
            JSONUtilities.safePut(progress, "maxmemory", Runtime.getRuntime().maxMemory() / 1000000);
        }
    }

    public void setFileSelection(List<Integer> fileSelectionArray) {
        synchronized (config) {
            config.fileSelection = fileSelectionArray;
        }
    }

    public void setRankedFormats(List<String> rankedFormats) {
        synchronized (config) {
            config.rankedFormats = rankedFormats;
        }
    }

    @JsonIgnore
    public RetrievalRecord getRetrievalRecord() {
        synchronized (config) {
            return config.retrievalRecord;
        }
    }

    @JsonIgnore
    public List<ImportingFileRecord> getSelectedFileRecords() {
        List<ImportingFileRecord> results = new ArrayList<>();

        if (config.retrievalRecord != null && config.retrievalRecord.files != null && config.fileSelection != null) {
            List<ImportingFileRecord> records = config.retrievalRecord.files;
            for (int i = 0; i < config.fileSelection.size(); i++) {
                int index = config.fileSelection.get(i);
                if (index >= 0 && index < records.size()) {
                    results.add(records.get(index));
                }
            }
        }
        return results;
    }

    public void touch() {
        lastTouched = System.currentTimeMillis();
    }

    public void setProject(Project newProject) {
        if (project != null) {
            project.dispose();
        }

        // Make sure all projects have been saved in case we run out of memory
        // or have some other catastrophe on import
        ProjectManager.singleton.save(true);

        project = newProject;
        metadata = new ProjectMetadata();
    }

    public Project getProject() {
        return project;
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
}
