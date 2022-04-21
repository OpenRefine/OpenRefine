
package com.google.refine.io;

import com.google.refine.ProjectMetadata;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
    public void testSaveAndLoadFileDefault() {
        try {
            ProjectMetadataUtilities.saveToFile(actualMetadata, file);
            expectedMetadata = ProjectMetadataUtilities.loadFromFile(file);

            Assert.assertNotEquals(expectedMetadata.getName(), actualMetadata.getName());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            file.delete();
        }
    }

    @Test
    public void testSaveAndLoadFileWithCharset() {
        try {
            ProjectMetadataUtilities.saveToFile(actualMetadata, file);
            expectedMetadata = ProjectMetadataUtilities.loadFromFile(file, StandardCharsets.UTF_8);

            Assert.assertEquals(expectedMetadata.getName(), actualMetadata.getName());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            file.delete();
        }
    }
}
