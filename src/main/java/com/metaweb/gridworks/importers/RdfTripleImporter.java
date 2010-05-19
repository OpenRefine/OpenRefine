package com.metaweb.gridworks.importers;

import java.io.InputStream;
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
import org.jrdf.parser.line.GraphLineParser;
import org.jrdf.parser.line.LineHandler;
import org.jrdf.parser.ntriples.NTriplesParserFactory;
import org.jrdf.util.ClosableIterable;
import static org.jrdf.graph.AnyObjectNode.ANY_OBJECT_NODE;
import static org.jrdf.graph.AnyPredicateNode.ANY_PREDICATE_NODE;
import static org.jrdf.graph.AnySubjectNode.ANY_SUBJECT_NODE;

import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.ModelException;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class RdfTripleImporter implements Importer{
    JRDFFactory JrdfFactory;
    NTriplesParserFactory nTriplesParserFactory;
    MemMapFactory newMapFactory;

    public RdfTripleImporter(){
        JrdfFactory = SortedMemoryJRDFFactory.getFactory();
        nTriplesParserFactory = new NTriplesParserFactory();
        newMapFactory = new MemMapFactory();
    }

    @Override
    public void read(Reader reader, Project project, Properties options) throws Exception {
        String baseUrl = options.getProperty("base-url");
        
        Graph graph = JrdfFactory.getNewGraph();
        LineHandler lineHandler = nTriplesParserFactory.createParser(graph, newMapFactory);
        GraphLineParser parser = new GraphLineParser(graph, lineHandler);
        parser.parse(reader, baseUrl); // fills JRDF graph
        
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
    public void read(InputStream inputStream, Project project, Properties options) throws Exception {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean takesReader() {
        return true;
    }

}
