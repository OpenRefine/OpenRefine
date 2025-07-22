
package com.google.refine.importers;

import java.io.File;
import java.io.Serializable;

import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.model.Project;

public class ParquetImporterTests extends ImporterTest {

    // System Under Test
    ParquetImporter SUT = null;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Override
    @BeforeMethod
    public void setUp() {
        super.setUp();
        SUT = new ParquetImporter();
    }

    @Override
    @AfterMethod
    public void tearDown() {
        SUT = null;
        super.tearDown();
    }

    @Test
    public void readParquet() {

        File file = new File(ClassLoader.getSystemResource("films.parquet").getFile());

        parseOneFile(SUT, file);

        Project expectedProject = createProject(
                new String[] { "Category", "Title", "Director", "Release Date", "Gross", "Rating", "Rank", "Good?" },
                new Serializable[][] {
                        { "Narrative Features", "2 Days In New York", "Julie Delpy", "2012-03-28", 1.0E7, 4.5, 1.0, false },
                        { "Narrative Features", "Booster", null, null, null, null, null, true },
                        { "Narrative Features", "Dark Horse", null, null, null, null, null, null },
                        { "Narrative Features", "Fairhaven", null, null, null, null, null, null }
                });
        assertProjectEquals(project, expectedProject);
    }
}
