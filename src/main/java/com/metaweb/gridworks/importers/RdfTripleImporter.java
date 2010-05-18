package com.metaweb.gridworks.importers;

import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

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

import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
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
        parser.parse(reader, baseUrl); //fills JRDF graph
        
        //first column is subject
        project.columnModel.columns.add(0, new Column(0, "subject"));
        project.columnModel.setKeyColumnIndex(0); //the subject will be the key column
        project.columnModel.update();

        ClosableIterable<Triple> triples = graph.find(ANY_SUBJECT_NODE, ANY_PREDICATE_NODE, ANY_OBJECT_NODE);
        try {
            for (Triple triple : triples) {
                String subject = triple.getSubject().toString();
                String predicate = triple.getPredicate().toString();
                String object = triple.getObject().toString();
                
                //creates new column for every predicate
                int columnIndex = project.columnModel.getColumnIndexByName(predicate);
                if(columnIndex == -1){
                    AddNewColumn(project, predicate, subject);
                }

                //now find row to match with
                int candidateMergeRowIndex = -1;
                for(int i = 0; i < project.rows.size(); i++){
                    //check to see if the subjects are the same (merge if they are)
                    Cell cell = project.rows.get(i).cells.get(0);
                    if(cell != null){
                        if(project.rows.get(i).cells.get(0).value == subject){
                            candidateMergeRowIndex = i;
                        }
                    }
                }
                
                columnIndex = project.columnModel.getColumnIndexByName(predicate);
               
                if(candidateMergeRowIndex > -1){
                    Cell cell = project.rows.get(candidateMergeRowIndex).cells.get(columnIndex);
                    if(cell == null){
                        //empty, so merge in this value
                        MergeWithRow(project, candidateMergeRowIndex, columnIndex, object);
                    }else{
                        //can't overwrite existing, so add new dependent row
                        AddNewDependentRow(project, subject, candidateMergeRowIndex, columnIndex, object); //TODO group to original row.
                    }
                }else{
                    AddNewRow(project, subject, columnIndex, object);
                }
            }

        } finally {
            triples.iterator().close();
        }
    }

    protected void AddNewColumn(Project project, String predicate, String subject){
        int numberOfColumns = project.columnModel.columns.size();

        project.columnModel.columns.add(numberOfColumns, new Column(numberOfColumns, predicate));
        project.columnModel.setMaxCellIndex(numberOfColumns);
        project.columnModel.update();

        //update existing rows with new column
        for(int i = 0; i < project.rows.size(); i++){
            project.rows.get(i).cells.add(numberOfColumns, null);
        }
    }

    protected void MergeWithRow(Project project, int candidateMergeRowIndex, int columnIndex, String object){
        project.rows.get(candidateMergeRowIndex).setCell(columnIndex, new Cell(object, null));
    }

    protected void AddNewDependentRow(Project project, String subject, int candidateMergeRowIndex, int columnIndex, String object){
        Row row = AddNewRow(project, subject, columnIndex, object);
        
        Project.setRowDependency(project, row, columnIndex, candidateMergeRowIndex, project.columnModel.getKeyColumnIndex());
        
        row.cells.set(project.columnModel.getKeyColumnIndex(), null); //the subject can now be null, as the dependencies are set
    }

    protected Row AddNewRow(Project project, String subject, int columnIndex, String object){
        int numberOfColumns = project.columnModel.columns.size();
        
        //add subject
        Row row = new Row(numberOfColumns);
        row.setCell(0, new Cell(subject, null));

        //add object to a row
        row.setCell(columnIndex, new Cell(object, null));
        project.rows.add(row);
        return row;
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
