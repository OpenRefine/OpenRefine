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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.google.refine.util.LocaleUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.poi.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.history.HistoryEntryManager;
import com.google.refine.model.Project;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.preference.TopList;
import com.google.refine.util.ParsingUtilities;

public class FileProjectManager extends ProjectManager {

    final static protected String PROJECT_DIR_SUFFIX = ".project";

    protected File _workspaceDir;

    protected static boolean projectRemoved = false;

    final static Logger logger = LoggerFactory.getLogger("FileProjectManager");

    static public synchronized void initialize(File dir) {
        if (singleton != null) {
            logger.warn("Overwriting singleton already set: " + singleton);
        }
        logger.info("Using workspace directory: {}", dir.getAbsolutePath());
        singleton = new FileProjectManager(dir);
        // This needs our singleton set, thus the unconventional control flow
        ((FileProjectManager) singleton).recover();
    }

    protected FileProjectManager(File dir) {
        super();
        _workspaceDir = dir;
        if (!_workspaceDir.exists() && !_workspaceDir.mkdirs()) {
            logger.error("Failed to create directory : " + _workspaceDir);
            return;
        }

        load();
    }

    @JsonIgnore
    public File getWorkspaceDir() {
        return _workspaceDir;
    }

    static public File getProjectDir(File workspaceDir, long projectID) {
        File dir = new File(workspaceDir, projectID + PROJECT_DIR_SUFFIX);
        if (!dir.exists()) {
            dir.mkdir();
        }
        return dir;
    }

    @JsonIgnore
    public File getProjectDir(long projectID) {
        return getProjectDir(_workspaceDir, projectID);
    }

