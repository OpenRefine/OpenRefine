package com.google.refine.history;

import java.io.Writer;
import java.util.Properties;

import com.google.refine.model.Project;

public class MissingExtensionChange implements Change {
    
    private final String missingClassName;

    public MissingExtensionChange(String missingClassName) {
        this.missingClassName = missingClassName;
    }

    @Override
    public void apply(Project project) {
        throw new UnsupportedOperationException("Cannot apply this change. The extension providing it (" + missingClassName + ") is missing.");
    }

    @Override
    public void revert(Project project) {
        throw new UnsupportedOperationException("Cannot revert this change. The extension providing it (" + missingClassName + ") is missing.");
    }

    @Override
    public void save(Writer writer, Properties options) {
        throw new UnsupportedOperationException("Cannot save this change. The extension providing it (" + missingClassName + ") is missing.");
    }
}