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

package org.openrefine.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrefine.ProjectMetadata;
import org.openrefine.util.ParsingUtilities;

public class ProjectMetadataUtilities {

    final static Logger logger = LoggerFactory.getLogger("project_metadata_utilities");

    public static void save(ProjectMetadata projectMeta, File projectDir) throws IOException {
        File tempFile = new File(projectDir, "metadata.temp.json");
        saveToFile(projectMeta, tempFile);

        File file = new File(projectDir, "metadata.json");
        File oldFile = new File(projectDir, "metadata.old.json");

        if (oldFile.exists()) {
            oldFile.delete();
        }

        if (file.exists()) {
            file.renameTo(oldFile);
        }

        tempFile.renameTo(file);
    }

    protected static void saveToFile(ProjectMetadata projectMeta, File metadataFile) throws IOException {
        Writer writer = new OutputStreamWriter(new FileOutputStream(metadataFile));
        try {
            ParsingUtilities.defaultWriter.writeValue(writer, projectMeta);
        } finally {
            writer.close();
        }
    }

    static public ProjectMetadata load(File projectDir) {
        ProjectMetadata pm = null;

        pm = loadMetaDataIfExist(projectDir, ProjectMetadata.DEFAULT_FILE_NAME);

        if (pm == null) {
            pm = loadMetaDataIfExist(projectDir, ProjectMetadata.TEMP_FILE_NAME);
        }

        if (pm == null) {
            pm = loadMetaDataIfExist(projectDir, ProjectMetadata.OLD_FILE_NAME);
        }

        return pm;
    }

    private static ProjectMetadata loadMetaDataIfExist(File projectDir, String fileName) {
        ProjectMetadata pm = null;
        File file = new File(projectDir, fileName);
        if (file.exists()) {
            try {
                pm = loadFromFile(file);
            } catch (Exception e) {
                logger.warn("load metadata failed: " + file.getAbsolutePath());
                logger.error(ExceptionUtils.getStackTrace(e));
            }
        }

        return pm;
    }

    static protected ProjectMetadata loadFromFile(File metadataFile) throws Exception {
        FileReader reader = new FileReader(metadataFile);
        return ParsingUtilities.mapper.readValue(reader, ProjectMetadata.class);
    }
}
