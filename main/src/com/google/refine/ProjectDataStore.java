package com.google.refine;

import java.io.IOException;
import java.io.InputStream;

import org.apache.tools.tar.TarOutputStream;
import org.json.JSONException;

import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Project;

public abstract class ProjectDataStore {
    protected Project project;
    
    protected ProjectDataStore(Project project) {
        this.project = project;
    }
    
    public abstract boolean save() throws IOException;
    public abstract boolean load();
    public abstract void delete();
    public abstract void exportProject(TarOutputStream tos) throws IOException;
    public abstract void importProject(InputStream inputStream, boolean gziped) throws IOException;
    public abstract void deleteChange(long historyEntryId);
    public abstract void loadChange(HistoryEntry historyEntry);
    public abstract void saveChange(HistoryEntry historyEntry) throws IOException;
    public abstract ProjectMetadata loadMetadata();
    public abstract void saveMetadata(ProjectMetadata metadata) throws JSONException, IOException;
    public abstract ProjectMetadata recoverMetadata();
}
