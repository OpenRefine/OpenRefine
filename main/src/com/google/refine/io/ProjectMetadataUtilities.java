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
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.ProjectMetadata;


public class ProjectMetadataUtilities {
    final static Logger logger = LoggerFactory.getLogger("project_metadata_utilities");

    public static void save(ProjectMetadata projectMeta, File projectDir) throws Exception {
        File tempFile = new File(projectDir, "metadata.temp.json");
        try {
            saveToFile(projectMeta, tempFile);
        } catch (Exception e) {
            e.printStackTrace();

            logger.warn("Failed to save project metadata");
            return;
        }

        File file = new File(projectDir, "metadata.json");
        File oldFile = new File(projectDir, "metadata.old.json");

        if (file.exists()) {
            file.renameTo(oldFile);
        }

        tempFile.renameTo(file);
        if (oldFile.exists()) {
            oldFile.delete();
        }
    }

    protected static void saveToFile(ProjectMetadata projectMeta, File metadataFile) throws Exception {
        Writer writer = new OutputStreamWriter(new FileOutputStream(metadataFile));
        try {
            JSONWriter jsonWriter = new JSONWriter(writer);
            projectMeta.write(jsonWriter);
        } finally {
            writer.close();
        }
    }

    static public ProjectMetadata load(File projectDir) {
        try {
            return loadFromFile(new File(projectDir, "metadata.json"));
        } catch (Exception e) {
        }

        try {
            return loadFromFile(new File(projectDir, "metadata.temp.json"));
        } catch (Exception e) {
        }

        try {
            return loadFromFile(new File(projectDir, "metadata.old.json"));
        } catch (Exception e) {
        }

        return null;
    }

    static protected ProjectMetadata loadFromFile(File metadataFile) throws Exception {
        FileReader reader = new FileReader(metadataFile);
        try {
            JSONTokener tokener = new JSONTokener(reader);
            JSONObject obj = (JSONObject) tokener.nextValue();

            return ProjectMetadata.loadFromJSON(obj);
        } finally {
            reader.close();
        }
    }
}
