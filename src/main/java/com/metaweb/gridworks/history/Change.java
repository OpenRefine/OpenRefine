package com.metaweb.gridworks.history;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import com.metaweb.gridworks.model.Project;

/**
 * Interface for a concrete change to a project's data. A change should consist
 * of new values already computed. When apply() is called, the change should not
 * spend any more time computing anything. It should simply save existing values
 * and swap in new values. Similarly, when revert() is called, the change
 * should only swap old values back in.
 */
public interface Change {
    public void apply(Project project);
    public void revert(Project project);
    
    public void save(Writer writer, Properties options) throws IOException;
}
