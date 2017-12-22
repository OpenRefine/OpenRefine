/*

Copyright 2010, Google Inc.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.model.Project;
import com.google.refine.model.medadata.IMetadata;
import com.google.refine.model.medadata.ProjectMetadata;

public class ProjectMetadataUtilities {
    final static Logger logger = LoggerFactory.getLogger("project_metadata_utilities");
    
    public static void save(IMetadata projectMeta, File projectDir) throws JSONException, IOException  {
        File tempFile = new File(projectDir, ProjectMetadata.TEMP_FILE_NAME);
        saveToFile(projectMeta, tempFile);

        File file = new File(projectDir, ProjectMetadata.DEFAULT_FILE_NAME);
        File oldFile = new File(projectDir, ProjectMetadata.OLD_FILE_NAME);

        if (oldFile.exists()) {
            oldFile.delete();
        }
        
        if (file.exists()) {
            file.renameTo(oldFile);
        }

        tempFile.renameTo(file);
    }
    
    public static void saveTableSchema(Project project, File projectDir) throws JSONException, IOException  {

    }
    
    protected static void saveToFile(IMetadata projectMeta, File metadataFile) throws JSONException, IOException   {
        Writer writer = new OutputStreamWriter(new FileOutputStream(metadataFile));
        try {
            JSONWriter jsonWriter = new JSONWriter(writer);
            projectMeta.write(jsonWriter, false);
        } finally {
            writer.close();
        }
    }

    static public ProjectMetadata load(File projectDir) {
        try {
            return loadFromFile(new File(projectDir, ProjectMetadata.DEFAULT_FILE_NAME));
        } catch (Exception e) {
        }

        try {
            return loadFromFile(new File(projectDir, ProjectMetadata.TEMP_FILE_NAME));
        } catch (Exception e) {
        }

        try {
            return loadFromFile(new File(projectDir, ProjectMetadata.OLD_FILE_NAME));
        } catch (Exception e) {
        }

        return null;
    }
    
    /**
     * Reconstruct the project metadata on a best efforts basis.  The name is
     * gone, so build something descriptive from the column names.  Recover the
     * creation and modification times based on whatever files are available.
     * 
     * @param projectDir the project directory
     * @param id the proejct id
     * @return
     */
    static public ProjectMetadata recover(File projectDir, long id) {
        ProjectMetadata pm = null;
        Project p = ProjectUtilities.load(projectDir, id);
        if (p != null) {
            List<String> columnNames = p.columnModel.getColumnNames();
            String tempName = "<recovered project> - " + columnNames.size() 
                    + " cols X " + p.rows.size() + " rows - "
                    + StringUtils.join(columnNames,'|');
            p.dispose();
            long ctime = System.currentTimeMillis();
            long mtime = 0;

            File dataFile = new File(projectDir, "data.zip");
            ctime = mtime = dataFile.lastModified();

            File historyDir = new File(projectDir,"history");
            File[] files = historyDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    long time = f.lastModified();
                    ctime = Math.min(ctime, time);
                    mtime = Math.max(mtime, time);
                }
            }
            pm = new ProjectMetadata(LocalDateTime.ofInstant(Instant.ofEpochMilli(ctime), ZoneId.systemDefault()),
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(mtime), ZoneId.systemDefault()),
                    tempName);
            logger.error("Partially recovered missing metadata project in directory " + projectDir + " - " + tempName);
        }
        return pm;
    }

    static protected ProjectMetadata loadFromFile(File metadataFile) throws Exception {
        ProjectMetadata projectMetaData =  new ProjectMetadata();
        projectMetaData.loadFromFile(metadataFile);
        return projectMetaData;
    }
}
