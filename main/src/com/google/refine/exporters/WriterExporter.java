
package com.google.refine.exporters;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import com.google.refine.browsing.Engine;
import com.google.refine.model.Project;

public interface WriterExporter extends Exporter {

    public void export(Project project, Properties options, Engine engine, Writer writer) throws IOException;

}
