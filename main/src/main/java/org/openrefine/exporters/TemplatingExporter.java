/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.openrefine.exporters;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import org.openrefine.ProjectMetadata;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.Engine.Mode;
import org.openrefine.expr.ParsingException;
import org.openrefine.model.GridState;
import org.openrefine.sorting.SortingConfig;
import org.openrefine.templating.Parser;
import org.openrefine.templating.Template;
import org.openrefine.util.ParsingUtilities;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TemplatingExporter implements WriterExporter {

    @Override
    public String getContentType() {
        return "application/x-unknown";
    }
    
    protected static class TemplateConfig {
        @JsonProperty("template")
        protected String template;
        @JsonProperty("prefix")
        protected String prefix;
        @JsonProperty("suffix")
        protected String suffix;
        @JsonProperty("separator")
        protected String separator;
        
        protected TemplateConfig(
                String template, String prefix,
                String suffix, String separator) {
            this.template = template;
            this.prefix = prefix;
            this.suffix = suffix;
            this.separator = separator;
        }
    }

    @Override
    public void export(GridState grid, ProjectMetadata projectMetadata, Properties options, Engine engine, Writer writer) throws IOException {
        String limitString = options.getProperty("limit");
        int limit = limitString != null ? Integer.parseInt(limitString) : -1;
        
        String sortingJson = options.getProperty("sorting");
        
        String templateString = options.getProperty("template");
        String prefixString = options.getProperty("prefix");
        String suffixString = options.getProperty("suffix");
        String separatorString = options.getProperty("separator");
        
        Template template;
        try {
            template = Parser.parse(templateString);
        } catch (ParsingException e) {
            throw new IOException("Missing or bad template", e);
        }
        
        template.setPrefix(prefixString);
        template.setSuffix(suffixString);
        template.setSeparator(separatorString);
        
        if (!"true".equals(options.getProperty("preview"))) {
            TemplateConfig config = new TemplateConfig(templateString, prefixString,
                    suffixString, separatorString);
            projectMetadata.getPreferenceStore().put("exporters.templating.template",
                    ParsingUtilities.defaultWriter.writeValueAsString(config));
        }
        
        SortingConfig sorting = SortingConfig.NO_SORTING;
        if (sortingJson != null) {
            sorting = SortingConfig.reconstruct(sortingJson);
        }
        
        if (engine.getMode() == Mode.RowBased) {
            template.writeRows(engine.getMatchingRows(sorting), writer, grid.getColumnModel(), limit);
        } else {
            template.writeRecords(engine.getMatchingRecords(sorting), writer, grid.getColumnModel(), limit);
        }
    }
    
}
