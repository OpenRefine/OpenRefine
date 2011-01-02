package com.google.refine.commands.importing;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.google.refine.RefineServlet;
import com.google.refine.model.meta.ImportSource;

public class ImportManager {
    static final private Map<String, Class<? extends ImportSource>> nameToImportSourceClass =
        new HashMap<String, Class<? extends ImportSource>>();
    
    static final private Map<String, String> importSourceClassNameToName =
        new HashMap<String, String>();
    
    /**
     * Register a single import source class.
     *
     * @param name importer verb for importer
     * @param importerObject object implementing the importer
     * 
     * @return true if importer was loaded and registered successfully
     */
    static public boolean registerImportSourceClass(String name, Class<? extends ImportSource> klass) {
        if (nameToImportSourceClass.containsKey(name)) {
            return false;
        }
        nameToImportSourceClass.put(name, klass);
        importSourceClassNameToName.put(klass.getName(), name);
        return true;
    }
    
    static public Class<? extends ImportSource> getImportSourceClass(String name) {
        return nameToImportSourceClass.get(name);
    }
    
    static public String getImportSourceClassName(Class<? extends ImportSource> klass) {
        return importSourceClassNameToName.get(klass.getName());
    }
    
    final private RefineServlet servlet;
    final private Map<Long, ImportJob> jobs = new HashMap<Long, ImportJob>();
    private File importDir;
    
    static private ImportManager singleton;
    
    static public void initialize(RefineServlet servlet) {
        singleton = new ImportManager(servlet);
    }
    
    static public ImportManager singleton() {
        return singleton;
    }
    
    private ImportManager(RefineServlet servlet) {
        this.servlet = servlet;
    }
    
    private File getImportDir() {
        if (importDir == null) {
            File tempDir = servlet.getTempDir();
            importDir = tempDir == null ? new File(".import-temp") : new File(tempDir, "import");
            
            if (importDir.exists()) {
                try {
                    // start fresh
                    FileUtils.deleteDirectory(importDir);
                } catch (IOException e) {
                }
            }
            importDir.mkdirs();
        }
        return importDir;
    }
    
    public ImportJob createJob() {
        long id = System.currentTimeMillis() + (long) (Math.random() * 1000000);
        File jobDir = new File(getImportDir(), Long.toString(id));
        
        ImportJob job = new ImportJob(id, jobDir);
        jobs.put(id, job);
        
        return job;
    }
    
    public ImportJob getJob(long id) {
        return jobs.get(id);
    }
    
    public void disposeJob(long id) {
        ImportJob job = getJob(id);
        if (job != null) {
            job.dispose();
            jobs.remove(id);
        }
    }
}
