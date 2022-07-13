/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.operations.cell;

import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class SplitMultiValuedCellsTests extends RefineTest {

    Project project;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
        OperationRegistry.registerOperation(getCoreModule(), "multivalued-cell-split", MultiValuedCellSplitOperation.class);
    }

    @BeforeMethod
    public void createProject() {
        project = createCSVProject(
                "Key,Value\n"
                        + "Record_1,one:two;three four;fiveSix SevèËight;niné91011twelve thirteen 14Àifteen\n");
    }

    @Test
    public void serializeMultiValuedCellSplitOperationWithSeparator() throws Exception {
        String json = "{\"op\":\"core/multivalued-cell-split\","
                + "\"description\":\"Split multi-valued cells in column Value\","
                + "\"columnName\":\"Value\","
                + "\"keyColumnName\":\"Key\","
                + "\"mode\":\"separator\","
                + "\"separator\":\":\","
                + "\"regex\":false}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, MultiValuedCellSplitOperation.class), json);
    }

    @Test
    public void serializeMultiValuedCellSplitOperationWithLengths() throws Exception {
        String json = "{\"op\":\"core/multivalued-cell-split\","
                + "\"description\":\"Split multi-valued cells in column Value\","
                + "\"columnName\":\"Value\","
                + "\"keyColumnName\":\"Key\","
                + "\"mode\":\"lengths\","
                + "\"fieldLengths\":[1,1]}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, MultiValuedCellSplitOperation.class), json);
    }

    /**
     * Test to demonstrate the intended behaviour of the function, for issue #1268
     * https://github.com/OpenRefine/OpenRefine/issues/1268
     */

    @Test
    public void testSplitMultiValuedCellsTextSeparator() throws Exception {
        AbstractOperation op = new MultiValuedCellSplitOperation(
                "Value",
                "Key",
                ":",
                false);
        Process process = op.createProcess(project, new Properties());
        process.performImmediate();

        int keyCol = project.columnModel.getColumnByName("Key").getCellIndex();
        int valueCol = project.columnModel.getColumnByName("Value").getCellIndex();

        Assert.assertEquals(project.rows.get(0).getCellValue(keyCol), "Record_1");
        Assert.assertEquals(project.rows.get(0).getCellValue(valueCol), "one");
        Assert.assertEquals(project.rows.get(1).getCellValue(keyCol), null);
        Assert.assertEquals(project.rows.get(1).getCellValue(valueCol),
                "two;three four;fiveSix SevèËight;niné91011twelve thirteen 14Àifteen");
    }

    @Test
    public void testSplitMultiValuedCellsRegExSeparator() throws Exception {
        AbstractOperation op = new MultiValuedCellSplitOperation(
                "Value",
                "Key",
                "\\W",
                true);
        Process process = op.createProcess(project, new Properties());
        process.performImmediate();

        int keyCol = project.columnModel.getColumnByName("Key").getCellIndex();
        int valueCol = project.columnModel.getColumnByName("Value").getCellIndex();

        Assert.assertEquals(project.rows.get(0).getCellValue(keyCol), "Record_1");
        Assert.assertEquals(project.rows.get(0).getCellValue(valueCol), "one");
        Assert.assertEquals(project.rows.get(1).getCellValue(keyCol), null);
        Assert.assertEquals(project.rows.get(1).getCellValue(valueCol), "two");
        Assert.assertEquals(project.rows.get(2).getCellValue(keyCol), null);
        Assert.assertEquals(project.rows.get(2).getCellValue(valueCol), "three");
        Assert.assertEquals(project.rows.get(3).getCellValue(keyCol), null);
        Assert.assertEquals(project.rows.get(3).getCellValue(valueCol), "four");
    }

    @Test
    public void testSplitMultiValuedCellsLengths() throws Exception {
        int[] lengths = { 4, 4, 6, 4 };

        AbstractOperation op = new MultiValuedCellSplitOperation(
                "Value",
                "Key",
                lengths);
        Process process = op.createProcess(project, new Properties());
        process.performImmediate();

        int keyCol = project.columnModel.getColumnByName("Key").getCellIndex();
        int valueCol = project.columnModel.getColumnByName("Value").getCellIndex();

        Assert.assertEquals(project.rows.get(0).getCellValue(keyCol), "Record_1");
        Assert.assertEquals(project.rows.get(0).getCellValue(valueCol), "one:");
        Assert.assertEquals(project.rows.get(1).getCellValue(keyCol), null);
        Assert.assertEquals(project.rows.get(1).getCellValue(valueCol), "two;");
        Assert.assertEquals(project.rows.get(2).getCellValue(keyCol), null);
        Assert.assertEquals(project.rows.get(2).getCellValue(valueCol), "three ");
        Assert.assertEquals(project.rows.get(3).getCellValue(keyCol), null);
        Assert.assertEquals(project.rows.get(3).getCellValue(valueCol), "four");
    }

    @Test
    public void testSplitMultiValuedCellsTextCase() throws Exception {
        AbstractOperation op = new MultiValuedCellSplitOperation(
                "Value",
                "Key",
                "(?<=\\p{Lower}|[\\p{Lower}][\\s])(?=\\p{Upper})",
                true);
        Process process = op.createProcess(project, new Properties());
        process.performImmediate();

        int keyCol = project.columnModel.getColumnByName("Key").getCellIndex();
        int valueCol = project.columnModel.getColumnByName("Value").getCellIndex();

        Assert.assertEquals(project.rows.get(0).getCellValue(keyCol), "Record_1");
        Assert.assertEquals(project.rows.get(0).getCellValue(valueCol), "one:two;three four;five");
        Assert.assertEquals(project.rows.get(1).getCellValue(keyCol), null);
        Assert.assertEquals(project.rows.get(1).getCellValue(valueCol), "Six ");
        Assert.assertEquals(project.rows.get(2).getCellValue(keyCol), null);
        Assert.assertEquals(project.rows.get(2).getCellValue(valueCol), "Sevè");
        Assert.assertEquals(project.rows.get(3).getCellValue(keyCol), null);
        Assert.assertEquals(project.rows.get(3).getCellValue(valueCol), "Ëight;niné91011twelve thirteen 14Àifteen");
    }

    @Test
    public void testSplitMultiValuedCellsTextCaseReverse() throws Exception {
        AbstractOperation op = new MultiValuedCellSplitOperation(
                "Value",
                "Key",
                "(?<=\\p{Upper}|[\\p{Upper}][\\s])(?=\\p{Lower})",
                true);
        Process process = op.createProcess(project, new Properties());
        process.performImmediate();

        int keyCol = project.columnModel.getColumnByName("Key").getCellIndex();
        int valueCol = project.columnModel.getColumnByName("Value").getCellIndex();

        Assert.assertEquals(project.rows.get(0).getCellValue(keyCol), "Record_1");
        Assert.assertEquals(project.rows.get(0).getCellValue(valueCol), "one:two;three four;fiveS");
        Assert.assertEquals(project.rows.get(1).getCellValue(keyCol), null);
        Assert.assertEquals(project.rows.get(1).getCellValue(valueCol), "ix S");
        Assert.assertEquals(project.rows.get(2).getCellValue(keyCol), null);
        Assert.assertEquals(project.rows.get(2).getCellValue(valueCol), "evèË");
        Assert.assertEquals(project.rows.get(3).getCellValue(keyCol), null);
        Assert.assertEquals(project.rows.get(3).getCellValue(valueCol), "ight;niné91011twelve thirteen 14À");
        Assert.assertEquals(project.rows.get(4).getCellValue(keyCol), null);
        Assert.assertEquals(project.rows.get(4).getCellValue(valueCol), "ifteen");
    }

    @Test
    public void testSplitMultiValuedCellsTextNumber() throws Exception {
        AbstractOperation op = new MultiValuedCellSplitOperation(
                "Value",
                "Key",
                "(?<=\\p{Digit}|[\\p{Digit}][\\s])(?=\\p{L})",
                true);
        Process process = op.createProcess(project, new Properties());
        process.performImmediate();

        int keyCol = project.columnModel.getColumnByName("Key").getCellIndex();
        int valueCol = project.columnModel.getColumnByName("Value").getCellIndex();

        Assert.assertEquals(project.rows.get(0).getCellValue(keyCol), "Record_1");
        Assert.assertEquals(project.rows.get(0).getCellValue(valueCol), "one:two;three four;fiveSix SevèËight;niné91011");
        Assert.assertEquals(project.rows.get(1).getCellValue(keyCol), null);
        Assert.assertEquals(project.rows.get(1).getCellValue(valueCol), "twelve thirteen 14");
        Assert.assertEquals(project.rows.get(2).getCellValue(keyCol), null);
        Assert.assertEquals(project.rows.get(2).getCellValue(valueCol), "Àifteen");
    }

    @Test
    public void testSplitMultiValuedCellsTextNumberReverse() throws Exception {
        AbstractOperation op = new MultiValuedCellSplitOperation(
                "Value",
                "Key",
                "(?<=\\p{L}|[\\p{L}][\\s])(?=\\p{Digit})",
                true);
        Process process = op.createProcess(project, new Properties());
        process.performImmediate();

        int keyCol = project.columnModel.getColumnByName("Key").getCellIndex();
        int valueCol = project.columnModel.getColumnByName("Value").getCellIndex();

        Assert.assertEquals(project.rows.get(0).getCellValue(keyCol), "Record_1");
        Assert.assertEquals(project.rows.get(0).getCellValue(valueCol), "one:two;three four;fiveSix SevèËight;niné");
        Assert.assertEquals(project.rows.get(1).getCellValue(keyCol), null);
        Assert.assertEquals(project.rows.get(1).getCellValue(valueCol), "91011twelve thirteen ");
        Assert.assertEquals(project.rows.get(2).getCellValue(keyCol), null);
        Assert.assertEquals(project.rows.get(2).getCellValue(valueCol), "14Àifteen");
    }
}
