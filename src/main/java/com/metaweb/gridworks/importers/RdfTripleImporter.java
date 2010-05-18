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
        //System.out.println("--------------------------------");
        //System.out.println("initial number of triples before parsing is : " + graph.getNumberOfTriples());
        LineHandler lineHandler = nTriplesParserFactory.createParser(graph, newMapFactory);
        GraphLineParser parser = new GraphLineParser(graph, lineHandler);
        parser.parse(reader, baseUrl); //fills JRDF graph
        //System.out.println("number of triples parsed is : " + graph.getNumberOfTriples());

        //first column is subject
        project.columnModel.columns.add(0, new Column(0, "subject"));
        project.columnModel.update();

        ClosableIterable<Triple> triples = graph.find(ANY_SUBJECT_NODE, ANY_PREDICATE_NODE, ANY_OBJECT_NODE);
        try {
            for (Triple triple : triples) {

                //System.out.println("Triple    : " + triple);
                String subject = triple.getSubject().toString();
                String predicate = triple.getPredicate().toString();
                String object = triple.getObject().toString();

                //System.out.println("subject   : " + subject);
                //System.out.println("predicate : " + predicate);
                //System.out.println("object    : " + object);
                //System.out.println("predicate relates to column    : " + project.columnModel.getColumnByName(predicate));

                int candidateMergeRowIndex = -1;

                //creates new column for every predicate
                int columnIndex = project.columnModel.getColumnIndexByName(predicate);
                if(columnIndex == -1){
                    candidateMergeRowIndex = AddNewColumn(project, predicate, subject);
                }
                columnIndex = project.columnModel.getColumnIndexByName(predicate);

                if(candidateMergeRowIndex > -1){
                    if(project.rows.get(candidateMergeRowIndex).cells.get(columnIndex) == null){
                        //empty, so merge in this value
                        MergeWithRow(project, candidateMergeRowIndex, columnIndex, object);
                    }else{
                        //can't overwrite existing, so add new row
                        AddNewRow(project, subject, predicate, object); //TODO group to original row.
                    }
                }else{
                    AddNewRow(project, subject, predicate, object);
                }
            }

        } finally {
            triples.iterator().close();
        }
    }

    protected int AddNewColumn(Project project, String predicate, String subject){
        //System.out.println("adding new column");
        int numberOfColumns = project.columnModel.columns.size();

        project.columnModel.columns.add(numberOfColumns, new Column(numberOfColumns, predicate));
        project.columnModel.update();

        int candidateMergeRowIndex = -1;
        //update existing rows with new column
        for(int i = 0; i < project.rows.size(); i++){
            project.rows.get(i).cells.add(numberOfColumns, null);
            if(project.rows.get(i).cells.get(0).value == subject){
                candidateMergeRowIndex = i;
            }
        }

        //numberOfColumns = project.columnModel.columns.size();
        //System.out.println("New total number of columns      : " + numberOfColumns);

        return candidateMergeRowIndex;
    }

    protected void MergeWithRow(Project project, int candidateMergeRowIndex, int columnIndex, String object){
        project.rows.get(candidateMergeRowIndex).setCell(columnIndex, new Cell(object, null));
    }

    protected void AddNewRow(Project project, String subject, String predicate, String object){
        int numberOfColumns = project.columnModel.columns.size();

        //add subject
        Row row = new Row(numberOfColumns);
        row.setCell(0, new Cell(subject, null));

        //add object to a row
        int columnIndex = project.columnModel.getColumnIndexByName(predicate);
        //System.out.println("predicate relates to columnIndex : " + columnIndex);
        row.setCell(columnIndex, new Cell(object, null));
        //System.out.println("Number of cells in new row       : " + row.cells.size());
        project.rows.add(row);
        //System.out.println("New total number of rows         : " + project.rows.size());
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
