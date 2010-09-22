package com.google.refine.importers;


public interface Importer {

    /**
     * Determine whether importer can handle given contentType and filename.
     * 
     * @param contentType
     * @param fileName
     * @return true if the importer can handle this
     */
    public boolean canImportData(String contentType, String fileName);
}
