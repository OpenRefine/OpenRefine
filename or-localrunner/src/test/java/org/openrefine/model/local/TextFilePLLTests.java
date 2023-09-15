
package org.openrefine.model.local;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.util.TestUtils;

public class TextFilePLLTests extends PLLTestsBase {

    File tempDir;

    @BeforeTest
    public void setUp() throws IOException {
        tempDir = TestUtils.createTempDirectory("datamodelrunnertest");
    }

    @AfterTest
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(tempDir);
        tempDir = null;
    }

    @Test
    public void testLoadTextFile() throws IOException {
        File tempFile = new File(tempDir, "textfile.txt");
        createTestTextFile(tempFile, "foo\nbar\nbaz");

        PLL<String> pll = new TextFilePLL(context, tempFile.getAbsolutePath());

        Assert.assertEquals(pll.collect(), Arrays.asList("foo", "bar", "baz"));
        // Iterate a second time
        Assert.assertEquals(pll.collect(), Arrays.asList("foo", "bar", "baz"));
    }

    @Test
    public void testRoundTripSerialization() throws IOException {
        PLL<String> pll = parallelize(2, Arrays.asList("foo", "bar", "baz"));
        File tempFile = new File(tempDir, "roundtrip");
        pll.saveAsTextFile(tempFile.getAbsolutePath());

        PLL<String> deserializedPLL = new TextFilePLL(context, tempFile.getAbsolutePath());

        Assert.assertEquals(pll.collect(), deserializedPLL.collect());
    }

    protected void createTestTextFile(File file, String contents) throws IOException {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file);
            fileWriter.write(contents);
        } finally {
            if (fileWriter != null) {
                fileWriter.close();
            }
        }
    }
}
