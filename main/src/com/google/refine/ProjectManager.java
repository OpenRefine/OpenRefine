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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.tools.tar.TarOutputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.history.HistoryEntryManager;
import com.google.refine.model.Project;
import com.google.refine.model.medadata.IMetadata;
import com.google.refine.model.medadata.MetadataFormat;
import com.google.refine.model.medadata.ProjectMetadata;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.preference.TopList;

/**
 * ProjectManager is responsible for loading and saving the workspace and projects.
 *
 *
 */
public abstract class ProjectManager {
    // last n expressions used across all projects
    static protected final int s_expressionHistoryMax = 100;

    // If a project has been idle this long, flush it from memory
    static protected final int PROJECT_FLUSH_DELAY = 1000 * 60 * 15; // 15 minutes
    
    // Don't spend more than this much time saving projects if doing a quick save
    static protected final int QUICK_SAVE_MAX_TIME = 1000 * 30; // 30 secs


    protected PreferenceStore            _preferenceStore;

    final static Logger logger = LoggerFactory.getLogger("ProjectManager");

    /**
     *  What caches the joins between projects.
     */
    transient protected InterProjectModel _interProjectModel = new InterProjectModel();

    /**
     *  Flag for heavy operations like creating or importing projects.  Workspace saves are skipped while it's set.
     */
    transient protected int _busy = 0;

    /**
     *  While each project's metadata is loaded completely at start-up, each project's raw data
     *  is loaded only when the project is accessed by the user. This is because project
     *  metadata is tiny compared to raw project data. This hash map from project ID to project
     *  is more like a last accessed-last out cache.
     */
    transient protected Map<Long, Project> _projects;

    static public ProjectManager singleton;
    
    protected ProjectManager(){
        _preferenceStore = new PreferenceStore();
        _projects = new HashMap<Long, Project>();

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
    }

    /**
     * Registers the project in the memory of the current session
     * @param project
     * @param projectMetadata
     */
    public void registerProject(Project project) {
        synchronized (this) {
            _projects.put(project.id, project);
        }
    }

    /**
     * Load project metadata from data storage
     * @param projectID
     * @return
     */
    public abstract boolean loadProjectMetadata(long projectID);

    /**
     * Loads a project from the data store into memory
     * @param id
     * @return
     */
    protected abstract Project loadProject(long id);

    /**
     * Import project from a Refine archive
     * @param projectID
     * @param inputStream
     * @param gziped
     * @throws IOException
     */
    public abstract void importProject(long projectID, InputStream inputStream, boolean gziped) throws IOException;

    /**
     * Export project to a Refine archive
     * @param projectId
     * @param tos
     * @throws IOException
     */
    public abstract void exportProject(long projectId, TarOutputStream tos) throws IOException;


