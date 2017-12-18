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

package com.google.refine.history;

import java.io.Writer;
import java.time.ZoneId;
import java.time.OffsetDateTime;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.Jsonizable;
import com.google.refine.ProjectManager;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;

/**
 * This is the metadata of a Change. It's small, so we can load it in order to
 * obtain information about a change without actually loading the change.
 */
public class HistoryEntry implements Jsonizable {
    final static Logger logger = LoggerFactory.getLogger("HistoryEntry");
    final public long   id;
    final public long   projectID;
    final public String description;
    final public OffsetDateTime   time;

    // the manager (deals with IO systems or databases etc.)
    final public HistoryEntryManager _manager;

    // the abstract operation, if any, that results in the change
    final public AbstractOperation operation;

    // the actual change, loaded on demand
    private transient Change _change;

    private final static String OPERATION = "operation";

    public void setChange(Change _change) {
        this._change = _change;
    }

    public Change getChange() {
        return _change;
    }

    static public long allocateID() {
        return Math.round(Math.random() * 1000000) + System.currentTimeMillis();
    }

    public HistoryEntry(long id, Project project, String description, AbstractOperation operation, Change change) {
        this(id,project.id,description,operation,OffsetDateTime.now(ZoneId.of("Z")));
        setChange(change);
    }

    protected HistoryEntry(long id, long projectID, String description, AbstractOperation operation, OffsetDateTime time) {
        this.id = id;
        this.projectID = projectID;
        this.description = description;
        this.operation = operation;
        this.time = time;
        this._manager = ProjectManager.singleton.getHistoryEntryManager();
        if (this._manager == null) {
            logger.error("Failed to get history entry manager from project manager: " 
                    + ProjectManager.singleton );
        }
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {

        writer.object();
        writer.key("id"); writer.value(id);
        writer.key("description"); writer.value(description);
        writer.key("time"); writer.value(ParsingUtilities.dateToString(time));
        if ("save".equals(options.getProperty("mode")) && operation != null) {
            writer.key(OPERATION); operation.write(writer, options);
        }
        writer.endObject();
    }

    public void save(Writer writer, Properties options){
        _manager.save(this, writer, options);
    }

    /**
     * Apply a change to a project.  In most cases you should already hold the Project lock
     * before calling this method to prevent deadlocks.
     * 
     * @param project the project the change should be applied to
     */
    public void apply(Project project) {
        if (getChange() == null) {
            ProjectManager.singleton.getHistoryEntryManager().loadChange(this);
        }

        synchronized (project) {
            getChange().apply(project);

            // When a change is applied, it can hang on to old data (in order to be able
            // to revert later). Hence, we need to save the change out.

            try {
                _manager.saveChange(this);
            } catch (Exception e) {
                e.printStackTrace();

                getChange().revert(project);

                throw new RuntimeException("Failed to apply change", e);
            }
        }
    }

    public void revert(Project project) {
        if (getChange() == null) {
            _manager.loadChange(this);
        }
        getChange().revert(project);
    }

    static public HistoryEntry load(Project project, String s) throws Exception {
        JSONObject obj = ParsingUtilities.evaluateJsonStringToObject(s);

        AbstractOperation operation = null;
        if (obj.has(OPERATION) && !obj.isNull(OPERATION)) {
            operation = OperationRegistry.reconstruct(project, obj.getJSONObject(OPERATION));
        }

        return new HistoryEntry(
            obj.getLong("id"),
            project.id,
            obj.getString("description"),
            operation,
            ParsingUtilities.stringToDate(obj.getString("time"))
        );
    }

    public void delete(){
        _manager.delete(this);
    }

}
