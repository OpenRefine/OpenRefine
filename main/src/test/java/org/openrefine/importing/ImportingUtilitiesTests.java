/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package org.openrefine.importing;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openrefine.ProjectMetadata;
import org.openrefine.importers.ImporterTest;
import org.openrefine.importers.ImporterUtilities;
import org.openrefine.model.GridState;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ImportingUtilitiesTests extends ImporterTest {

    @Override
    @BeforeMethod
    public void setUp(){
        super.setUp();
    }
    
    @Test
    public void createProjectMetadataTest()
            throws Exception {
        ObjectNode optionObj = ParsingUtilities.evaluateJsonStringToObjectNode(
                "{\"projectName\":\"acme\",\"projectTags\":[],\"created\":\"2017-12-18T13:28:40.659\",\"modified\":\"2017-12-20T09:28:06.654\",\"creator\":\"\",\"contributors\":\"\",\"subject\":\"\",\"description\":\"\",\"rowCount\":50,\"customMetadata\":{}}");
        ProjectMetadata pm = ImportingUtilities.createProjectMetadata(optionObj);
        Assert.assertEquals(pm.getName(), "acme");
        Assert.assertEquals(pm.getEncoding(), "UTF-8");
        Assert.assertTrue(pm.getTags().length == 0);
    }
    
    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testZipSlip() throws IOException {
    	File tempDir = TestUtils.createTempDirectory("openrefine-zip-slip-test");
        // For CVE-2018-19859, issue #1840
    	ImportingUtilities.allocateFile(tempDir, "../../tmp/script.sh");
    }
    
    @Test
    public void testMostCommonFormatEmpty() {
    	Assert.assertNull(ImporterUtilities.mostCommonFormat(Collections.emptyList()));
    }
    
    @Test
    public void testMostCommonFormat() {
    	ImportingFileRecord recA = mock(ImportingFileRecord.class);
    	when(recA.getFormat()).thenReturn("foo");
    	ImportingFileRecord recB = mock(ImportingFileRecord.class);
    	when(recB.getFormat()).thenReturn("bar");
    	ImportingFileRecord recC = mock(ImportingFileRecord.class);
    	when(recC.getFormat()).thenReturn("foo");
    	ImportingFileRecord recD = mock(ImportingFileRecord.class);
    	when(recD.getFormat()).thenReturn(null);
    	List<ImportingFileRecord> records = Arrays.asList(recA, recB, recC, recD);
    	
    	Assert.assertEquals(ImporterUtilities.mostCommonFormat(records), "foo");
    }
    
    @Test
    public void testExtractFilenameFromSparkURI() {
    	Assert.assertEquals(ImportingUtilities.extractFilenameFromSparkURI("hdfs:///data/records"), "records");
    	Assert.assertNull(ImportingUtilities.extractFilenameFromSparkURI("////"));
    }

}
