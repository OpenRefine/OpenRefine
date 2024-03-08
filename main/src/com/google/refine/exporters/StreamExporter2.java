package com.google.refine.exporters;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import com.google.refine.browsing.Engine;
import com.google.refine.model.Project;

public interface StreamExporter2 extends StreamExporter{

    public void export(Project project, Map<String,String> options, Engine engine, Writer writer) throws IOException;
}
