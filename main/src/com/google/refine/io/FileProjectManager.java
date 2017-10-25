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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.tools.tar.TarOutputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.ProjectDataStore;
import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.model.Project;
import com.google.refine.preference.TopList;

public class FileProjectManager extends ProjectManager {
    final static protected String PROJECT_DIR_SUFFIX = ".project";

    protected File                       _workspaceDir;

    final static Logger logger = LoggerFactory.getLogger("FileProjectManager");

    static public synchronized void initialize(File dir) {
        if (singleton != null) {
            logger.warn("Overwriting singleton already set: " + singleton);
        }
        logger.info("Using workspace directory: {}", dir.getAbsolutePath());
        
        FileProjectManager instance = new FileProjectManager(dir); 
        singleton = instance;
        
        // These calls need our singleton set, thus the unconventional control flow
        instance.load();
        instance.recover();
    }

    protected FileProjectManager(File dir) {
        super();
        _workspaceDir = dir;
        if (!_workspaceDir.exists() && !_workspaceDir.mkdirs()) {
            logger.error("Failed to create directory : " + _workspaceDir);
            return;
        }
    }

    public File getWorkspaceDir() {
        return _workspaceDir;
    }
    
    @Override
    public ProjectDataStore getProjectDataStore(Project project) {
        return new FileProjectDataStore(project, _workspaceDir, PROJECT_DIR_SUFFIX);
    }

    /**
     * Import an external project that has been received as a .tar file, expanded, and
     * copied into our workspace directory.
     *
     * @param project
     */
    @Override
    public boolean loadProjectMetadata(Project project) {
        synchronized (this) {
            ProjectMetadata metadata = project.loadMetadata();
            if (metadata == null) {
                metadata = project.recoverMetadata();
            }
            if (metadata != null) {
                _projectsMetadata.put(project.id, metadata);
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public void importProject(Project project, InputStream inputStream, boolean gziped) throws IOException {
        project.doImport(inputStream, gziped);
    }

    @Override
    public void exportProject(Project project, TarOutputStream tos) throws IOException {
        project.doExport(tos);
    }

    @Override
    protected void saveMetadata(ProjectMetadata metadata, Project project) throws Exception {
        project.saveMetadata(metadata);
    }

    @Override
    protected void saveProject(Project project) throws IOException{
        project.save();
    }

    @Override
    protected Project loadProject(long id) {
        Project project = new Project(id);
        
        if (project.load())
            return project;
        else
            return null;
    }

    /**
     * Save the workspace's data out to file in a safe way: save to a temporary file first
     * and rename it to the real file.
     */
    @Override
    protected void saveWorkspace() {
        synchronized (this) {
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

            File file = new File(_workspaceDir, "workspace.json");
            File oldFile = new File(_workspaceDir, "workspace.old.json");

            if (oldFile.exists()) {
                oldFile.delete();
            }
            
            if (file.exists()) {
                file.renameTo(oldFile);
            }

            tempFile.renameTo(file);

            logger.info("Saved workspace");
        }
    }

    protected boolean saveToFile(File file) throws IOException, JSONException {
        FileWriter writer = new FileWriter(file);
        boolean saveWasNeeded = false;
        try {
            JSONWriter jsonWriter = new JSONWriter(writer);
            jsonWriter.object();
            jsonWriter.key("projectIDs");
            jsonWriter.array();
            for (Long id : _projectsMetadata.keySet()) {
                ProjectMetadata metadata = _projectsMetadata.get(id);
                if (metadata != null) {
                    jsonWriter.value(id);
                    if (metadata.isDirty()) {
                        Project project = _projects.get(id);
                        
                        if (project == null)
                            project = new Project(id);
                        
                        project.saveMetadata(metadata);
                        saveWasNeeded = true;
                    }
                }
            }
            jsonWriter.endArray();
            writer.write('\n');

            jsonWriter.key("preferences");
            saveWasNeeded |= _preferenceStore.isDirty();
            _preferenceStore.write(jsonWriter, new Properties());

            jsonWriter.endObject();
        } finally {
            writer.close();
        }
        return saveWasNeeded;
    }



    @Override
    public void deleteProject(long projectID) {
        synchronized (this) {
            Project project = removeProject(projectID);
            
            // In case the project has been removed from memory, create a new instance to delete from data store.
            if (project == null)
                project = new Project(projectID);
            
            project.delete();
        }

        saveWorkspace();
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
            FileReader reader = null;
            try {
                reader = new FileReader(file);
                JSONTokener tokener = new JSONTokener(reader);
                JSONObject obj = (JSONObject) tokener.nextValue();

                JSONArray a = obj.getJSONArray("projectIDs");
                int count = a.length();
                for (int i = 0; i < count; i++) {
                    long id = a.getLong(i);

                    Project project = new Project(id);
                    ProjectMetadata metadata = project.loadMetadata();

                    _projectsMetadata.put(id, metadata);
                }

                if (obj.has("preferences") && !obj.isNull("preferences")) {
                    _preferenceStore.load(obj.getJSONObject("preferences"));
                }

                if (obj.has("expressions") && !obj.isNull("expressions")) { // backward compatibility
                    ((TopList) _preferenceStore.get("scripting.expressions"))
                    .load(obj.getJSONArray("expressions"));
                }

                found = true;
            } catch (JSONException e) {
                logger.warn("Error reading file", e);
            } catch (IOException e) {
                logger.warn("Error reading file", e);
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    logger.warn("Exception closing file",e);
                }
            }
        }

        return found;
    }

    protected void recover() {
        boolean recovered = false;
        for (File file : _workspaceDir.listFiles()) {
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
                        Project project = new Project(id);
                        
                        if (loadProjectMetadata(project)) {
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
}
