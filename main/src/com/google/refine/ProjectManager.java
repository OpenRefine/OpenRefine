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

package com.google.refine;

import com.google.refine.util.GetProjectIDException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.history.HistoryEntryManager;
import com.google.refine.model.Project;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.preference.TopList;
import com.google.refine.util.ParsingUtilities;

/**
 * ProjectManager is responsible for loading and saving the workspace and projects.
 *
 *
 */
public abstract class ProjectManager {

    // last n expressions used across all projects
    static public final int EXPRESSION_HISTORY_MAX = 100;

    // If a project has been idle this long, flush it from memory
    static protected final int PROJECT_FLUSH_DELAY = 1000 * 60 * 15; // 15 minutes

    // Don't spend more than this much time saving projects if doing a quick save
    static protected final int QUICK_SAVE_MAX_TIME = 1000 * 30; // 30 secs

    protected Map<Long, ProjectMetadata> _projectsMetadata;
    protected Map<String, Integer> _projectsTags;// TagName, number of projects having that tag
    protected PreferenceStore _preferenceStore;

    final static Logger logger = LoggerFactory.getLogger("ProjectManager");

    /**
     * What caches the lookups of projects.
     */
    transient protected LookupCacheManager _lookupCacheManager = new LookupCacheManager();

    /**
     * Flag for heavy operations like creating or importing projects. Workspace saves are skipped while it's set.
     */
    transient protected int _busy = 0;

    /**
     * While each project's metadata is loaded completely at start-up, each project's raw data is loaded only when the
     * project is accessed by the user. This is because project metadata is tiny compared to raw project data. This hash
     * map from project ID to project is more like a last accessed-last out cache.
     */
    transient protected Map<Long, Project> _projects;

    static public ProjectManager singleton;

    protected ProjectManager() {
        _projectsMetadata = new HashMap<Long, ProjectMetadata>();
        _preferenceStore = new PreferenceStore();
        _projects = new HashMap<Long, Project>();
        _projectsTags = new HashMap<String, Integer>();

        preparePreferenceStore(_preferenceStore);
    }

    public void dispose() {
        save(true); // complete save

        for (Project project : _projects.values()) {
            if (project != null) {
                project.dispose();
            }
        }

        _projects.clear();
        _projectsMetadata.clear();
    }

    /**
     * Registers the project in the memory of the current session
     * 
     * @param project
     * @param projectMetadata
     */
    public void registerProject(Project project, ProjectMetadata projectMetadata) {
        synchronized (this) {
            _projects.put(project.id, project);
            _projectsMetadata.put(project.id, projectMetadata);
            if (_projectsTags == null)
                _projectsTags = new HashMap<String, Integer>();
            String[] tags = projectMetadata.getTags();
            if (tags != null) {
                for (String tag : tags) {
                    if (_projectsTags.containsKey(tag)) {
                        _projectsTags.put(tag, _projectsTags.get(tag) + 1);
                    } else {
                        _projectsTags.put(tag, 1);
                    }
                }
            }
        }
    }

    /**
     * Load project metadata from data storage
     * 
     * @param projectID
     * @return
     */
    public abstract boolean loadProjectMetadata(long projectID);

    /**
     * Loads a project from the data store into memory
     * 
     * @param id
     * @return
     */
    protected abstract Project loadProject(long id);

    /**
     * Import project from a Refine archive
     * 
     * @param projectID
     * @param inputStream
     * @param gziped
     * @throws IOException
     */
    public abstract void importProject(long projectID, InputStream inputStream, boolean gziped) throws IOException;

    /**
     * Export project to a Refine archive
     * 
     * @param projectId
     * @param tos
     * @throws IOException
     */
    public abstract void exportProject(long projectId, TarArchiveOutputStream tos) throws IOException;

