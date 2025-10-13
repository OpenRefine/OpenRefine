
package com.google.refine.extension.database;

import static org.testng.Assert.assertEquals;

import java.util.Properties;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class DatabaseModuleImplTest {

    private Properties originalProperties;

    @BeforeMethod
    public void setUp() throws Exception {
        // Save the original properties
        originalProperties = DatabaseModuleImpl.extensionProperties;
    }

    @AfterMethod
    public void tearDown() throws Exception {
        // Restore the original properties
        DatabaseModuleImpl.extensionProperties = originalProperties;
    }

    @DataProvider(name = "batchSizeTestData")
    public Object[][] provideBatchSizeTestData() {
        return new Object[][] {
                // propertyName, propertyValue, expectedValue, message
                // Valid value tests
                { "create.batchSize", "500", 500, "Should return the correct create batch size" },
                { "preview.batchSize", "200", 200, "Should return the correct preview batch size" },
                // Invalid value tests
                { "create.batchSize", "not-a-number", DatabaseModuleImpl.DEFAULT_CREATE_BATCH_SIZE,
                        "Should return default create batch size for invalid input" },
                { "preview.batchSize", "not-a-number", DatabaseModuleImpl.DEFAULT_PREVIEW_BATCH_SIZE,
                        "Should return default preview batch size for invalid input" },
                // Invalid value tests - Decimal value tests
                { "create.batchSize", "2.5", DatabaseModuleImpl.DEFAULT_CREATE_BATCH_SIZE,
                        "Should return default create batch size when value is decimal" },
                { "preview.batchSize", "2.5", DatabaseModuleImpl.DEFAULT_PREVIEW_BATCH_SIZE,
                        "Should return default preview batch size when value is decimal" },
                // Value less than one tests
                { "create.batchSize", "-5", DatabaseModuleImpl.DEFAULT_CREATE_BATCH_SIZE,
                        "Should return default create batch size when value is less than 1" },
                { "preview.batchSize", "-5", DatabaseModuleImpl.DEFAULT_PREVIEW_BATCH_SIZE,
                        "Should return default preview batch size when value is less than 1" },
        };
    }

    @Test(dataProvider = "batchSizeTestData")
    public void testBatchSizes(String propertyName, String propertyValue,
            int expectedValue, String message) throws Exception {
        Properties testProps = new Properties();
        testProps.setProperty(propertyName, propertyValue);
        DatabaseModuleImpl.extensionProperties = testProps;

        int result;
        if ("create.batchSize".equals(propertyName)) {
            result = DatabaseModuleImpl.getCreateBatchSize();
        } else {
            result = DatabaseModuleImpl.getPreviewBatchSize();
        }

        assertEquals(result, expectedValue, message);
    }

    @DataProvider(name = "nullPropertiesTestData")
    public Object[][] provideNullPropertiesTestData() {
        return new Object[][] {
                // propertyName, expectedValue, message
                { "create.batchSize", DatabaseModuleImpl.DEFAULT_CREATE_BATCH_SIZE,
                        "Should return default create batch size when properties are null" },
                { "preview.batchSize", DatabaseModuleImpl.DEFAULT_PREVIEW_BATCH_SIZE,
                        "Should return default preview batch size when properties are null" }
        };
    }

    @Test(dataProvider = "nullPropertiesTestData")
    public void testBatchSizesWithNullProperties(String propertyName, int expectedValue, String message) throws Exception {
        DatabaseModuleImpl.extensionProperties = null;

        int result;
        if ("create.batchSize".equals(propertyName)) {
            result = DatabaseModuleImpl.getCreateBatchSize();
        } else {
            result = DatabaseModuleImpl.getPreviewBatchSize();
        }

        assertEquals(result, expectedValue, message);
    }
}
