package com.google.refine.tests.model.changes;

import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.changes.DataExtensionChange;
import com.google.refine.model.changes.MassChange;

import static org.junit.Assert.assertEquals;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;

import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.history.Change;
import com.google.refine.tests.RefineTest;
import com.google.refine.util.Pool;


public class DataExtensionChangeTest extends RefineTest {

    Project project;
    
    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void SetUp()
            throws IOException, ModelException {
        project = createCSVProject(
                "reconciled\n"+
                "some item");
    }
    
    @Test
    public void testApplyOldChange() throws Exception {
        Pool pool = new Pool();
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream("changes/data_extension_2.8.txt");
        LineNumberReader lineReader = new LineNumberReader(new InputStreamReader(in));
        // skip the header
        lineReader.readLine();
        lineReader.readLine();
        Change change = DataExtensionChange.load(lineReader, pool);
        change.apply(project);
        assertEquals("Wikimedia content project", project.rows.get(0).getCell(1).value);
    }
    
    @Test
    public void testApplyNewChange() throws Exception {
        Pool pool = new Pool();
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream("changes/data_extension_3.0.txt");
        LineNumberReader lineReader = new LineNumberReader(new InputStreamReader(in));
        // skip the header
        lineReader.readLine();
        lineReader.readLine();
        Change change = DataExtensionChange.load(lineReader, pool);
        change.apply(project);
        assertEquals("Wikimedia content project", project.rows.get(0).getCell(1).value);
    }
}
