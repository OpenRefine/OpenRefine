/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package org.openrefine.wikibase.operations;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.Properties;

import org.openrefine.wikibase.schema.WikibaseSchema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.Pool;

public class SaveWikibaseSchemaOperation extends AbstractOperation {

    @JsonIgnore
    final public static String operationDescription = "Save Wikibase schema";
    @JsonProperty("schema")
    final protected WikibaseSchema _schema;

    @JsonCreator
    public SaveWikibaseSchemaOperation(
            @JsonProperty("schema") WikibaseSchema schema) {
        this._schema = schema;

    }

    @Override
    protected String getBriefDescription(Project project) {
        return operationDescription;
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID)
            throws Exception {
        String description = operationDescription;

        Change change = new WikibaseSchemaChange(_schema);

        return new HistoryEntry(historyEntryID, project, description, SaveWikibaseSchemaOperation.this, change);
    }

    static public class WikibaseSchemaChange implements Change {

        final protected WikibaseSchema _newSchema;
        protected WikibaseSchema _oldSchema = null;
        public final static String overlayModelKey = "wikibaseSchema";

        public WikibaseSchemaChange(WikibaseSchema newSchema) {
            _newSchema = newSchema;
        }

        public void apply(Project project) {
            synchronized (project) {
                _oldSchema = (WikibaseSchema) project.overlayModels.get(overlayModelKey);
                project.overlayModels.put(overlayModelKey, _newSchema);
            }
        }

        public void revert(Project project) {
            synchronized (project) {
                if (_oldSchema == null) {
                    project.overlayModels.remove(overlayModelKey);
                } else {
                    project.overlayModels.put(overlayModelKey, _oldSchema);
                }
            }
        }

        public void save(Writer writer, Properties options)
                throws IOException {
            writer.write("newSchema=");
            writeWikibaseSchema(_newSchema, writer);
            writer.write('\n');
            writer.write("oldSchema=");
            writeWikibaseSchema(_oldSchema, writer);
            writer.write('\n');
            writer.write("/ec/\n"); // end of change marker
        }

        static public Change load(LineNumberReader reader, Pool pool)
                throws Exception {
            WikibaseSchema oldSchema = null;
            WikibaseSchema newSchema = null;

            String line;
            while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
                int equal = line.indexOf('=');
                CharSequence field = line.subSequence(0, equal);
                String value = line.substring(equal + 1);

                if ("oldSchema".equals(field) && value.length() > 0) {
                    oldSchema = ParsingUtilities.mapper.readValue(value, WikibaseSchema.class);
                } else if ("newSchema".equals(field) && value.length() > 0) {
                    newSchema = ParsingUtilities.mapper.readValue(value, WikibaseSchema.class);
                }
            }

            WikibaseSchemaChange change = new WikibaseSchemaChange(newSchema);
            change._oldSchema = oldSchema;

            return change;
        }

        static protected void writeWikibaseSchema(WikibaseSchema s, Writer writer)
                throws IOException {
            if (s != null) {
                ParsingUtilities.defaultWriter.writeValue(writer, s);
            }
        }
    }
}
