package com.google.gridworks.importers;

import java.net.URL;
import java.util.Properties;

import com.google.gridworks.model.Project;

public interface UrlImporter extends Importer {

    public void read(URL url, Project project, Properties options) throws Exception;

    public boolean canImportData(URL url);

}
