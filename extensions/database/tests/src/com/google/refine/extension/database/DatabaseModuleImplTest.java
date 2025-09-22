
package com.google.refine.extension.database;

import static org.testng.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.Properties;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DatabaseModuleImplTest {

    private Properties originalProperties;
    private String originalCreateBatchSize;
    private String originalPreviewBatchSize;
    private static final String MOCKED_DEFAULT_CREATE_BATCH_SIZE = "1";
    private static final String MOCKED_DEFAULT_PREVIEW_BATCH_SIZE = "2";

    @BeforeMethod
    public void setUp() throws Exception {
        // Save the original properties
        originalProperties = DatabaseModuleImpl.extensionProperties;

        // Save original default values
        Field createBatchSizeField = DatabaseModuleImpl.class.getDeclaredField("DEFAULT_CREATE_PROJ_BATCH_SIZE");
        createBatchSizeField.setAccessible(true);
        originalCreateBatchSize = (String) createBatchSizeField.get(null);

        Field previewBatchSizeField = DatabaseModuleImpl.class.getDeclaredField("DEFAULT_PREVIEW_BATCH_SIZE");
        previewBatchSizeField.setAccessible(true);
        originalPreviewBatchSize = (String) previewBatchSizeField.get(null);

        // Set mocked default values
        createBatchSizeField.set(null, MOCKED_DEFAULT_CREATE_BATCH_SIZE);
        previewBatchSizeField.set(null, MOCKED_DEFAULT_PREVIEW_BATCH_SIZE);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        // Restore the original properties
        DatabaseModuleImpl.extensionProperties = originalProperties;

        // Restore original default values
        Field createBatchSizeField = DatabaseModuleImpl.class.getDeclaredField("DEFAULT_CREATE_PROJ_BATCH_SIZE");
        createBatchSizeField.setAccessible(true);
        createBatchSizeField.set(null, originalCreateBatchSize);

        Field previewBatchSizeField = DatabaseModuleImpl.class.getDeclaredField("DEFAULT_PREVIEW_BATCH_SIZE");
        previewBatchSizeField.setAccessible(true);
        previewBatchSizeField.set(null, originalPreviewBatchSize);
    }

    @Test
    public void testGetImportCreateBatchSizeAsInt_WithValidValue() throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty("create.batchSize", "500");
        DatabaseModuleImpl.extensionProperties = testProps;

        int result = DatabaseModuleImpl.getImportCreateBatchSize();

        assertEquals(result, 500, "Should return the correct import batch size");
    }

    @Test
    public void testGetImportCreateBatchSizeAsInt_WithInvalidValue() throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty("create.batchSize", "not-a-number");
        DatabaseModuleImpl.extensionProperties = testProps;

        int result = DatabaseModuleImpl.getImportCreateBatchSize();

        int expectedDefault = Integer.parseInt(MOCKED_DEFAULT_CREATE_BATCH_SIZE);
        assertEquals(result, expectedDefault, "Should return default import batch size for invalid input");
    }

    @Test
    public void testGetImportCreateBatchSizeAsInt_WithNullProperties() throws Exception {
        DatabaseModuleImpl.extensionProperties = null;

        int result = DatabaseModuleImpl.getImportCreateBatchSize();

        int expectedDefault = Integer.parseInt(MOCKED_DEFAULT_CREATE_BATCH_SIZE);
        assertEquals(result, expectedDefault, "Should return default import batch size when properties are null");
    }

    @Test
    public void testGetImportPreviewBatchSizeAsInt_WithValidValue() throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty("preview.batchSize", "200");
        DatabaseModuleImpl.extensionProperties = testProps;

        int result = DatabaseModuleImpl.getImportPreviewBatchSize();

        assertEquals(result, 200, "Should return the correct preview batch size");
    }

    @Test
    public void testGetImportPreviewBatchSizeAsInt_WithInvalidValue() throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty("preview.batchSize", "invalid");
        DatabaseModuleImpl.extensionProperties = testProps;

        int result = DatabaseModuleImpl.getImportPreviewBatchSize();

        int expectedDefault = Integer.parseInt(MOCKED_DEFAULT_PREVIEW_BATCH_SIZE);
        assertEquals(result, expectedDefault, "Should return default preview batch size for invalid input");
    }

    @Test
    public void testGetImportPreviewBatchSizeAsInt_WithNullProperties() throws Exception {
        DatabaseModuleImpl.extensionProperties = null;

        int result = DatabaseModuleImpl.getImportPreviewBatchSize();

        int expectedDefault = Integer.parseInt(MOCKED_DEFAULT_PREVIEW_BATCH_SIZE);
        assertEquals(result, expectedDefault, "Should return default preview batch size when properties are null");
    }
}
