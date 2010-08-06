package com.google.gridworks.importers;

import java.io.InputStream;
import java.util.Properties;

import com.google.gridworks.model.Project;

public interface StreamImporter extends Importer {

    /**
     * @param inputStream stream to be imported
     * @param project project to import stream into
     * @param options
     * @throws ImportException
     */
    public void read(InputStream inputStream, Project project,
            Properties options) throws ImportException;

}
