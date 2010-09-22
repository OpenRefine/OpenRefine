package com.google.refine.importers;

import java.io.Reader;
import java.util.Properties;

import com.google.refine.ProjectMetadata;
import com.google.refine.model.Project;

/**
 * Interface for importers which take a Reader as input.
 */
public interface ReaderImporter extends Importer {

    /**
     * Read data from a input reader into project.
     * 
     * @param reader
     *            reader to import data from. It is assumed to be positioned at
     *            the correct point and ready to go.
     * @param project
     *            project which will contain data
     * @param metadata
     *            metadata of new project
     * @param options
     *            set of properties with import options
     * @throws ImportException
     */
    public void read(Reader reader, Project project, ProjectMetadata metadata, Properties options)
            throws ImportException;
}
