/*

Copyright 2010,2012, Google Inc.
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

package com.google.refine.importers;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectMetadata;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.importing.ImportingJob;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class RdfTripleImporter extends ImportingParserBase {

    private Mode mode;

    public enum Mode {
        RDFXML, NT, N3, TTL, JSONLD
    }

    public RdfTripleImporter() {
        this(Mode.NT);
    }

    public RdfTripleImporter(Mode mode) {
        super(true);
        this.mode = mode;
    }

    @Override
    public void parseOneFile(Project project, ProjectMetadata metadata, ImportingJob job, String fileSource,
            InputStream input, int limit, ObjectNode options, List<Exception> exceptions) {
        // create an empty model
        Model model = ModelFactory.createDefaultModel();

        try {
            switch (mode) {
                case NT:
                    // TODO: The standard lang name is "N-TRIPLE"
                    // we may need to switch if we change packagings
                    model.read(input, null, "NT");
                    break;
                case N3:
                    model.read(input, null, "N3");
                    break;
                case TTL:
                    model.read(input, null, "TTL");
                    break;
                case JSONLD:
                    // TODO: The standard lang name is "JSONLD"
                    model.read(input, null, "JSON-LD");
                    break;
                case RDFXML:
                    model.read(input, null);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown parsing mode");
            }
        } catch (Exception e) {
            exceptions.add(e);
            return;
        }

        StmtIterator triples = model.listStatements();

        try {
            Map<String, List<Row>> subjectToRows = new LinkedHashMap<String, List<Row>>();
            Column subjectColumn = new Column(project.columnModel.allocateNewCellIndex(), "subject");
            project.columnModel.addColumn(0, subjectColumn, false);
            project.columnModel.setKeyColumnIndex(0);

            while (triples.hasNext()) {
                Statement triple = triples.nextStatement();
                String subject = triple.getSubject().toString();
                String predicate = triple.getPredicate().toString();
                String object = triple.getObject().toString();

                Column column = project.columnModel.getColumnByName(predicate);
                if (column == null) {
                    column = new Column(project.columnModel.allocateNewCellIndex(), predicate);
                    project.columnModel.addColumn(-1, column, true);
                }

                int cellIndex = column.getCellIndex();
                if (subjectToRows.containsKey(subject)) {
                    List<Row> rows = subjectToRows.get(subject);
                    for (Row row : rows) {
                        if (!ExpressionUtils.isNonBlankData(row.getCellValue(cellIndex))) {
                            row.setCell(cellIndex, new Cell(object, null));
                            object = null;
                            break;
                        }
                    }

                    if (object != null) {
                        Row row = new Row(project.columnModel.getMaxCellIndex() + 1);
                        rows.add(row);

                        row.setCell(cellIndex, new Cell(object, null));
                    }
                } else {
                    List<Row> rows = new ArrayList<Row>();
                    subjectToRows.put(subject, rows);

                    Row row = new Row(project.columnModel.getMaxCellIndex() + 1);
                    rows.add(row);

                    row.setCell(subjectColumn.getCellIndex(), new Cell(subject, null));
                    row.setCell(cellIndex, new Cell(object, null));
                }
            }

            for (Entry<String, List<Row>> entry : subjectToRows.entrySet()) {
                project.rows.addAll(entry.getValue());
            }
        } catch (ModelException e) {
            exceptions.add(e);
        }
    }
}
