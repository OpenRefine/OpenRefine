/*

Copyright 2010, 2022 Google Inc. & OpenRefine contributors
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

package com.google.refine.io;

import static com.google.refine.io.FileHistoryEntryManager.HISTORY_DIR;
import static com.google.refine.io.ProjectUtilities.DATA_ZIP;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.ProjectMetadata;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

public class ProjectMetadataUtilities {

    final static Logger logger = LoggerFactory.getLogger("project_metadata_utilities");

    public static void save(ProjectMetadata projectMeta, File projectDir) throws IOException {
        File tempFile = new File(projectDir, ProjectMetadata.TEMP_FILE_NAME);
        saveToFile(projectMeta, tempFile);
        if (tempFile.length() == 0) {
            throw new IOException("Failed to save project metadata - keeping backups");
        }

        // TODO Do we want to make sure we can successfully deserialize the file too?

        File file = new File(projectDir, ProjectMetadata.DEFAULT_FILE_NAME);
        File oldFile = new File(projectDir, ProjectMetadata.OLD_FILE_NAME);

        if (file.exists()) {
            if (file.length() > 0) {
                if (oldFile.exists()) {
                    oldFile.delete();
                }
                file.renameTo(oldFile);
            } else {
                file.delete();
            }
        }
        projectMeta.setLastSave();
        tempFile.renameTo(file);
    }

    protected static void saveToFile(ProjectMetadata projectMeta, File metadataFile) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(metadataFile), StandardCharsets.UTF_8)) {
            ParsingUtilities.saveWriter.writeValue(writer, projectMeta);
        }
    }

    static public ProjectMetadata load(File projectDir) {
        if (!projectDir.exists()) {
            logger.error("Project directory doesn't exist - {}", projectDir);
            return null;
        }
        if (!projectDir.isDirectory()) {
            logger.error("File isn't a project directory {}", projectDir);
            return null;
        }
        for (String filename : new String[] { ProjectMetadata.DEFAULT_FILE_NAME, ProjectMetadata.TEMP_FILE_NAME,
                ProjectMetadata.OLD_FILE_NAME }) {
            File file = new File(projectDir, filename);
            try {
                return loadFromFile(file);
            } catch (IOException e) {
                logger.warn("load metadata failed: {}", file.getAbsolutePath(), e);
            }
        }
        logger.error("Failed to load any metadata for project {}", projectDir.getAbsolutePath());
        return null;
    }

    /**
     * Reconstruct the project metadata on a best efforts basis. The name is gone, so build something descriptive from
     * the column names. Recover the creation and modification times based on whatever files are available.
     * 
     * @param projectDir
     *            the project directory
     * @param id
     *            the project id
     */
    static public ProjectMetadata recover(File projectDir, long id) {
        ProjectMetadata pm = null;
        Project p = ProjectUtilities.load(projectDir, id);
        if (p != null) {
            List<String> columnNames = p.columnModel.getColumnNames();
            String tempName = "<recovered project> - " + columnNames.size()
                    + " cols X " + p.rows.size() + " rows - "
                    + StringUtils.join(columnNames, '|');
            p.dispose();
            long ctime;
            long mtime;

            File dataFile = new File(projectDir, DATA_ZIP);
            ctime = mtime = dataFile.lastModified();

            File historyDir = new File(projectDir, HISTORY_DIR);
            File[] files = historyDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    long time = f.lastModified();
                    ctime = Math.min(ctime, time);
                    mtime = Math.max(mtime, time);
                }
            }
            pm = new ProjectMetadata(Instant.ofEpochMilli(ctime),
                    Instant.ofEpochMilli(mtime),
                    tempName);
            logger.error("Partially recovered missing metadata project in directory {} / {} ", projectDir, tempName);
        }
        return pm;
    }

    static protected ProjectMetadata loadFromFile(File metadataFile) throws IOException {
        Reader reader = new InputStreamReader(new FileInputStream(metadataFile), StandardCharsets.UTF_8);
        ProjectMetadata metadata = ParsingUtilities.mapper.readValue(reader, ProjectMetadata.class);
        metadata.setLastSave(); // No need to write it until it has been modified
        return metadata;
    }
}
