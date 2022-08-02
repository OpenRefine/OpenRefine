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

package com.google.refine.operations.cell;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.MetaParser;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.changes.CellChange;
import com.google.refine.operations.EngineDependentMassCellOperation;
import com.google.refine.operations.OperationDescription;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.StringUtils;

public class MassEditOperation extends EngineDependentMassCellOperation {

    final protected String _expression;
    final protected List<Edit> _edits;

    static public class Edit {

        @JsonProperty("from")
        final public List<String> from;
        @JsonProperty("fromBlank")
        final public boolean fromBlank;
        @JsonProperty("fromError")
        final public boolean fromError;
        @JsonProperty("to")
        final public Serializable to;

        public Edit(
                List<String> from,
                boolean fromBlank,
                boolean fromError,
                Serializable to) {
            this.from = from;
            this.fromBlank = fromBlank || (from.size() == 1 && from.get(0).length() == 0);
            this.fromError = fromError;
            this.to = to;
        }

        @JsonCreator
        public static Edit deserialize(
                @JsonProperty("from") List<String> from,
                @JsonProperty("fromBlank") boolean fromBlank,
                @JsonProperty("fromError") boolean fromError,
                @JsonProperty("to") Object to,
                @JsonProperty("type") String type) {
            Serializable serializable = (Serializable) to;
            if ("date".equals(type)) {
                serializable = ParsingUtilities.stringToDate((String) to);
            }
            return new Edit(from == null ? new ArrayList<>() : from,
                    fromBlank, fromError, serializable);
        }
    }

    @JsonCreator
    public MassEditOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("expression") String expression,
            @JsonProperty("edits") List<Edit> edits) {
        super(engineConfig, columnName, true);
        _expression = expression;
        _edits = edits;
    }

    @JsonProperty("expression")
    public String getExpression() {
        return _expression;
    }

    @JsonProperty("edits")
    public List<Edit> getEdits() {
        return _edits;
    }

    @Override
    protected String getBriefDescription(Project project) {
        return OperationDescription.cell_mass_edit_brief(_columnName);
    }

    @Override
    protected String createDescription(Column column,
            List<CellChange> cellChanges) {

        return OperationDescription.cell_mass_edit_desc(cellChanges.size(), column.getName());
    }

    @Override
    protected RowVisitor createRowVisitor(Project project, List<CellChange> cellChanges, long historyEntryID) throws Exception {
        Column column = project.columnModel.getColumnByName(_columnName);

        Evaluable eval = MetaParser.parse(_expression);
        Properties bindings = ExpressionUtils.createBindings(project);

        Map<String, Serializable> fromTo = new HashMap<String, Serializable>();
        Serializable fromBlankTo = null;
        Serializable fromErrorTo = null;

        for (Edit edit : _edits) {
            for (String s : edit.from) {
                fromTo.put(s, edit.to);
            }

            // the last edit wins
            if (edit.fromBlank) {
                fromBlankTo = edit.to;
            }
            if (edit.fromError) {
                fromErrorTo = edit.to;
            }
        }

        return new RowVisitor() {

            int cellIndex;
            Properties bindings;
            List<CellChange> cellChanges;
            Evaluable eval;

            Map<String, Serializable> fromTo;
            Serializable fromBlankTo;
            Serializable fromErrorTo;

            public RowVisitor init(
                    int cellIndex,
                    Properties bindings,
                    List<CellChange> cellChanges,
                    Evaluable eval,
                    Map<String, Serializable> fromTo,
                    Serializable fromBlankTo,
                    Serializable fromErrorTo) {
                this.cellIndex = cellIndex;
                this.bindings = bindings;
                this.cellChanges = cellChanges;
                this.eval = eval;
                this.fromTo = fromTo;
                this.fromBlankTo = fromBlankTo;
                this.fromErrorTo = fromErrorTo;
                return this;
            }

            @Override
            public void start(Project project) {
                // nothing to do
            }

            @Override
            public void end(Project project) {
                // nothing to do
            }

            @Override
            public boolean visit(Project project, int rowIndex, Row row) {
                Cell cell = row.getCell(cellIndex);
                Cell newCell = null;

                ExpressionUtils.bind(bindings, row, rowIndex, _columnName, cell);

                Object v = eval.evaluate(bindings);
                if (ExpressionUtils.isError(v)) {
                    if (fromErrorTo != null) {
                        newCell = new Cell(fromErrorTo, (cell != null) ? cell.recon : null);
                    }
                } else if (ExpressionUtils.isNonBlankData(v)) {
                    String from = StringUtils.toString(v);
                    Serializable to = fromTo.get(from);
                    if (to != null) {
                        newCell = new Cell(to, (cell != null) ? cell.recon : null);
                    }
                } else {
                    if (fromBlankTo != null) {
                        newCell = new Cell(fromBlankTo, (cell != null) ? cell.recon : null);
                    }
                }

                if (newCell != null) {
                    CellChange cellChange = new CellChange(rowIndex, cellIndex, cell, newCell);
                    cellChanges.add(cellChange);
                }
                return false;
            }
        }.init(column.getCellIndex(), bindings, cellChanges, eval, fromTo, fromBlankTo, fromErrorTo);
    }
}
