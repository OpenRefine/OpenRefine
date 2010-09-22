package com.google.refine.importers;

import java.net.URL;
import java.util.Properties;

import com.google.refine.ProjectMetadata;
import com.google.refine.model.Project;

public interface UrlImporter extends Importer {

    public void read(URL url, Project project, ProjectMetadata metadata, Properties options) throws Exception;

    public boolean canImportData(URL url);

}
