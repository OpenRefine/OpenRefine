package com.metaweb.gridworks.importers;

import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

import com.metaweb.gridworks.model.Project;

public interface Importer {
	public boolean takesReader();
	
	public void read(Reader reader, Project project, Properties options, int limit) throws Exception;
	public void read(InputStream inputStream, Project project, Properties options, int limit) throws Exception;
}
