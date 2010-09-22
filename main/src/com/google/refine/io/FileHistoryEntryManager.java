package com.google.refine.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.io.Writer;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.ProjectManager;
import com.google.refine.history.History;
import com.google.refine.history.HistoryEntry;
import com.google.refine.history.HistoryEntryManager;
import com.google.refine.util.Pool;


public class FileHistoryEntryManager implements HistoryEntryManager{

    public void delete(HistoryEntry historyEntry) {
        File file = getChangeFile(historyEntry);
        if (file.exists()) {
            file.delete();
        }
    }

    public void save(HistoryEntry historyEntry, Writer writer, Properties options) {
        JSONWriter jsonWriter = new JSONWriter(writer);
        try {
            historyEntry.write(jsonWriter, options);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

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
                pool.load(new InputStreamReader(
                    zipFile.getInputStream(poolEntry)));
            } // else, it's a legacy project file

            historyEntry.setChange(History.readOneChange(
                    zipFile.getInputStream(zipFile.getEntry("change.txt")), pool));
        } finally {
            zipFile.close();
        }
    }

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
        File dir = new File(((FileProjectManager)ProjectManager.singleton)
                .getProjectDir(historyEntry.projectID),
                "history");
        dir.mkdirs();

        return dir;
    }
}