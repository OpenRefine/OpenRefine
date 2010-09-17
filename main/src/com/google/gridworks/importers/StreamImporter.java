package com.google.gridworks.importers;

import java.io.InputStream;
import java.util.Properties;

import com.google.gridworks.ProjectMetadata;
import com.google.gridworks.model.Project;

public interface StreamImporter extends Importer {

    /**
     * @param inputStream stream to be imported
     * @param project project to import stream into
     * @param metadata metadata of new project
     * @param options
     * @throws ImportException
     */
    public void read(InputStream inputStream, Project project,
            ProjectMetadata metadata, Properties options) throws ImportException;

}
