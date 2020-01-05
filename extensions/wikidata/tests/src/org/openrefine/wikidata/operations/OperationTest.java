/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package org.openrefine.wikidata.operations;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.history.Change;
import org.openrefine.model.AbstractOperation;
import org.openrefine.model.Project;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.openrefine.wikidata.testing.WikidataRefineTest;

public abstract class OperationTest extends WikidataRefineTest {

    protected Project project = null;

    @BeforeMethod
    public void setUp() {
        project = createCSVProject("a,b\nc,d");
    }

    protected void registerOperation(String name, Class klass) {
        OperationRegistry.registerOperation("wikidata", name, klass);
    }

    public abstract AbstractOperation reconstruct()
            throws Exception;

    public abstract String getJson()
            throws Exception;

    @Test
    public void testReconstruct()
            throws Exception {
        String json = getJson();
        AbstractOperation op = reconstruct();
        StringWriter writer = new StringWriter();
        ParsingUtilities.defaultWriter.writeValue(writer, op);
        TestUtils.assertEqualAsJson(json, writer.toString());
    }

    protected LineNumberReader makeReader(String input) {
        StringReader reader = new StringReader(input);
        return new LineNumberReader(reader);
    }

    protected String saveChange(Change change)
            throws IOException {
        StringWriter writer = new StringWriter();
        change.save(writer, new Properties());
        return writer.toString();
    }

}
