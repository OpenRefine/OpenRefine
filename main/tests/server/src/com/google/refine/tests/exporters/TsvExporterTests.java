package com.google.refine.tests.exporters;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.browsing.Engine;
import com.google.refine.exporters.CsvExporter;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.tests.RefineTest;

public class TsvExporterTests extends RefineTest {

    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    //dependencies
    StringWriter writer;
    Project project;
    Engine engine;
    Properties options;

    //System Under Test
    CsvExporter SUT;

    @BeforeMethod
    public void SetUp(){
        SUT = new CsvExporter('\t');//new TsvExporter();
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
    public void exportSimpleTsv(){
        CreateGrid(2, 2);

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\n" +
                                               "row0cell0\trow0cell1\n" +
                                               "row1cell0\trow1cell1\n");

    }

    @Test
    public void exportSimpleTsvNoHeader(){
        CreateGrid(2, 2);
        when(options.getProperty("printColumnHeader")).thenReturn("false");
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "row0cell0\trow0cell1\n" +
                                               "row1cell0\trow1cell1\n");

        verify(options,times(2)).getProperty("printColumnHeader");
    }

    @Test
    public void exportTsvWithLineBreaks(){
        CreateGrid(3,3);

        project.rows.get(1).cells.set(1, new Cell("line\n\n\nbreak", null));
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                                               "row0cell0\trow0cell1\trow0cell2\n" +
                                               "row1cell0\t\"line\n\n\nbreak\"\trow1cell2\n" +
                                               "row2cell0\trow2cell1\trow2cell2\n");
    }

    @Test
    public void exportTsvWithComma(){
        CreateGrid(3,3);

        project.rows.get(1).cells.set(1, new Cell("with\t tab", null));
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                                               "row0cell0\trow0cell1\trow0cell2\n" +
                                               "row1cell0\t\"with\t tab\"\trow1cell2\n" +
                                               "row2cell0\trow2cell1\trow2cell2\n");
    }

    @Test
    public void exportTsvWithQuote(){
        CreateGrid(3,3);

        project.rows.get(1).cells.set(1, new Cell("line has \"quote\"", null));
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                                               "row0cell0\trow0cell1\trow0cell2\n" +
                                               "row1cell0\t\"line has \"\"quote\"\"\"\trow1cell2\n" +
                                               "row2cell0\trow2cell1\trow2cell2\n");
    }

    @Test
    public void exportTsvWithEmptyCells(){
        CreateGrid(3,3);

        project.rows.get(1).cells.set(1, null);
        project.rows.get(2).cells.set(0, null);
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                                               "row0cell0\trow0cell1\trow0cell2\n" +
                                               "row1cell0\t\trow1cell2\n" +
                                               "\trow2cell1\trow2cell2\n");
    }

    //helper methods

    protected void CreateColumns(int noOfColumns){
        for(int i = 0; i < noOfColumns; i++){
            try {
                project.columnModel.addColumn(i, new Column(i, "column" + i), true);
                project.columnModel.columns.get(i).getCellIndex();
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

