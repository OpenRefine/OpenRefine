package com.google.refine.tests.exporters;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.browsing.Engine;
import com.google.refine.exporters.StreamExporter;
import com.google.refine.exporters.XlsExporter;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.tests.ProjectManagerStub;
import com.google.refine.tests.RefineTest;

public class XlsExporterTests extends RefineTest {

    private static final String TEST_PROJECT_NAME = "xls exporter test project";
    
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    //dependencies
    ByteArrayOutputStream stream;
    ProjectMetadata projectMetadata;
    Project project;
    Engine engine;
    Properties options;

    //System Under Test
    StreamExporter SUT;

    @BeforeMethod
    public void SetUp(){
        SUT = new XlsExporter();
        stream = new ByteArrayOutputStream();
        ProjectManager.singleton = new ProjectManagerStub();
        projectMetadata = new ProjectMetadata();
        project = new Project();
        projectMetadata.setName(TEST_PROJECT_NAME);
        ProjectManager.singleton.registerProject(project, projectMetadata);
        engine = new Engine(project);
        options = mock(Properties.class);
    }

    @AfterMethod
    public void TearDown(){
        SUT = null;
        stream = null;
        ProjectManager.singleton.deleteProject(project.id);
        project = null;
        engine = null;
        options = null;
    }

    @Test
    public void exportSimpleXls(){
        CreateGrid(2, 2);

        try {
            SUT.export(project, options, engine, stream);
        } catch (IOException e) {
            Assert.fail();
        }

        // TODO: Not a very effective test! 
        // (it didn't crash though, and it created output)
        Assert.assertEquals(stream.size(),4096);

    }

    @Test(enabled=false)
    public void exportSimpleXlsNoHeader(){
        CreateGrid(2, 2);
        when(options.getProperty("printColumnHeader")).thenReturn("false");
        try {
            SUT.export(project, options, engine, stream);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(stream.toString(), "row0cell0,row0cell1\n" +
                                               "row1cell0,row1cell1\n");

        verify(options,times(2)).getProperty("printColumnHeader");
    }


    @Test(enabled=false)
    public void exportXlsWithEmptyCells(){
        CreateGrid(3,3);

        project.rows.get(1).cells.set(1, null);
        project.rows.get(2).cells.set(0, null);
        try {
            SUT.export(project, options, engine, stream);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(stream.toString(), "column0,column1,column2\n" +
                                               "row0cell0,row0cell1,row0cell2\n" +
                                               "row1cell0,,row1cell2\n" +
                                               ",row2cell1,row2cell2\n");
    }

    //helper methods

    protected void CreateColumns(int noOfColumns){
        for(int i = 0; i < noOfColumns; i++){
            try {
                project.columnModel.addColumn(i, new Column(i, "column" + i), true);
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
