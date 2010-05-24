package com.metaweb.gridworks.tests.exporters;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import static org.mockito.Mockito.mock;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.exporters.CsvExporter;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.ModelException;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class CsvExporterTests {
    //dependencies
    StringWriter writer;
    Project project;
    Engine engine;
    Properties options;

    //System Under Test
    CsvExporter SUT;

    @BeforeMethod
    public void SetUp(){
        SUT = new CsvExporter();
        writer = new StringWriter();
        project = new Project();
        engine = new Engine(project);
        options = mock(Properties.class);
    }

    @AfterMethod
    public void TearDown(){
        SUT = null;
        writer = null;
        project = null;
        engine = null;
        options = null;
    }

    @Test
    public void exportSimpleCsv(){
        CreateGrid(2, 2);

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "\"column0\",\"column1\"\n" +
                                               "\"row0cell0\",\"row0cell1\"\n" +
                                               "\"row1cell0\",\"row1cell1\"\n");

    }
    
    @Test
    public void exportCsvWithLineBreaks(){
        CreateGrid(3,3);
        
        project.rows.get(1).cells.set(1, new Cell("line\n\n\nbreak", null));
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "\"column0\",\"column1\",\"column2\"\n" +
                                               "\"row0cell0\",\"row0cell1\",\"row0cell2\"\n" +
                                               "\"row1cell0\",\"line\n\n\nbreak\",\"row1cell2\"\n" +
                                               "\"row2cell0\",\"row2cell1\",\"row2cell2\"\n");
    }
    
    @Test
    public void exportCsvWithQuote(){
        CreateGrid(3,3);
        
        project.rows.get(1).cells.set(1, new Cell("line has \"quote\"", null));
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "\"column0\",\"column1\",\"column2\"\n" +
                                               "\"row0cell0\",\"row0cell1\",\"row0cell2\"\n" +
                                               "\"row1cell0\",\"line has \"\"quote\"\"\",\"row1cell2\"\n" +
                                               "\"row2cell0\",\"row2cell1\",\"row2cell2\"\n");
    }
    
    @Test
    public void exportCsvWithEmptyCells(){
        CreateGrid(3,3);
        
        project.rows.get(1).cells.set(1, null);
        project.rows.get(2).cells.set(0, null);
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "\"column0\",\"column1\",\"column2\"\n" +
                                               "\"row0cell0\",\"row0cell1\",\"row0cell2\"\n" +
                                               "\"row1cell0\",,\"row1cell2\"\n" +
                                               ",\"row2cell1\",\"row2cell2\"\n");
    }
    
    //helper methods

    protected void CreateColumns(int noOfColumns){
        for(int i = 0; i < noOfColumns; i++){
            try {
                project.columnModel.addColumn(i, new Column(0, "column" + i), true);
            } catch (ModelException e1) {
                Assert.fail("Could not create column");
            }
        }
    }

    protected void CreateGrid(int noOfRows, int noOfColumns){
        CreateColumns(noOfColumns);
        
        for(int i = 0; i < noOfRows; i++){
            Row row = new Row(noOfColumns);
            for(int j = 0; j < noOfColumns; j++){
                row.cells.add(new Cell("row" + i + "cell" + j, null));
            }
            project.rows.add(row);
        }
    }
}
