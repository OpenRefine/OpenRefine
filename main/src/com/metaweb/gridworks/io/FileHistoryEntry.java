package com.metaweb.gridworks.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.io.Writer;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.ProjectManager;
import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.history.History;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.util.Pool;


public class FileHistoryEntry extends HistoryEntry{
    protected FileHistoryEntry(long id, long projectID, String description, AbstractOperation operation, Date time) {
        super(id, projectID, description, operation, time);
    }
    protected FileHistoryEntry(long id, Project project, String description, AbstractOperation operation, Change change){
        super(id, project, description, operation, change);
    }

    protected void delete() {
        File file = getChangeFile();
        if (file.exists()) {
            file.delete();
        }
    }

    public void save(Writer writer, Properties options) {
        JSONWriter jsonWriter = new JSONWriter(writer);
        try {
            write(jsonWriter, options);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void loadChange() {
        File changeFile = getChangeFile();

        try {
            loadChange(changeFile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load change file " + changeFile.getAbsolutePath(), e);
        }
    }

    protected void loadChange(File file) throws Exception {
        ZipFile zipFile = new ZipFile(file);
        try {
            Pool pool = new Pool();
            ZipEntry poolEntry = zipFile.getEntry("pool.txt");
            if (poolEntry != null) {
                pool.load(new InputStreamReader(
                    zipFile.getInputStream(poolEntry)));
            } // else, it's a legacy project file

            _change = History.readOneChange(
                    zipFile.getInputStream(zipFile.getEntry("change.txt")), pool);
        } finally {
            zipFile.close();
        }
    }

    protected void saveChange() throws Exception {
        File changeFile = getChangeFile();
        if (!(changeFile.exists())) {
            saveChange(changeFile);
        }
    }

    protected void saveChange(File file) throws Exception {
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
        try {
            Pool pool = new Pool();

            out.putNextEntry(new ZipEntry("change.txt"));
            try {
                History.writeOneChange(out, _change, pool);
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

    protected File getChangeFile() {
        return new File(getHistoryDir(), id + ".change.zip");
    }

    protected File getHistoryDir() {//FIXME relies on FileProjectManager
        File dir = new File(((FileProjectManager)ProjectManager.singleton).getProjectDir(projectID), "history");
        dir.mkdirs();

        return dir;
    }
}
