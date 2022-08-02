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
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import com.google.refine.ProjectManager;
import com.google.refine.history.History;
import com.google.refine.history.HistoryEntry;
import com.google.refine.history.HistoryEntryManager;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.Pool;

public class FileHistoryEntryManager implements HistoryEntryManager {

    @Override
    public void delete(HistoryEntry historyEntry) {
        File file = getChangeFile(historyEntry);
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public void save(HistoryEntry historyEntry, Writer writer, Properties options) {
        try {
            if ("save".equals(options.getProperty("mode"))) {
                ParsingUtilities.saveWriter.writeValue(writer, historyEntry);
            } else {
                ParsingUtilities.defaultWriter.writeValue(writer, historyEntry);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadChange(HistoryEntry historyEntry) {
        File changeFile = getChangeFile(historyEntry);

        try {
            loadChange(historyEntry, changeFile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load change file " + changeFile.getAbsolutePath(), e);
        }
    }

    protected void loadChange(HistoryEntry historyEntry, File file) throws Exception {
        ZipFile zipFile = new ZipFile(file);
        try {
            Pool pool = new Pool();
            ZipEntry poolEntry = zipFile.getEntry("pool.txt");
            if (poolEntry != null) {
                pool.load(new InputStreamReader(
                        zipFile.getInputStream(poolEntry)));
            } // else, it's a legacy project file

            historyEntry.setChange(History.readOneChange(
                    zipFile.getInputStream(zipFile.getEntry("change.txt")), pool));
        } finally {
            zipFile.close();
        }
    }

    @Override
    public void saveChange(HistoryEntry historyEntry) throws Exception {
        File changeFile = getChangeFile(historyEntry);
        if (!(changeFile.exists())) {
            saveChange(historyEntry, changeFile);
        }
    }

    protected void saveChange(HistoryEntry historyEntry, File file) throws Exception {
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
        try {
            Pool pool = new Pool();

            out.putNextEntry(new ZipEntry("change.txt"));
            try {
                History.writeOneChange(out, historyEntry.getChange(), pool);
            } catch (Exception e) {
                e.printStackTrace();
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

    protected File getChangeFile(HistoryEntry historyEntry) {
        return new File(getHistoryDir(historyEntry), historyEntry.id + ".change.zip");
    }

    protected File getHistoryDir(HistoryEntry historyEntry) {
        File dir = new File(((FileProjectManager) ProjectManager.singleton)
                .getProjectDir(historyEntry.projectID),
                "history");
        dir.mkdirs();

        return dir;
    }
}
