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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.ProjectManager;
import com.google.refine.model.Project;
import com.google.refine.util.Pool;

public class ProjectUtilities {

    final static Logger logger = LoggerFactory.getLogger("project_utilities");
    public static final String DATA_ZIP = "data.zip";
    public static final String DATA_TEMP_ZIP = "data.temp.zip";
    public static final String DATA_OLD_ZIP = "data.old.zip";

    synchronized public static void save(Project project) throws IOException {
        synchronized (project) {
            long id = project.id;
            File dir = ((FileProjectManager) ProjectManager.singleton).getProjectDir(id);

            File tempFile = new File(dir, DATA_TEMP_ZIP);
            try {
                saveToFile(project, tempFile);
            } catch (IOException e) {
                logger.warn("Failed to save project {}", id, e);
                try {
                    tempFile.delete();
                } catch (Exception e2) {
                    // just ignore - file probably was never created.
                }
                throw e;
            }

            File file = new File(dir, DATA_ZIP);
            File oldFile = new File(dir, DATA_OLD_ZIP);

            if (file.exists()) {
                file.renameTo(oldFile);
            }

            tempFile.renameTo(file);
            if (oldFile.exists()) {
                oldFile.delete();
            }

            project.setLastSave();

            logger.info("Saved project '{}'", id);
        }
    }

    protected static void saveToFile(Project project, File file) throws IOException {
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
        try {
            Pool pool = new Pool();

            out.putNextEntry(new ZipEntry("data.txt"));
            try {
                project.saveToOutputStream(out, pool);
            } finally {
                out.closeEntry();
            }

            out.putNextEntry(new ZipEntry("pool.txt"));
            try {
                pool.save(out);
            } finally {
                out.closeEntry();
            }
        } finally {
            out.close();
        }
    }

    static public Project load(File dir, long id) {
        for (String filename : new String[] { DATA_ZIP, DATA_TEMP_ZIP, DATA_OLD_ZIP }) {
            try {
                File file = new File(dir, filename);
                if (file.exists()) {
                    return loadFromFile(file, id);
                }
            } catch (IOException e) {
                logger.warn("Failed to load from data file {} / {}", dir, filename, e);
            }
        }
        logger.error("All data files and backup data files failed to load");
        return null;
    }

    static protected Project loadFromFile(
            File file,
            long id) throws IOException {
        ZipFile zipFile = new ZipFile(file);
        try {
            Pool pool = new Pool();
            ZipEntry poolEntry = zipFile.getEntry("pool.txt");
            if (poolEntry != null) {
                pool.load(zipFile.getInputStream(poolEntry));
            } // else, it's a legacy project file

            return Project.loadFromInputStream(
                    zipFile.getInputStream(zipFile.getEntry("data.txt")),
                    id,
                    pool);
        } finally {
            zipFile.close();
        }
    }
}