    /**
     * Saves a project and its metadata to the data store
     * 
     * @param id
     */
    public void ensureProjectSaved(long id) {
        synchronized (this) {
            ProjectMetadata metadata = this.getProjectMetadata(id);
            if (metadata != null) {
                try {
                    saveMetadata(metadata, id);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } // FIXME what should be the behaviour if metadata is null? i.e. not found

            Project project = getProject(id);
            if (project != null && metadata != null && metadata.getModified().isAfter(project.getLastSave())) {
                try {
                    saveProject(project);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } // FIXME what should be the behaviour if project is null? i.e. not found or loaded.
              // FIXME what should happen if the metadata is found, but not the project? or vice versa?
        }

    }

    /**
     * Save project metadata to the data store
     * 
     * @param metadata
     * @param projectId
     * @throws Exception
     */
    public abstract void saveMetadata(ProjectMetadata metadata, long projectId) throws Exception;

    /**
     * Save project to the data store
     * 
     * @param project
     * @throws IOException
     */
    protected abstract void saveProject(Project project) throws IOException;

    /**
     * Save workspace and all projects to data store
     * 
     * @param allModified
     */
    public void save(boolean allModified) {
        if (allModified || _busy == 0) {
            saveProjects(allModified);
            saveWorkspace();
        }
    }

    /**
     * Saves the workspace to the data store
     */
    protected abstract void saveWorkspace();

    /**
     * A utility class to prioritize projects for saving, depending on how long ago they have been changed but have not
     * been saved.
     */
    static protected class SaveRecord {

        final Project project;
        final long overdue;

        SaveRecord(Project project, long overdue) {
            this.project = project;
            this.overdue = overdue;
        }
    }

    /**
     * Saves all projects to the data store
     * 
     * @param allModified
     */
    protected void saveProjects(boolean allModified) {
        List<SaveRecord> records = new ArrayList<SaveRecord>();
        LocalDateTime startTimeOfSave = LocalDateTime.now();

        synchronized (this) {
            for (long id : _projectsMetadata.keySet()) {
                ProjectMetadata metadata = getProjectMetadata(id);
                Project project = _projects.get(id); // don't call getProject() as that will load the project.

                if (project != null) {
                    boolean hasUnsavedChanges = metadata.getModified().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() >= project
                            .getLastSave().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    // We use >= instead of just > to avoid the case where a newly created project
                    // has the same modified and last save times, resulting in the project not getting
                    // saved at all.

                    if (hasUnsavedChanges) {
                        long msecsOverdue = startTimeOfSave.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                - project.getLastSave().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

                        records.add(new SaveRecord(project, msecsOverdue));

                    } else if (!project.getProcessManager().hasPending()
                            && startTimeOfSave.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - project.getLastSave()
                                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() > PROJECT_FLUSH_DELAY) {

                        /*
                         * It's been a while since the project was last saved and it hasn't been modified. We can safely
                         * remove it from the cache to save some memory.
                         */
                        _projects.remove(id).dispose();
                    }
                }
            }
        }

        if (records.size() > 0) {
            Collections.sort(records, new Comparator<SaveRecord>() {

                @Override
                public int compare(SaveRecord o1, SaveRecord o2) {
                    if (o1.overdue < o2.overdue) {
                        return 1;
                    } else if (o1.overdue > o2.overdue) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            });

            logger.info(allModified ? "Saving all modified projects ..." : "Saving some modified projects ...");

            for (int i = 0; i < records.size() &&
                    (allModified || (LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() -
                            startTimeOfSave.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() < QUICK_SAVE_MAX_TIME)); i++) {

                try {
                    saveProject(records.get(i).project);
                } catch (Exception e) {
                    e.printStackTrace();
                    // In case we're running low on memory, free as much as we can
                    disposeUnmodifiedProjects();
                }
            }
        }
    }

    /**
     * Flush all unmodified projects from memory.
     */
    protected void disposeUnmodifiedProjects() {
        synchronized (this) {
            for (long id : _projectsMetadata.keySet()) {
                ProjectMetadata metadata = getProjectMetadata(id);
                Project project = _projects.get(id);
                if (project != null && !project.getProcessManager().hasPending()
                        && metadata.getModified().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() < project.getLastSave()
                                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()) {
                    _projects.remove(id).dispose();
                }
            }
        }
    }

    /**
     * Gets the LookupCacheManager from memory
     */
    @JsonIgnore
    public LookupCacheManager getLookupCacheManager() {
        return _lookupCacheManager;
    }

    /**
     * Gets the project metadata from memory Requires that the metadata has already been loaded from the data store
     * 
     * @param id
     * @return
     */
    public ProjectMetadata getProjectMetadata(long id) {
        return _projectsMetadata.get(id);
    }

    /**
     * Gets the project metadata from memory Requires that the metadata has already been loaded from the data store
     * 
     * @param name
     * @return
     */
    public ProjectMetadata getProjectMetadata(String name) {
        for (ProjectMetadata pm : _projectsMetadata.values()) {
            if (pm.getName().equals(name)) {
                return pm;
            }
        }
        return null;
    }

    /**
     * Tries to find the project id when given a project name Requires that all project metadata exists has been loaded
     * to memory from the data store
     * 
     * @param name
     *            The name of the project
     * @return The id of the project
     * @throws GetProjectIDException
     *             If no unique project is found with the given name
     */
    public long getProjectID(String name) throws GetProjectIDException {
        Integer c = 0;
        Long id = 0L;
        for (Entry<Long, ProjectMetadata> entry : _projectsMetadata.entrySet()) {
            if (entry.getValue().getName().equals(name)) {
                id = entry.getKey();
                c += 1;
            }
        }
        if (c == 1) {
            return id;
        } else if (c == 0) {
            throw new GetProjectIDException("Unable to find project with name: " + name);
        } else {
            throw new GetProjectIDException(c + " projects found with name: " + name);
        }
    }

    /**
     * A valid user meta data definition should have name and display property
     * 
     * @param placeHolderJsonObj
     * @return
     */
    private boolean isValidUserMetadataDefinition(ObjectNode placeHolderJsonObj) {
        return (placeHolderJsonObj != null &&
                placeHolderJsonObj.has("name") &&
                placeHolderJsonObj.has("display"));
    }

    public void mergeEmptyUserMetadata(ProjectMetadata metadata) {
        if (metadata == null)
            return;

        // place holder
        ArrayNode userMetadataPreference = null;
        // actual metadata for project
        ArrayNode jsonObjArray = metadata.getUserMetadata();

        initDisplay(jsonObjArray);

        String userMeta = (String) _preferenceStore.get(PreferenceStore.USER_METADATA_KEY);
        if (userMeta == null)
            return;
        userMetadataPreference = ParsingUtilities.mapper.createArrayNode();

        for (int index = 0; index < userMetadataPreference.size(); index++) {
            boolean found = false;
            ObjectNode placeHolderJsonObj = (ObjectNode) userMetadataPreference.get(index);

            if (!isValidUserMetadataDefinition(placeHolderJsonObj)) {
                logger.warn("Skipped invalid user metadata definition" + placeHolderJsonObj.toString());
                continue;
            }

            for (int i = 0; i < jsonObjArray.size(); i++) {
                JsonNode jsonObj = jsonObjArray.get(i);
                if (!(jsonObj instanceof ObjectNode)) {
                    continue;
                }
                ObjectNode node = (ObjectNode) jsonObj;
                if (node.get("name").asText("").equals(placeHolderJsonObj.get("name").asText(""))) {
                    found = true;
                    node.set("display", placeHolderJsonObj.get("display"));
                    break;
                }
            }

            if (!found) {
                placeHolderJsonObj.put("value", "");
                metadata.getUserMetadata().add(placeHolderJsonObj);
                logger.info("Put the placeholder {} for project {}",
                        placeHolderJsonObj.get("name").asText(""),
                        metadata.getName());
            }
        }
    }

    /**
     * honor the meta data preference
     * 
     * @param jsonObjArray
     */
    private void initDisplay(ArrayNode jsonObjArray) {
        for (int index = 0; index < jsonObjArray.size(); index++) {
            if (jsonObjArray.get(index) instanceof ObjectNode) {
                ObjectNode projectMetaJsonObj = (ObjectNode) jsonObjArray.get(index);
                projectMetaJsonObj.put("display", false);
            }
        }
    }

    /**
     * Gets all the project Metadata currently held in memory.
     * 
     * @return
     */
    @JsonIgnore
    public Map<Long, ProjectMetadata> getAllProjectMetadata() {
        for (Project project : _projects.values()) {
            mergeEmptyUserMetadata(project.getMetadata());
        }

        return _projectsMetadata;
    }

    /**
     * Gets all the project tags currently held in memory
     * 
     * @return
     */
    @JsonIgnore
    public Map<String, Integer> getAllProjectTags() {
        return _projectsTags;
    }

    /**
     * Gets the required project from the data store If project does not already exist in memory, it is loaded from the
     * data store
     * 
     * @param id
     *            the id of the project
     * @return the project with the matching id, or null if it can't be found
     */
    public Project getProject(long id) {
        synchronized (this) {
            if (_projects.containsKey(id)) {
                return _projects.get(id);
            } else {
                Project project = loadProject(id);
                if (project != null) {
                    _projects.put(id, project);
                }
                return project;
            }
        }
    }

    /**
     * Gets the preference store
     * 
     * @return
     */
    @JsonProperty("preferences")
    public PreferenceStore getPreferenceStore() {
        return _preferenceStore;
    }

    /**
     * Gets all expressions from the preference store
     * 
     * @return
     */
    @JsonIgnore
    public List<String> getExpressions() {
        return ((TopList) _preferenceStore.get("scripting.expressions")).getList();
    }

    /**
     * The history entry manager deals with changes
     * 
     * @return manager for handling history
     */
    @JsonIgnore
    public abstract HistoryEntryManager getHistoryEntryManager();

    /**
     * Remove the project from the data store
     * 
     * @param project
     */
    public void deleteProject(Project project) {
        deleteProject(project.id);
    }

    /**
     * Remove project from data store
     * 
     * @param projectID
     */
    public abstract void deleteProject(long projectID);

    /**
     * Removes project from memory
     * 
     * @param projectID
     */
    protected void removeProject(long projectID) {
        if (_projects.containsKey(projectID)) {
            _projects.remove(projectID).dispose();
        }
        if (_projectsMetadata.containsKey(projectID)) {
            _projectsMetadata.remove(projectID);
        }
    }

    /**
     * Sets the flag for long running operations. This will prevent workspace saves from happening while it's set.
     * 
     * @param busy
     */
    public void setBusy(boolean busy) {
        synchronized (this) {
            if (busy) {
                _busy++;
            } else {
                _busy--;
            }
        }
    }

    /**
     * Add the latest expression to the preference store
     * 
     * @param s
     */
    public void addLatestExpression(String s) {
        synchronized (this) {
            ((TopList) _preferenceStore.get("scripting.expressions")).add(s);
        }
    }

    /**
     *
     * @param ps
     */
    static protected void preparePreferenceStore(PreferenceStore ps) {
        ps.put("scripting.expressions", new TopList(EXPRESSION_HISTORY_MAX));
        ps.put("scripting.starred-expressions", new TopList(Integer.MAX_VALUE));
    }
}
