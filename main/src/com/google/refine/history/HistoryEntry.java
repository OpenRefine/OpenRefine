package com.google.refine.history;

import java.io.Writer;
import java.util.Date;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

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
    final public long   id;
    final public long   projectID;
    final public String description;
    final public Date   time;

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
        this.id = id;
        this.projectID = project.id;
        this.description = description;
        this.operation = operation;
        this.time = new Date();

        this._manager = ProjectManager.singleton.getHistoryEntryManager();
        setChange(change);
    }

    protected HistoryEntry(long id, long projectID, String description, AbstractOperation operation, Date time) {
        this.id = id;
        this.projectID = projectID;
        this.description = description;
        this.operation = operation;
        this.time = time;
        this._manager = ProjectManager.singleton.getHistoryEntryManager();
    }

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
