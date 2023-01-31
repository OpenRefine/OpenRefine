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

import java.util.HashMap;
import java.util.Map;

import org.openrefine.history.GridPreservation;
import org.openrefine.history.dag.DagSlice;
import org.openrefine.model.Grid;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.Operation;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.wikibase.schema.WikibaseSchema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SaveWikibaseSchemaOperation implements Operation {

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
    public String getDescription() {
        return operationDescription;
    }

    @Override
    public Change createChange() {
        Change change = new WikibaseSchemaChange(_schema);
        return change;
    }

    static public class WikibaseSchemaChange implements Change {

        final protected WikibaseSchema _newSchema;
        protected WikibaseSchema _oldSchema = null;
        public final static String overlayModelKey = "wikibaseSchema";

        public WikibaseSchemaChange(WikibaseSchema newSchema) {
            _newSchema = newSchema;
        }

        @Override
        public ChangeResult apply(Grid projectState, ChangeContext context) throws DoesNotApplyException {
            Map<String, OverlayModel> newModels = new HashMap<>(projectState.getOverlayModels());
            newModels.put(overlayModelKey, _newSchema);
            return new ChangeResult(
                    projectState.withOverlayModels(newModels),
                    GridPreservation.PRESERVES_RECORDS,
                    null);
        }

        @Override
        public boolean isImmediate() {
            return true;
        }

    }

}
