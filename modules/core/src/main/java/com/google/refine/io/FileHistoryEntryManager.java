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
import java.io.LineNumberReader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.ProjectManager;
import com.google.refine.RefineServlet;
import com.google.refine.history.Change;
import com.google.refine.history.History;
import com.google.refine.history.HistoryEntry;
import com.google.refine.history.HistoryEntryManager;
import com.google.refine.model.changes.MassChange;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.Pool;

public class FileHistoryEntryManager implements HistoryEntryManager {

    public static final String HISTORY_DIR = "history";

    final static Logger logger = LoggerFactory.getLogger("FileHistoryEntryManager");

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
                pool.load(new InputStreamReader( // TODO: Missing encoding here
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
                HISTORY_DIR);
        dir.mkdirs();

        return dir;
    }

    /**
     * Scans a project's change files for classes (typically registered by extensions) which are not available on this
     * instance of OpenRefine. This is used to warn users importing a project which depends on extensions they don't
     * have installed, since undoing or redoing such a change would otherwise fail later on with a much less useful
     * error.
     *
     * @param projectDir
     *            the directory of the project to scan, as returned by {@link FileProjectManager#getProjectDir(long)}
     * @return the names of the classes referenced by the project's history which cannot be found
     */
    public static List<String> findMissingChangeClasses(File projectDir) {
        List<String> missingClasses = new ArrayList<>();
        File historyDir = new File(projectDir, HISTORY_DIR);
        File[] changeFiles = historyDir.listFiles((dir, name) -> name.endsWith(".change.zip"));
        if (changeFiles == null) {
            return missingClasses;
        }

        for (File changeFile : changeFiles) {
            for (String className : readChangeClassNames(changeFile)) {
                if (!isClassAvailable(className) && !missingClasses.contains(className)) {
                    missingClasses.add(className);
                }
            }
        }

        return missingClasses;
    }

    /**
     * Reads all the change class names recorded in a single change file. A plain change only ever records one class
     * name (line 2 of "change.txt"), but a {@link MassChange} (as used by, e.g., "Apply operations" or column
     * reordering) bundles several sub-changes together in the same file, each with its own recorded class name -- see
     * {@link MassChange#save} / {@link History#writeOneChange}. This walks that nested structure so an extension class
     * referenced from inside a {@link MassChange} is found too, not just top-level ones.
     */
    protected static List<String> readChangeClassNames(File changeFile) {
        List<String> classNames = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(changeFile)) {
            ZipEntry changeEntry = zipFile.getEntry("change.txt");
            if (changeEntry == null) {
                return classNames;
            }

            try (LineNumberReader reader = new LineNumberReader(
                    new InputStreamReader(zipFile.getInputStream(changeEntry), "UTF-8"))) {
                reader.readLine(); // version, unused here
                String className = reader.readLine();
                if (className != null) {
                    classNames.add(className);
                    // The pool is only needed to resolve pooled string values referenced from inside a MassChange's
                    // nested changes, so only load it (an extra zip-entry read plus a full parse) when we're
                    // actually about to recurse into one -- every other change file skips this cost entirely.
                    if (MassChange.class.getName().equals(className)) {
                        readMassChangeClassNames(reader, loadPool(zipFile), classNames);
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to read change file " + changeFile.getAbsolutePath()
                    + " while scanning for missing extension classes", e);
        }
        return classNames;
    }

    private static Pool loadPool(ZipFile zipFile) throws IOException {
        Pool pool = new Pool();
        ZipEntry poolEntry = zipFile.getEntry("pool.txt");
        if (poolEntry != null) {
            try (InputStreamReader poolReader = new InputStreamReader(zipFile.getInputStream(poolEntry), "UTF-8")) {
                pool.load(poolReader);
            }
        } // else, it's a legacy project file
        return pool;
    }

    /**
     * Parses the envelope {@link MassChange#save} writes ("updateRowContextDependencies=...", "changeCount=N", the N
     * nested changes themselves, then the "/ec/" marker) in order to recover the class names of the sub-changes it
     * bundles together, recursing into any of them which are themselves a {@link MassChange}.
     *
     * @return true if the whole envelope, including the trailing "/ec/" marker, was consumed, so a sibling change
     *         recorded immediately afterwards (if any) can be read next; false if scanning had to stop early because
     *         one of the nested classes could not be resolved (see {@link #readNestedChangeClassName})
     */
    private static boolean readMassChangeClassNames(LineNumberReader reader, Pool pool, List<String> classNames) throws IOException {
        String line;
        int changeCount = -1;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            if (equal < 0) {
                continue;
            }
            if ("changeCount".equals(line.substring(0, equal))) {
                try {
                    changeCount = Integer.parseInt(line.substring(equal + 1));
                } catch (NumberFormatException e) {
                    return false; // malformed envelope, nothing more we can safely read
                }
                break;
            }
        }
        if (changeCount < 0) {
            return false; // malformed envelope, nothing more we can safely read
        }

        for (int i = 0; i < changeCount; i++) {
            if (!readNestedChangeClassName(reader, pool, classNames)) {
                // We can't tell where this sub-change's own content ends without its class being available, so we
                // can't locate any further sibling sub-changes recorded after it in this same change file. Any
                // extension classes referenced beyond this point won't be flagged as missing until this one is
                // resolved (e.g. by installing the extension it belongs to).
                return false;
            }
        }
        reader.readLine(); // "/ec/" end marker
        return true;
    }

    /**
     * Reads one change record (a "VERSION\nclassName\n" header followed by the change's own serialized content) nested
     * inside a {@link MassChange}, adding its class name to {@code classNames}.
     *
     * @return true if the class was available and the reader was advanced past its content, so a sibling change
     *         recorded immediately afterwards (if any) can be read next; false if the class could not be resolved, in
     *         which case the reader position can no longer be trusted for anything recorded after it
     */
    private static boolean readNestedChangeClassName(LineNumberReader reader, Pool pool, List<String> classNames) throws IOException {
        reader.readLine(); // version, unused here
        String className = reader.readLine();
        if (className == null) {
            return false;
        }
        classNames.add(className);

        if (!isClassAvailable(className)) {
            return false;
        }
        if (MassChange.class.getName().equals(className)) {
            return readMassChangeClassNames(reader, pool, classNames);
        }
        return skipChangeContent(className, reader, pool);
    }

    /**
     * Advances the reader past a single change's serialized content by actually invoking its {@code load} method -- the
     * same reflective call {@link History#readOneChange} makes -- and discarding the resulting {@link Change}. Only
     * safe to call for classes already confirmed to be available.
     * <p>
     * Deliberately catches only {@link Exception}, not {@link Error}: an {@code OutOfMemoryError} thrown here means the
     * change's actual content is too large to load at all, which is exactly what would happen anyway when this history
     * entry is later undone/redone/loaded for real -- letting it propagate surfaces that resource problem plainly
     * instead of swallowing it during what is only a best-effort pre-import warning.
     */
    private static boolean skipChangeContent(String className, LineNumberReader reader, Pool pool) {
        try {
            Class<? extends Change> klass = History.getChangeClass(className);
            Method load = klass.getMethod("load", LineNumberReader.class, Pool.class);
            load.invoke(null, reader, pool);
            return true;
        } catch (Exception e) {
            logger.warn("Failed to skip over the content of change class " + className
                    + " while scanning for missing extension classes", e);
            return false;
        }
    }

    protected static boolean isClassAvailable(String className) {
        try {
            RefineServlet.getClass(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
