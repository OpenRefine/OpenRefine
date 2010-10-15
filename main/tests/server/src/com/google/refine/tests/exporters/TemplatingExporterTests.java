package com.google.refine.tests.exporters;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
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
import com.google.refine.exporters.TemplatingExporter;
import com.google.refine.exporters.WriterExporter;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.tests.ProjectManagerStub;
import com.google.refine.tests.RefineTest;

public class TemplatingExporterTests extends RefineTest {

    private static final String TEST_PROJECT_NAME = "templating exporter test project";

    String rowPrefix = "boilerplate";
    String cellSeparator = "spacer";
    String prefix = "test prefix>";
    String suffix = "<test suffix";
    String rowSeparator = "\n";

    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    //dependencies
    StringWriter writer;
    ProjectMetadata projectMetadata;
    Project project;
    Engine engine;
    Properties options;

    //System Under Test
    WriterExporter SUT;

    @BeforeMethod
    public void SetUp(){
        SUT = new TemplatingExporter();
        writer = new StringWriter();
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
        writer = null;
        ProjectManager.singleton.deleteProject(project.id);
        project = null;
        engine = null;
        options = null;
    }
    @Test
    public void exportEmptyTemplate(){

//        when(options.getProperty("limit")).thenReturn("100"); // optional integer
//        when(options.getProperty("sorting")).thenReturn(""); //optional
        when(options.getProperty("template")).thenReturn("a template that should never get used");
        when(options.getProperty("prefix")).thenReturn(prefix);
        when(options.getProperty("suffix")).thenReturn(suffix);
        when(options.getProperty("separator")).thenReturn(rowSeparator);
//        when(options.getProperty("preview")).thenReturn("false"); // optional true|false

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), prefix + suffix);
    }
    
    @Test
    public void exportSimpleTemplate(){
        CreateGrid(2, 2);
        String template = rowPrefix + "${column0}" + cellSeparator + "${column1}";
//      String template = "boilerplate${column0}{{4+3}}${column1}";

        
//        when(options.getProperty("limit")).thenReturn("100"); // optional integer
//        when(options.getProperty("sorting")).thenReturn(""); //optional
        when(options.getProperty("template")).thenReturn(template);
        when(options.getProperty("prefix")).thenReturn(prefix);
        when(options.getProperty("suffix")).thenReturn(suffix);
        when(options.getProperty("separator")).thenReturn(rowSeparator);
//        when(options.getProperty("preview")).thenReturn("false"); // optional true|false

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), 
                prefix 
                + rowPrefix + "row0cell0" + cellSeparator + "row0cell1" + rowSeparator
                + rowPrefix + "row1cell0" + cellSeparator + "row1cell1" 
                + suffix);
    }


    @Test()
    public void exportTemplateWithEmptyCells(){
        
//      when(options.getProperty("limit")).thenReturn("100"); // optional integer
//      when(options.getProperty("sorting")).thenReturn(""); //optional
      when(options.getProperty("template")).thenReturn(rowPrefix + "${column0}" + cellSeparator + "${column1}" + cellSeparator + "${column2}");
      when(options.getProperty("prefix")).thenReturn(prefix);
      when(options.getProperty("suffix")).thenReturn(suffix);
      when(options.getProperty("separator")).thenReturn(rowSeparator);
//      when(options.getProperty("preview")).thenReturn("false"); // optional true|false

        CreateGrid(3,3);

        project.rows.get(1).cells.set(1, null);
        project.rows.get(2).cells.set(0, null);
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        // TODO: Template exporter returns null for empty cells, which doesn't
        // doesn't seem to be desirable behavior.
        Assert.assertEquals(writer.toString(), 
                prefix 
                + rowPrefix + "row0cell0" + cellSeparator + "row0cell1" + cellSeparator + "row0cell2" + rowSeparator
                + rowPrefix + "row1cell0" + cellSeparator               + cellSeparator + "row1cell2" + rowSeparator 
                + rowPrefix +               cellSeparator + "row2cell1" + cellSeparator + "row2cell2" 
                + suffix);

    }

    @Test()
    public void exportTemplateWithLimit(){
        
      when(options.getProperty("limit")).thenReturn("2"); // optional integer
//      when(options.getProperty("sorting")).thenReturn(""); //optional
      when(options.getProperty("template")).thenReturn(rowPrefix + "${column0}" + cellSeparator + "${column1}" + cellSeparator + "${column2}");
      when(options.getProperty("prefix")).thenReturn(prefix);
      when(options.getProperty("suffix")).thenReturn(suffix);
      when(options.getProperty("separator")).thenReturn(rowSeparator);
//      when(options.getProperty("preview")).thenReturn("false"); // optional true|false

        CreateGrid(3,3);

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), 
                prefix 
                + rowPrefix + "row0cell0" + cellSeparator + "row0cell1" + cellSeparator + "row0cell2" + rowSeparator
                + rowPrefix + "row1cell0" + cellSeparator + "row1cell1" + cellSeparator + "row1cell2"  
                // third row should be skipped because of limit
                + suffix);

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