    /**
     * Saves a project and its metadata to the data store
     * @param id
     */
    public void ensureProjectSaved(long id) {
        synchronized(this){
            Map<MetadataFormat, IMetadata> metadataMap = this.getProject(id).getMetadataMap();
            for (IMetadata metadata : metadataMap.values()) {
                if (metadata != null) {
                    try {
                        // XXX: metadata: something like metadata.save()
                        saveMetadata(metadata, id);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }//FIXME what should be the behaviour if metadata is null? i.e. not found
            }

            Project project = getProject(id);
            if (project != null && 
                    project.getProjectMetadata() != null && 
                    project.getProjectMetadata().getModified().after(project.getLastSave())) {
                try {
                    saveProject(project);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }//FIXME what should be the behaviour if project is null? i.e. not found or loaded.
            //FIXME what should happen if the metadata is found, but not the project? or vice versa?
        }

    }

    /**
     * Save project metadata to the data store
     * @param metadata
     * @param projectId
     * @throws Exception
     */
    public abstract void saveMetadata(IMetadata metadata, long projectId) throws Exception;

    /**
     * Save project to the data store
     * @param project
     * @throws IOException 
     */
    protected abstract void saveProject(Project project) throws IOException;

    /**
     * Save workspace and all projects to data store
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
     * A utility class to prioritize projects for saving, depending on how long ago
     * they have been changed but have not been saved.
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
     * @param allModified
     */
    protected void saveProjects(boolean allModified) {
        List<SaveRecord> records = new ArrayList<SaveRecord>();
        Date startTimeOfSave = new Date();
        
        synchronized (this) {
            for (long id : _projects.keySet()) {
                Project project = _projects.get(id); // don't call getProject() as that will load the project.
                IMetadata metadata = project.getProjectMetadata();

                if (project != null) {
                    boolean hasUnsavedChanges =
                        metadata.getModified().getTime() >= project.getLastSave().getTime();
                    // We use >= instead of just > to avoid the case where a newly created project
                    // has the same modified and last save times, resulting in the project not getting
                    // saved at all.

                    if (hasUnsavedChanges) {
                        long msecsOverdue = startTimeOfSave.getTime() - project.getLastSave().getTime();

                        records.add(new SaveRecord(project, msecsOverdue));

                    } else if (!project.getProcessManager().hasPending()
                              && startTimeOfSave.getTime() - project.getLastSave().getTime() > PROJECT_FLUSH_DELAY) {
                        
                        /*
                         *  It's been a while since the project was last saved and it hasn't been
                         *  modified. We can safely remove it from the cache to save some memory.
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

            logger.info(allModified ?
                "Saving all modified projects ..." :
                "Saving some modified projects ..."
            );

            for (int i = 0;
                 i < records.size() &&
                    (allModified || (new Date().getTime() - startTimeOfSave.getTime() < QUICK_SAVE_MAX_TIME));
                 i++) {

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
            for (long id : _projects.keySet()) {
                IMetadata metadata = getProjectMetadata(id);
                Project project = _projects.get(id);
                if (project != null && !project.getProcessManager().hasPending() 
                        && metadata.getModified().getTime() < project.getLastSave().getTime()) {
                        _projects.remove(id).dispose();
                }
            }
        }
    }

    /**
     * Gets the InterProjectModel from memory
     */
    public InterProjectModel getInterProjectModel() {
        return _interProjectModel;
    }


    /**
     * Gets the project metadata from memory
     * Requires that the metadata has already been loaded from the data store.
     * Delegate to project itself 
     * @param id
     * @return
     */
    public ProjectMetadata getProjectMetadata(long id) {
        return (ProjectMetadata) _projects.get(id).getProjectMetadata();
    }
    
    /**
     * Get metadata based on the format
     * @param id
     * @param format
     * @return
     */
    public IMetadata getMetadata(long id, MetadataFormat format) {
        return _projects.get(id).getMetadata(format);
    }

    /**
     * Gets the project metadata from memory
     * Requires that the metadata has already been loaded from the data store
     * @param name
     * @return
     */
    /** XXX: metadata: get rid of it for now. try to get it from project object
    public IMetadata getProjectMetadata(String name) {
        for (IMetadata pm : _projects.values()) {
            if (pm.getName().equals(name)) {
                return pm;
            }
        }
        return null;
    }
    */

    /**
     * Tries to find the project id when given a project name
     * Requires that all project metadata exists has been loaded to memory from the data store
     * @param name
     *     The name of the project
     * @return
     *     The id of the project, or -1 if it cannot be found
     */
    public long getProjectID(String name) {
        for (Entry<Long, Project> entry : _projects.entrySet()) {
            if (entry.getValue().getProjectMetadata().getName().equals(name)) {
                return entry.getKey();
            }
        }
        return -1;
    }
    
    /**
     * A valid user meta data definition should have name and display property
     * @param placeHolderJsonObj
     * @return
     */
    private boolean isValidUserMetadataDefinition(JSONObject placeHolderJsonObj) {
        return (placeHolderJsonObj != null &&
                placeHolderJsonObj.has("name") &&
            placeHolderJsonObj.has("display"));
    }
    
    public void mergeEmptyUserMetadata(IMetadata metadata) {
        if (metadata == null)
            return;
        
        // place holder
        JSONArray userMetadataPreference = null;
        // actual metadata for project
        JSONArray jsonObjArray = metadata.getUserMetadata();
        
        initDisplay(jsonObjArray);
        
        try {
            String userMeta = (String)_preferenceStore.get(PreferenceStore.USER_METADATA_KEY);
            if (userMeta == null)
                return;
            userMetadataPreference = new JSONArray(userMeta);
        } catch (JSONException e1) {
            logger.warn("wrong definition of userMetadata format. Please use form [{\"name\": \"client name\", \"display\":true}, {\"name\": \"progress\", \"display\":false}]");
            logger.error(ExceptionUtils.getStackTrace(e1));
        }
        
        for (int index = 0; index < userMetadataPreference.length(); index++) {
            try {
                boolean found = false;
                JSONObject placeHolderJsonObj = userMetadataPreference.getJSONObject(index);
                
                if (!isValidUserMetadataDefinition(placeHolderJsonObj)) {
                    logger.warn("Skipped invalid user metadata definition" + placeHolderJsonObj.toString());
                    continue;
                }

                for (int i = 0; i < jsonObjArray.length(); i++) {
                    JSONObject jsonObj = jsonObjArray.getJSONObject(i);
                    if (jsonObj.getString("name").equals(placeHolderJsonObj.getString("name"))) {
                        found = true;
                        jsonObj.put("display", placeHolderJsonObj.get("display"));
                        break;
                    }
                }

                if (!found) {
                    placeHolderJsonObj.put("value", "");
                    metadata.getUserMetadata().put(placeHolderJsonObj);
                    logger.info("Put the placeholder {} for project {}",
                            placeHolderJsonObj.getString("name"),
                            metadata.getName());
                } 
            } catch (JSONException e) {
                logger.warn("Exception when mergeEmptyUserMetadata",e);
            }
        }
    }
    
    /**
     * honor the meta data preference
     * @param jsonObjArray
     */
    private void initDisplay(JSONArray jsonObjArray) {
        for (int index = 0; index < jsonObjArray.length(); index++) {
            try {
                JSONObject projectMetaJsonObj = jsonObjArray.getJSONObject(index);
                projectMetaJsonObj.put("display", false);
            } catch (JSONException e) {
                logger.error(ExceptionUtils.getStackTrace(e));
            }
        }
    }

    /**
     * Gets all the project Metadata currently held in memory.
     * @return
     */
    public Map<Long, IMetadata> getAllProjectMetadata() {
        Map<Long, IMetadata> resultMap = new HashMap<Long, IMetadata>();
        
        for(Entry<Long, Project> entry : _projects.entrySet()) {
            ProjectMetadata metadata = entry.getValue().getProjectMetadata();
            mergeEmptyUserMetadata(metadata);
            resultMap.put(entry.getKey(), metadata);
        }
        return resultMap;
    }

    /**
     * Gets the required project from the data store
     * If project does not already exist in memory, it is loaded from the data store
     * @param id
     *     the id of the project
     * @return
     *     the project with the matching id, or null if it can't be found
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
     * @return
     */
    public PreferenceStore getPreferenceStore() {
        return _preferenceStore;
    }

    /**
     * Gets all expressions from the preference store
     * @return
     */
    public List<String> getExpressions() {
        return ((TopList) _preferenceStore.get("scripting.expressions")).getList();
    }

    /**
     * The history entry manager deals with changes
     * @return manager for handling history
     */
    public abstract HistoryEntryManager getHistoryEntryManager();


    /**
     * Remove the project from the data store
     * @param project
     */
    public void deleteProject(Project project) {
        deleteProject(project.id);
    }

    /**
     * Remove project from data store
     * @param projectID
     */
    public abstract void deleteProject(long projectID);

    /**
     * Removes project from memory
     * @param projectID
     */
    protected void removeProject(long projectID){
        if (_projects.containsKey(projectID)) {
            _projects.remove(projectID).dispose();
        }
    }


    /**
     * Sets the flag for long running operations.  This will prevent
     * workspace saves from happening while it's set.
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
   public static void preparePreferenceStore(PreferenceStore ps) {
       ps.put("scripting.expressions", new TopList(s_expressionHistoryMax));
       ps.put("scripting.starred-expressions", new TopList(Integer.MAX_VALUE));
   }
}
