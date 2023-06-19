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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;

import org.openrefine.ProjectManager;
import org.openrefine.ProjectMetadata;
import org.openrefine.importers.ImporterUtilities;
import org.openrefine.model.Project;
import org.openrefine.util.JSONUtilities;
import org.openrefine.util.ParsingUtilities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public static class ImportingJobConfig {

        @JsonProperty("retrievalRecord")
        public RetrievalRecord retrievalRecord = new RetrievalRecord();
        @JsonProperty("fileSelection")
        public List<Integer> fileSelection;
        @JsonProperty("state")
        public String state = "new";
        @JsonProperty("hasData")
        @JsonInclude(JsonInclude.Include.ALWAYS)
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
        public List<ImportingFileRecord> files = new ArrayList<>();
        @JsonProperty("archiveCount")
        public int archiveCount = 0;
        @JsonProperty("uploadCount")
        public int uploadCount = 0;
        @JsonProperty("downloadCount")
        public int downloadCount = 0;
        @JsonProperty("clipboardCount")
        public int clipboardCount = 0;
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
            config.errors = ImporterUtilities.convertErrorsToJsonArray(exceptions);
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
            Runtime runtime = Runtime.getRuntime();
            JSONUtilities.safePut(progress, "memory", (runtime.totalMemory() - runtime.freeMemory()) / 1048576);
            JSONUtilities.safePut(progress, "maxmemory", runtime.maxMemory() / 1048576);
        }
    }

    public void setFileSelection(List<Integer> fileSelectionArray) {
        synchronized (config) {
            config.fileSelection = fileSelectionArray;
        }
    }

    public List<Integer> getFileSelection() {
        return config.fileSelection;
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
        ProjectManager.singleton.save();

        project = newProject;
        metadata = new ProjectMetadata();
    }

    @JsonIgnore
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

    /**
     * Returns the most common format among the selected files.
     *
     * @return the most common format found, or null if no format could be guessed.
     */
    @JsonIgnore
    public String getCommonFormatForSelectedFiles() {
        RetrievalRecord retrievalRecord = this.getRetrievalRecord();

        List<ImportingFileRecord> fileRecords = retrievalRecord.files;
        List<ImportingFileRecord> selectedFiles = getFileSelection().stream()
                .filter(idx -> idx >= 0 && idx < fileRecords.size())
                .map(idx -> fileRecords.get(idx))
                .collect(Collectors.toList());

        return ImporterUtilities.mostCommonFormat(selectedFiles);
    }

    /**
     * Guesses a better format by inspecting the first file to import with format guessers.
     * 
     * @param bestFormat
     *            the best format guessed so far
     * @return any better format, or the current best format
     */
    public String guessBetterFormat(String bestFormat) {
        RetrievalRecord retrievalRecord = getRetrievalRecord();
        if (retrievalRecord == null) {
            return bestFormat;
        }
        List<ImportingFileRecord> fileRecords = retrievalRecord.files;
        if (fileRecords == null) {
            return bestFormat;
        }
        if (bestFormat != null && fileRecords != null && fileRecords.size() > 0) {
            ImportingFileRecord firstFileRecord = fileRecords.get(0);
            String encoding = firstFileRecord.getDerivedEncoding();
            String location = firstFileRecord.getLocation();

            if (location != null) {
                File file = new File(getRawDataDir(), location);

                while (true) {
                    String betterFormat = null;

                    List<FormatGuesser> guessers = FormatRegistry.getFormatToGuessers().get(bestFormat);
                    if (guessers != null) {
                        for (FormatGuesser guesser : guessers) {
                            betterFormat = guesser.guess(file, encoding, bestFormat);
                            if (betterFormat != null) {
                                break;
                            }
                        }
                    }

                    if (betterFormat != null && !betterFormat.equals(bestFormat)) {
                        bestFormat = betterFormat;
                    } else {
                        break;
                    }
                }
            }
        }
        return bestFormat;
    }

    /**
     * Updates the ranked formats stored in the configuration.
     * 
     * @param bestFormat
     *            the format that should be put first in the list
     */
    void rerankFormats(String bestFormat) {
        List<String> rankedFormats = new ArrayList<>();
        final String bestFormat1 = bestFormat;
        final Map<String, String[]> formatToSegments = new HashMap<String, String[]>();

        boolean download = true;
        if (bestFormat1 != null && FormatRegistry.getFormatToRecord().get(bestFormat1) != null) {
            download = FormatRegistry.getFormatToRecord().get(bestFormat1).download;
        }

        List<String> formats = new ArrayList<String>(FormatRegistry.getFormatToRecord().keySet().size());
        for (String format : FormatRegistry.getFormatToRecord().keySet()) {
            ImportingFormat record = FormatRegistry.getFormatToRecord().get(format);
            if (record.uiClass != null && record.parser != null && record.download == download) {
                formats.add(format);
                formatToSegments.put(format, format.split("/"));
            }
        }

        if (bestFormat1 == null) {
            Collections.sort(formats);
        } else {
            Collections.sort(formats, new Comparator<String>() {

                @Override
                public int compare(String format1, String format2) {
                    if (format1.equals(bestFormat1)) {
                        return -1;
                    } else if (format2.equals(bestFormat1)) {
                        return 1;
                    } else {
                        return compareBySegments(format1, format2);
                    }
                }

                int compareBySegments(String format1, String format2) {
                    int c = commonSegments(format2) - commonSegments(format1);
                    return c != 0 ? c : format1.compareTo(format2);
                }

                int commonSegments(String format) {
                    String[] bestSegments = formatToSegments.get(bestFormat1);
                    String[] segments = formatToSegments.get(format);
                    if (bestSegments == null || segments == null) {
                        return 0;
                    } else {
                        int i;
                        for (i = 0; i < bestSegments.length && i < segments.length; i++) {
                            if (!bestSegments[i].equals(segments[i])) {
                                break;
                            }
                        }
                        return i;
                    }
                }
            });
        }

        for (String format : formats) {
            rankedFormats.add(format);
        }
        setRankedFormats(rankedFormats);
    }

    /**
     * Figure out the best (most common) format for the set of files, select all files which match that format, and
     * return the format found.
     * 
     * @return best (highest frequency) format
     */
    @JsonIgnore
    public String autoSelectFiles() {
        RetrievalRecord retrievalRecord = getRetrievalRecord();
        List<Integer> fileSelection = config.fileSelection;
        List<ImportingFileRecord> fileRecords = retrievalRecord.files;
        int count = fileRecords.size();

        // Default to text to to avoid parsing as binary/excel.
        // "text" is more general than "text/line-based", so better as a fallback
        String bestFormat = ImporterUtilities.mostCommonFormat(retrievalRecord.files);
        if (bestFormat == null) {
            bestFormat = "text";
        }

        if (retrievalRecord.archiveCount == 0) {
            // If there's no archive, then select everything
            for (int i = 0; i < count; i++) {
                fileSelection.add(i);
            }
        } else {
            // Otherwise, select files matching the best format
            for (int i = 0; i < count; i++) {
                ImportingFileRecord fileRecord = fileRecords.get(i);
                String format = fileRecord.getFormat();
                if (format != null && format.equals(bestFormat)) {
                    fileSelection.add(i);
                }
            }

            // If nothing matches the best format but we have some files,
            // then select them all
            if (fileSelection.size() == 0 && count > 0) {
                for (int i = 0; i < count; i++) {
                    fileSelection.add(i);
                }
            }
        }
        return bestFormat;
    }

    public void updateWithNewFileSelection(List<Integer> fileSelectionArray) {
        setFileSelection(fileSelectionArray);

        String bestFormat = getCommonFormatForSelectedFiles();
        bestFormat = guessBetterFormat(bestFormat);

        rerankFormats(bestFormat);
    }
}