    /**
     * Import an external project that has been received as a .tar file, expanded, and copied into our workspace
     * directory.
     *
     * @param projectID
     */
    @Override
    public boolean loadProjectMetadata(long projectID) {
        synchronized (this) {
            ProjectMetadata metadata = ProjectMetadataUtilities.load(getProjectDir(projectID));
            if (metadata == null) {
                metadata = ProjectMetadataUtilities.recover(getProjectDir(projectID), projectID);
            }

            if (metadata != null) {
                _projectsMetadata.put(projectID, metadata);
                if (_projectsTags == null) {
                    _projectsTags = new HashMap<String, Integer>();
                }

                if (metadata != null && metadata.getTags() != null) {
                    for (String tag : metadata.getTags()) {
                        if (_projectsTags.containsKey(tag)) {
                            _projectsTags.put(tag, _projectsTags.get(tag) + 1);
                        } else {
                            _projectsTags.put(tag, 1);
                        }
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public void importProject(long projectID, InputStream inputStream, boolean gziped) throws IOException {
        File destDir = this.getProjectDir(projectID);
        destDir.mkdirs();

        if (gziped) {
            GZIPInputStream gis = new GZIPInputStream(inputStream);
            untar(destDir, gis);
        } else {
            untar(destDir, inputStream);
        }
    }

    protected void untar(File destDir, InputStream inputStream) throws IOException {
        TarArchiveInputStream tin = new TarArchiveInputStream(inputStream);
        TarArchiveEntry tarEntry = null;

        while ((tarEntry = tin.getNextTarEntry()) != null) {
            File destEntry = new File(destDir, tarEntry.getName());
            File parent = destEntry.getParentFile();

            if (!parent.exists()) {
                parent.mkdirs();
            }

            if (tarEntry.isDirectory()) {
                destEntry.mkdirs();
            } else {
                FileOutputStream fout = new FileOutputStream(destEntry);
                try {
                    IOUtils.copy(tin, fout);
                } finally {
                    fout.close();
                }
            }
        }

        tin.close();
    }

    @Override
    public void exportProject(long projectId, TarArchiveOutputStream tos) throws IOException {
        File dir = this.getProjectDir(projectId);
        this.tarDir("", dir, tos);
    }

    protected void tarDir(String relative, File dir, TarArchiveOutputStream tos) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file == null) continue;
            if (!file.isHidden()) {
                String path = relative + file.getName();

                if (file.isDirectory()) {
                    tarDir(path + File.separator, file, tos);
                } else {
                    TarArchiveEntry entry = new TarArchiveEntry(path);

                    entry.setMode(TarArchiveEntry.DEFAULT_FILE_MODE);
                    entry.setSize(file.length());
                    entry.setModTime(file.lastModified());

                    tos.putArchiveEntry(entry);

                    copyFile(file, tos);

                    tos.closeArchiveEntry();
                }
            }
        }
    }

    protected void copyFile(File file, OutputStream os) throws IOException {
        final int buffersize = 4096;

        FileInputStream fis = new FileInputStream(file);
        try {
            byte[] buf = new byte[buffersize];
            int count;

            while ((count = fis.read(buf, 0, buffersize)) != -1) {
                os.write(buf, 0, count);
            }
        } finally {
            fis.close();
        }
    }

    @Override
    public void saveMetadata(ProjectMetadata metadata, long projectId) throws Exception {
        File projectDir = getProjectDir(projectId);
        ProjectMetadataUtilities.save(metadata, projectDir);
    }

    @Override
    protected void saveProject(Project project) throws IOException {
        ProjectUtilities.save(project);
    }

    @Override
    public Project loadProject(long id) {
        return ProjectUtilities.load(getProjectDir(id), id);
    }

    /**
     * Save the workspace's data out to file in a safe way: save to a temporary file first and rename it to the real
     * file.
     */
    @Override
    protected void saveWorkspace() {
        synchronized (this) {
            // TODO refactor this so that we check if the save is needed before writing to the file!
            File tempFile = new File(_workspaceDir, "workspace.temp.json");
            try {
                if (!saveToFile(tempFile)) {
                    // If the save wasn't really needed, just keep what we had
                    tempFile.delete();
                    logger.info("Skipping unnecessary workspace save");
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();

                logger.warn("Failed to save workspace");
                return;
            }
            // set the workspace to owner-only readable, because it can contain credentials
            tempFile.setReadable(false, false);
            tempFile.setReadable(true, true);
            File file = new File(_workspaceDir, "workspace.json");
            File oldFile = new File(_workspaceDir, "workspace.old.json");

            if (oldFile.exists()) {
                oldFile.delete();
            }

            if (file.exists()) {
                file.renameTo(oldFile);
            }

            tempFile.renameTo(file);
            projectRemoved = false;
            logger.info("Saved workspace");
        }
    }

    protected boolean saveNeeded() {
        boolean projectSaveNeeded = _projectsMetadata.entrySet().stream()
                .anyMatch(e -> e.getValue() != null && e.getValue().isDirty());
        return projectSaveNeeded || _preferenceStore.isDirty() || projectRemoved;
    }

    protected void saveProjectMetadata() throws IOException {
        for (Entry<Long, ProjectMetadata> entry : _projectsMetadata.entrySet()) {
            ProjectMetadata metadata = entry.getValue();
            if (metadata != null && metadata.isDirty()) {
                ProjectMetadataUtilities.save(metadata, getProjectDir(entry.getKey()));
            }
        }
    }

    protected boolean saveToFile(File file) throws IOException {
        OutputStream stream = new FileOutputStream(file);
        boolean saveWasNeeded = saveNeeded();
        try {
            // writeValue(OutputStream) is documented to use JsonEncoding.UTF8
            ParsingUtilities.defaultWriter.writeValue(stream, this);
            saveProjectMetadata();
        } finally {
            stream.close();
        }
        return saveWasNeeded;
    }

    @Override
    public void deleteProject(long projectID) {
        synchronized (this) {
            removeProject(projectID);

            File dir = getProjectDir(projectID);
            if (dir.exists()) {
                deleteDir(dir);
            }
        }
        projectRemoved = true;
        saveWorkspace();
    }

    static protected void deleteDir(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file == null) continue;
            if (file.isDirectory()) {
                deleteDir(file);
            } else {
                file.delete();
            }
        }
        dir.delete();
    }

    protected void load() {
        if (loadFromFile(new File(_workspaceDir, "workspace.json"))) {
            return;
        }
        if (loadFromFile(new File(_workspaceDir, "workspace.temp.json"))) {
            return;
        }
        if (loadFromFile(new File(_workspaceDir, "workspace.old.json"))) {
            return;
        }
        logger.error("Failed to load workspace from any attempted alternatives.");
    }

    protected boolean loadFromFile(File file) {
        logger.info("Loading workspace: {}", file.getAbsolutePath());

        _projectsMetadata.clear();

        boolean found = false;

        if (file.exists() || file.canRead()) {
            try {
                ParsingUtilities.mapper.readerForUpdating(this).readValue(file);

                LocaleUtils.setLocale((String) this.getPreferenceStore().get("userLang"));

                found = true;
            } catch (IOException e) {
                logger.warn(e.toString());
            }
        }

        return found;
    }

    protected void recover() {
        boolean recovered = false;
        File[] files = _workspaceDir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file == null) continue;
            if (file.isDirectory() && !file.isHidden()) {
                String dirName = file.getName();
                if (file.getName().endsWith(PROJECT_DIR_SUFFIX)) {
                    String idString = dirName.substring(0, dirName.length() - PROJECT_DIR_SUFFIX.length());
                    long id = -1;
                    try {
                        id = Long.parseLong(idString);
                    } catch (NumberFormatException e) {
                        // ignore
                    }

                    if (id > 0 && !_projectsMetadata.containsKey(id)) {
                        if (loadProjectMetadata(id)) {
                            logger.info("Recovered project named "
                                    + getProjectMetadata(id).getName()
                                    + " in directory " + dirName);
                            recovered = true;
                        } else {
                            logger.warn("Failed to recover project in directory " + dirName);

                            file.renameTo(new File(file.getParentFile(), dirName + ".corrupted"));
                        }
                    }
                }
            }
        }
        if (recovered) {
            saveWorkspace();
        }
    }

    @Override
    public HistoryEntryManager getHistoryEntryManager() {
        return new FileHistoryEntryManager();
    }

    public static void gzipTarToOutputStream(Project project, OutputStream os) throws IOException {
        GZIPOutputStream gos = new GZIPOutputStream(os);
        TarArchiveOutputStream tos = new TarArchiveOutputStream(gos);
        try {
            ProjectManager.singleton.exportProject(project.id, tos);
        } finally {
            tos.close();
            gos.close();
        }
    }

    @JsonProperty("projectIDs")
    public Set<Long> getProjectIds() {
        return _projectsMetadata.keySet();
    }

    @JsonProperty("projectIDs")
    protected void loadProjects(List<Long> projectIDs) {
        for (Long id : projectIDs) {

            File projectDir = getProjectDir(id);
            ProjectMetadata metadata = ProjectMetadataUtilities.load(projectDir);

            mergeEmptyUserMetadata(metadata);

            _projectsMetadata.put(id, metadata);

            if (metadata != null && metadata.getTags() != null) {
                for (String tag : metadata.getTags()) {
                    if (_projectsTags.containsKey(tag)) {
                        _projectsTags.put(tag, _projectsTags.get(tag) + 1);
                    } else {
                        _projectsTags.put(tag, 1);
                    }
                }
            }
        }
    }

    @JsonProperty("preferences")
    protected void setPreferences(PreferenceStore preferences) {
        if (preferences != null) {
            _preferenceStore = preferences;
        }
    }

    // backwards compatibility
    @JsonProperty("expressions")
    protected void setExpressions(TopList newExpressions) {
        if (newExpressions != null) {
            _preferenceStore.put("scripting.expressions", newExpressions);
        }
    }
}
