
package com.google.refine.io;

import com.google.refine.ProjectMetadata;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class ProjectMetadataUtilitiesTest {

    ProjectMetadata actualMetadata = new ProjectMetadata();
    ProjectMetadata expectedMetadata = new ProjectMetadata();
    File file = null;

    @Before
    public void setUp() {
        actualMetadata.setName("Velmi dobře, použijeme vícejazyčný neurální stroj, abychom to vyřešili. ÜÜÖÖÄÄ");

        try {
            file = File.createTempFile("example_project_metadata_charset_test", ".json");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSaveAndLoadFile() {
        try {
            ProjectMetadataUtilities.saveToFile(actualMetadata, file);
            expectedMetadata = ProjectMetadataUtilities.loadFromFile(file);

            Assert.assertEquals(expectedMetadata.getName(), actualMetadata.getName());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            file.delete();
        }
    }

}
