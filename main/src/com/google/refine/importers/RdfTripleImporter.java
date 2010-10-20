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

package com.google.refine.importers;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.jrdf.JRDFFactory;
import org.jrdf.SortedMemoryJRDFFactory;
import org.jrdf.collection.MemMapFactory;
import org.jrdf.graph.Graph;
import org.jrdf.graph.Triple;
import org.jrdf.parser.ParseException;
import org.jrdf.parser.StatementHandlerException;
import org.jrdf.parser.line.GraphLineParser;
import org.jrdf.parser.line.LineHandler;
import org.jrdf.parser.ntriples.NTriplesParserFactory;
import org.jrdf.util.ClosableIterable;
import static org.jrdf.graph.AnyObjectNode.ANY_OBJECT_NODE;
import static org.jrdf.graph.AnyPredicateNode.ANY_PREDICATE_NODE;
import static org.jrdf.graph.AnySubjectNode.ANY_SUBJECT_NODE;

import com.google.refine.ProjectMetadata;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class RdfTripleImporter implements ReaderImporter{
    private JRDFFactory             _jrdfFactory;
    private NTriplesParserFactory   _nTriplesParserFactory;
    private MemMapFactory           _newMapFactory;

    public RdfTripleImporter(){
        _jrdfFactory = SortedMemoryJRDFFactory.getFactory();
        _nTriplesParserFactory = new NTriplesParserFactory();
        _newMapFactory = new MemMapFactory();
    }

    @Override
    public void read(Reader reader, Project project, ProjectMetadata metadata, Properties options) throws ImportException {
        String baseUrl = options.getProperty("base-url");

        Graph graph = _jrdfFactory.getNewGraph();
        LineHandler lineHandler = _nTriplesParserFactory.createParser(graph, _newMapFactory);
        GraphLineParser parser = new GraphLineParser(graph, lineHandler);
        try {
            parser.parse(reader, baseUrl); // fills JRDF graph
        } catch (IOException e) {
            throw new ImportException("i/o error while parsing RDF",e);
        } catch (ParseException e) {
            throw new ImportException("error parsing RDF",e);
        } catch (StatementHandlerException e) {
            throw new ImportException("error parsing RDF",e);
        } 

        Map<String, List<Row>> subjectToRows = new HashMap<String, List<Row>>();

        Column subjectColumn = new Column(0, "subject");
        project.columnModel.columns.add(0, subjectColumn);
        project.columnModel.setKeyColumnIndex(0);

        ClosableIterable<Triple> triples = graph.find(ANY_SUBJECT_NODE, ANY_PREDICATE_NODE, ANY_OBJECT_NODE);
        try {
            for (Triple triple : triples) {
                String subject = triple.getSubject().toString();
                String predicate = triple.getPredicate().toString();
                String object = triple.getObject().toString();

                Column column = project.columnModel.getColumnByName(predicate);
                if (column == null) {
                    column = new Column(project.columnModel.allocateNewCellIndex(), predicate);
                    try {
                        project.columnModel.addColumn(-1, column, true);
                    } catch (ModelException e) {
                        // ignore
                    }
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
        } finally {
            triples.iterator().close();
        }
    }

    
    @Override
    public boolean canImportData(String contentType, String fileName) {
        if (contentType != null) {
            contentType = contentType.toLowerCase().trim();

            if("application/rdf+xml".equals(contentType)) {
                return true;
            }
        } else if (fileName != null) {
            fileName = fileName.toLowerCase();
            if (
                    fileName.endsWith(".rdf")) {
                return true;
            }
        }
        return false;
    }

}
