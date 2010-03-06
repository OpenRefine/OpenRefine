package com.metaweb.gridworks.history;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import com.metaweb.gridworks.model.Project;

public interface Change {
    public void apply(Project project);
    public void revert(Project project);
    
    public void save(Writer writer, Properties options) throws IOException;
}
