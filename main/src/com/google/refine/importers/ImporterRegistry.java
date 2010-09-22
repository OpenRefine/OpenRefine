package com.google.refine.importers;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


abstract public class ImporterRegistry {
    final static Logger logger = LoggerFactory.getLogger("importer-registry");

    static final private Map<String, Importer> importers = new HashMap<String, Importer>();

    private static final String[][] importerNames = {
        {"ExcelImporter", "com.google.refine.importers.ExcelImporter"},
        {"XmlImporter", "com.google.refine.importers.XmlImporter"},
        {"RdfTripleImporter", "com.google.refine.importers.RdfTripleImporter"},
        {"MarcImporter", "com.google.refine.importers.MarcImporter"},
        {"TsvCsvImporter", "com.google.refine.importers.TsvCsvImporter"}
    };

    static {
        registerImporters(importerNames);
    }

    static public boolean registerImporters(String[][] importers) {
        boolean status = true;
        for (String[] importer : importerNames) {
            String importerName = importer[0];
            String className = importer[1];
            logger.debug("Loading command " + importerName + " class: " + className);
            Importer cmd;
            try {
                // TODO: May need to use the servlet container's class loader here
                cmd = (Importer) Class.forName(className).newInstance();
            } catch (InstantiationException e) {
                logger.error("Failed to load importer class " + className, e);
                status = false;
                continue;
            } catch (IllegalAccessException e) {
                logger.error("Failed to load importer class " + className, e);
                status = false;
                continue;
            } catch (ClassNotFoundException e) {
                logger.error("Failed to load importer class " + className, e);
                status = false;
                continue;
            }
            status |= registerImporter(importerName, cmd);
        }
        return status;
    }

    /**
     * Register a single importer.
     *
     * @param name importer verb for importer
     * @param importerObject object implementing the importer
     * 
     * @return true if importer was loaded and registered successfully
     */
    static public boolean registerImporter(String name, Importer importerObject) {
        if (importers.containsKey(name)) {
            return false;
        }
        importers.put(name, importerObject);
        return true;
    }

    // Currently only for test purposes
    static protected boolean unregisterImporter(String verb) {
        return importers.remove(verb) != null;
    }
    
    static public Importer guessImporter(String contentType, String fileName, boolean provideDefault) {
        for (Importer i : importers.values()){
            if(i.canImportData(contentType, fileName)){
                return i;
            }
        }
        if (provideDefault) {
            return new TsvCsvImporter(); // default
        } else {
            return null;
        }
    }
    
    static public Importer guessImporter(String contentType, String filename) {
        return guessImporter(contentType, filename, true);
    }

    static public Importer guessUrlImporter(URL url) {
        for (Importer importer : importers.values()){
            if (importer instanceof UrlImporter 
                    && ((UrlImporter) importer).canImportData(url)) {
                return importer;
            }
        }
        return null;
    }
}
