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

package org.openrefine.importers;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.openrefine.ProjectMetadata;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.importing.ImportingJob;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;

import com.fasterxml.jackson.databind.node.ObjectNode;


public class RdfTripleImporter extends ReaderImporter {
    private Mode mode;
    
    public enum Mode {
        RDFXML,
        NT,
        N3,
        TTL,
        JSONLD
    }

    public RdfTripleImporter(DatamodelRunner runner) {
        this(runner, Mode.NT);
    }
    
    public RdfTripleImporter(DatamodelRunner runner, Mode mode) {
        super(runner);
        this.mode = mode;
    }

	@Override
	public GridState parseOneFile(ProjectMetadata metadata, ImportingJob job, String fileSource, Reader input,
			long limit, ObjectNode options) throws Exception {
        // create an empty model
        Model model = ModelFactory.createDefaultModel();

        switch (mode) {
        case NT:
            model.read(input, null, "NT");
            break;
        case N3:
            model.read(input, null, "N3");
            break;
        case TTL:
            model.read(input, null, "TTL");
            break;
        case JSONLD:
            model.read(input, null, "JSON-LD");
            break;
        case RDFXML:
            model.read(input, null);            
            break;
        default:
            throw new IllegalArgumentException("Unknown parsing mode");    
        }

	    StmtIterator triples = model.listStatements();
	  
	    Map<String, EntityRecord> subjectToRows = new LinkedHashMap<>();
	    ColumnModel columnModel = new ColumnModel(Collections.singletonList(new ColumnMetadata("subject")));
	  
	    while (triples.hasNext()) {
	        Statement triple = triples.nextStatement();
	        String subject = triple.getSubject().toString();
	        String predicate = triple.getPredicate().toString();
	        String object = triple.getObject().toString();
	
	        int cellIndex = columnModel.getColumnIndexByName(predicate);
	        if (cellIndex == -1) {
	       	    cellIndex = columnModel.getColumns().size();
	            columnModel = columnModel.appendUnduplicatedColumn(new ColumnMetadata(predicate));
	        }
	
	        EntityRecord entityRecord = subjectToRows.get(subject);
	        if (entityRecord == null) {
	        	entityRecord = new EntityRecord(subject);
	        	subjectToRows.put(subject, entityRecord);
	        }
	        entityRecord.addValue(predicate, object);
	    }
	
	    List<ColumnMetadata> columns = columnModel.getColumns();
	    List<Row> rows = subjectToRows
			  .entrySet()
			  .stream()
			  .flatMap(entry -> entry.getValue().toRows(columns).stream())
			  .collect(Collectors.toList());
	  
	    return runner.create(columnModel, rows, Collections.emptyMap());
    }
	
	private class EntityRecord {
		protected String subject;
		protected Map<String, List<String>> values;
		protected int maxNbValues = 1;
		
		protected EntityRecord(String subject) {
			this.subject = subject;
			this.values = new LinkedHashMap<>();
		}
		
		protected void addValue(String predicate, String value) {
			List<String> predicateValues = values.get(predicate);
			if (predicateValues == null) {
				predicateValues = new LinkedList<>();
				values.put(predicate, predicateValues);
			}
			predicateValues.add(value);
			maxNbValues = Integer.max(predicateValues.size(), maxNbValues);
		}
		
		protected List<Row> toRows(List<ColumnMetadata> columns) {
			List<Row> rows = new ArrayList<>(maxNbValues);
			for (int i = 0; i != maxNbValues; i++) {
				List<Cell> cells = new ArrayList<>(columns.size());
				for (int j = 0; j != columns.size(); j++) {
					if (j == 0) {
						cells.add(i == 0 ? new Cell(subject, null) : null);
					} else {
						List<String> predicateValues = values.get(columns.get(j).getName());
						String currentValue = null;
						if (predicateValues != null && predicateValues.size() > i) {
							currentValue = predicateValues.get(i);
						}
						cells.add(new Cell(currentValue, null));
					}
				}
				rows.add(new Row(cells));
			}
			return rows;
		}
	}

}
