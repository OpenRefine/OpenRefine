package com.google.refine.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.ProjectMetadata;


public class ProjectMetadataUtilities {
    final static Logger logger = LoggerFactory.getLogger("project_metadata_utilities");

    public static void save(ProjectMetadata projectMeta, File projectDir) throws Exception {
        File tempFile = new File(projectDir, "metadata.temp.json");
        try {
            saveToFile(projectMeta, tempFile);
        } catch (Exception e) {
            e.printStackTrace();

            logger.warn("Failed to save project metadata");
            return;
        }

        File file = new File(projectDir, "metadata.json");
        File oldFile = new File(projectDir, "metadata.old.json");

        if (file.exists()) {
            file.renameTo(oldFile);
        }

        tempFile.renameTo(file);
        if (oldFile.exists()) {
            oldFile.delete();
        }
    }

    protected static void saveToFile(ProjectMetadata projectMeta, File metadataFile) throws Exception {
        Writer writer = new OutputStreamWriter(new FileOutputStream(metadataFile));
        try {
            JSONWriter jsonWriter = new JSONWriter(writer);
            projectMeta.write(jsonWriter);
        } finally {
            writer.close();
        }
    }

    static public ProjectMetadata load(File projectDir) {
        try {
            return loadFromFile(new File(projectDir, "metadata.json"));
        } catch (Exception e) {
        }

        try {
            return loadFromFile(new File(projectDir, "metadata.temp.json"));
        } catch (Exception e) {
        }

        try {
            return loadFromFile(new File(projectDir, "metadata.old.json"));
        } catch (Exception e) {
        }

        return null;
    }

    static protected ProjectMetadata loadFromFile(File metadataFile) throws Exception {
        FileReader reader = new FileReader(metadataFile);
        try {
            JSONTokener tokener = new JSONTokener(reader);
            JSONObject obj = (JSONObject) tokener.nextValue();

            return ProjectMetadata.loadFromJSON(obj);
        } finally {
            reader.close();
        }
    }
}
