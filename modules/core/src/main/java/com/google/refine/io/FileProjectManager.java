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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.history.HistoryEntryManager;
import com.google.refine.model.Project;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.preference.TopList;
import com.google.refine.util.LocaleUtils;
import com.google.refine.util.ParsingUtilities;

public class FileProjectManager extends ProjectManager {

    final static protected String PROJECT_DIR_SUFFIX = ".project";
    public static final String WORKSPACE_JSON = "workspace.json";
    public static final String WORKSPACE_OLD_JSON = "workspace.old.json";
    public static final String WORKSPACE_TEMP_JSON = "workspace.temp.json";

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
        return getProjectDir(workspaceDir, projectID, true);
    }

    static public File getProjectDir(File workspaceDir, long projectID, boolean createIfMissing) {
        File dir = new File(workspaceDir, projectID + PROJECT_DIR_SUFFIX);
        if (!dir.exists()) {
            if (createIfMissing) {
                logger.warn("(Re)creating missing project directory - {}", dir.getAbsolutePath());
                dir.mkdir();
            } else {
                logger.error("Missing project directory {}", dir.getAbsolutePath());
                return null;
            }
        }
        return dir;
    }

    @JsonIgnore
    public File getProjectDir(long projectID) {
        return getProjectDir(projectID, true);
    }

    @JsonIgnore
    public File getProjectDir(long projectID, boolean createIfMissing) {
        return getProjectDir(_workspaceDir, projectID, createIfMissing);
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
                addProjectTags(metadata.getTags());
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
            if (!destEntry.toPath().normalize().startsWith(destDir.toPath().normalize())) {
                throw new IllegalArgumentException("Zip archives with files escaping their root directory are not allowed.");
            }
            File parent = destEntry.getParentFile();

            if (!parent.exists()) {
                parent.mkdirs();
            }

            if (tarEntry.isDirectory()) {
                destEntry.mkdirs();
            } else {
                FileOutputStream fout = new FileOutputStream(destEntry);
                try {
                    tin.transferTo(fout);
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

                    // TODO: replace with Files.copy(file.toPath(), tos);
                    copyFile(file, tos);

                    tos.closeArchiveEntry();
                }
            }
        }
    }

    /**
     * @deprecated use {@link Files#copy(Path, OutputStream)}
     */
    @Deprecated(since = "3.9")
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
            List<Long> modified = getModifiedProjectIds();
            boolean saveNeeded = (modified.size() > 0) || _preferenceStore.isDirty() || projectRemoved;
            if (!saveNeeded) {
                logger.debug("Skipping unnecessary workspace save");
                return;
            }
            File tempFile = saveWorkspaceToTempFile();
            if (tempFile == null) return;
            File file = new File(_workspaceDir, WORKSPACE_JSON);
            File oldFile = new File(_workspaceDir, WORKSPACE_OLD_JSON);

            if (oldFile.exists()) {
                if (!oldFile.delete()) {
                    logger.warn("Failed to delete previous backup {}", oldFile.getAbsolutePath());
                }
            }

            if (file.exists()) {
                if (!file.renameTo(oldFile)) {
                    logger.error("Failed to rename {} to {}", file.getAbsolutePath(), oldFile.getAbsolutePath());
                }
            }

            if (!tempFile.renameTo(file)) {
                logger.error("Failed to rename new temp workspace file to {}", file.getAbsolutePath());
            }
            projectRemoved = false;
            logger.info("Saved workspace");
        }
    }

    private File saveWorkspaceToTempFile() {
        File tempFile = new File(_workspaceDir, WORKSPACE_TEMP_JSON);
        try {
            saveToFile(tempFile);
        } catch (IOException e) {
            logger.warn("Failed to save workspace", e);
            return null;
        }
        // set the workspace to owner-only readable, because it can contain credentials
        tempFile.setReadable(false, false);
        tempFile.setReadable(true, true);
        return tempFile;
    }

    protected List<Long> getModifiedProjectIds() {
        List<Long> modified = _projectsMetadata.entrySet().stream()
                .filter(e -> {
                    ProjectMetadata metadata = e.getValue();
                    if (metadata == null) {
                        logger.error("Missing metadata for project ID {}", e.getKey());
                        return false;
                    }
                    return metadata.isDirty();
                }).map(Entry::getKey).collect(Collectors.toList());
        return modified;
    }

    protected void saveProjectMetadata(List<Long> modified) throws IOException {
        for (Long id : modified) {
            ProjectMetadata metadata = _projectsMetadata.get(id);
            if (metadata != null) {
                ProjectMetadataUtilities.save(metadata, getProjectDir(id));
            } else {
                logger.error("Missing metadata on save for project ID {}", id);
            }
        }
    }

    protected void saveToFile(File file) throws IOException {
        try (OutputStream stream = new FileOutputStream(file)) {
            // writeValue(OutputStream) is documented to use JsonEncoding.UTF8
            ParsingUtilities.defaultWriter.writeValue(stream, this);
            saveProjectMetadata(getModifiedProjectIds());
        }
    }

    @Override
    public void deleteProject(long projectID) {
        synchronized (this) {

            // Remove this project's tags from the overall map
            ProjectMetadata metadata = getProjectMetadata(projectID);
            if (metadata != null) {
                String[] tags = metadata.getTags();
                if (tags != null) { // should only ever happen during tests with mocked ProjectMetadata
                    removeProjectTags(tags);
                }
            }

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
        for (String filename : new String[] { WORKSPACE_JSON, WORKSPACE_TEMP_JSON, WORKSPACE_OLD_JSON }) {
            if (loadFromFile(new File(_workspaceDir, filename))) {
                return;
            }
        }
        logger.error("Failed to load workspace from any attempted alternatives.");
    }

    protected boolean loadFromFile(File file) {
        logger.info("Loading workspace: {}", file.getAbsolutePath());

        _projectsMetadata.clear();

        if (file.exists() || file.canRead()) {
            try {
                ParsingUtilities.mapper.readerForUpdating(this).readValue(file);

                // TODO: This seems odd. Why is this here?
                LocaleUtils.setLocale((String) this.getPreferenceStore().get("userLang"));

                return true;
            } catch (IOException e) {
                logger.warn("Failed to load workspace", e);
            }
        }
        return false;
    }

    protected void recover() {
        boolean recovered = false;
        File[] files = _workspaceDir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file != null && file.isDirectory() && !file.isHidden()) {
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
                            logger.info("Recovered project named {} in directory {}", getProjectMetadata(id).getName(), dirName);
                            recovered = true;
                        } else {
                            logger.warn("Failed to recover project in directory {}", dirName);

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

            File projectDir = getProjectDir(id, false);
            if (projectDir == null) {
                logger.error("Missing project directory for project {}", id);
                continue;
            }
            ProjectMetadata metadata = ProjectMetadataUtilities.load(projectDir);

            mergeEmptyUserMetadata(metadata);

            _projectsMetadata.put(id, metadata);

            if (metadata != null) {
                addProjectTags(metadata.getTags());
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
