package com.google.refine.io;

import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.ProjectManager;
import com.google.refine.model.Project;
import com.google.refine.util.Pool;


public class ProjectUtilities {
    final static Logger logger = LoggerFactory.getLogger("project_utilities");

    synchronized public static void save(Project project) {
        synchronized (project) {
            long id = project.id;
            File dir = ((FileProjectManager)ProjectManager.singleton).getProjectDir(id);

            File tempFile = new File(dir, "data.temp.zip");
            try {
                saveToFile(project, tempFile);
            } catch (Exception e) {
                e.printStackTrace();

                logger.warn("Failed to save project {}", id);
                return;
            }

            File file = new File(dir, "data.zip");
            File oldFile = new File(dir, "data.old.zip");

            if (file.exists()) {
                file.renameTo(oldFile);
            }

            tempFile.renameTo(file);
            if (oldFile.exists()) {
                oldFile.delete();
            }

            project.setLastSave();

            logger.info("Saved project '{}'",id);
        }
    }

    protected static void saveToFile(Project project, File file) throws Exception {
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
        try {
            Pool pool = new Pool();

            out.putNextEntry(new ZipEntry("data.txt"));
            try {
                project.saveToOutputStream(out, pool);
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

    static public Project load(File dir, long id) {
        try {
            File file = new File(dir, "data.zip");
            if (file.exists()) {
                return loadFromFile(file, id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            File file = new File(dir, "data.temp.zip");
            if (file.exists()) {
                return loadFromFile(file, id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            File file = new File(dir, "data.old.zip");
            if (file.exists()) {
                return loadFromFile(file, id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    static protected Project loadFromFile(
            File file,
            long id
        ) throws Exception {
            ZipFile zipFile = new ZipFile(file);
            try {
                Pool pool = new Pool();
                ZipEntry poolEntry = zipFile.getEntry("pool.txt");
                if (poolEntry != null) {
                    pool.load(zipFile.getInputStream(poolEntry));
                } // else, it's a legacy project file

                return Project.loadFromInputStream(
                    zipFile.getInputStream(zipFile.getEntry("data.txt")),
                    id,
                    pool
                );
            } finally {
                zipFile.close();
            }
        }
}
