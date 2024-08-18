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

import java.io.Serializable;

import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class MultiValuedCellSplitOperationTests extends RefineTest {

    Project project;
    Project biggerProject;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
        OperationRegistry.registerOperation(getCoreModule(), "multivalued-cell-split", MultiValuedCellSplitOperation.class);
    }

    @BeforeMethod
    public void createProject() {
        project = createProject(
                new String[] { "Key", "Value" },
                new Serializable[][] {
                        { "Record_1", "one:two;three four;fiveSix SevèËight;niné91011twelve thirteen 14Àifteen" }
                });
        biggerProject = createProject(
                new String[] { "key", "foo", "bar" },
                new Serializable[][] {
                        { "record1", "a||b", "c" },
                        { null, "c|d", "e" },
                        { null, 12, "f" },
                        { "record2", "", "g" },
                        { null, "h|i", "" },
                        { null, null, "j" },
                        { null, null, null }
                });
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

        runOperation(op, project);

        Project expectedProject = createProject(
                new String[] { "Key", "Value" },
                new Serializable[][] {
                        { "Record_1", "one" },
                        { null, "two;three four;fiveSix SevèËight;niné91011twelve thirteen 14Àifteen" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void testSplitMultiValuedCellsRegExSeparator() throws Exception {
        AbstractOperation op = new MultiValuedCellSplitOperation(
                "Value",
                "Key",
                "\\W",
                true);

        runOperation(op, project);

        Project expectedProject = createProject(
                new String[] { "Key", "Value" },
                new Serializable[][] {
                        { "Record_1", "one" },
                        { null, "two" },
                        { null, "three" },
                        { null, "four" },
                        { null, "fiveSix" },
                        { null, "SevèËight" },
                        { null, "niné91011twelve" },
                        { null, "thirteen" },
                        { null, "14Àifteen" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void testSplitMultiValuedCellsLengths() throws Exception {
        int[] lengths = { 4, 4, 6, 4 };

        AbstractOperation op = new MultiValuedCellSplitOperation(
                "Value",
                "Key",
                lengths);

        runOperation(op, project);

        Project expectedProject = createProject(
                new String[] { "Key", "Value" },
                new Serializable[][] {
                        { "Record_1", "one:" },
                        { null, "two;" },
                        { null, "three " },
                        { null, "four" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void testSplitMultiValuedCellsTextCase() throws Exception {
        AbstractOperation op = new MultiValuedCellSplitOperation(
                "Value",
                "Key",
                "(?<=\\p{Lower}|[\\p{Lower}][\\s])(?=\\p{Upper})",
                true);

        runOperation(op, project);

        Project expectedProject = createProject(
                new String[] { "Key", "Value" },
                new Serializable[][] {
                        { "Record_1", "one:two;three four;five" },
                        { null, "Six " },
                        { null, "Sevè" },
                        { null, "Ëight;niné91011twelve thirteen 14Àifteen" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void testSplitMultiValuedCellsTextCaseReverse() throws Exception {
        AbstractOperation op = new MultiValuedCellSplitOperation(
                "Value",
                "Key",
                "(?<=\\p{Upper}|[\\p{Upper}][\\s])(?=\\p{Lower})",
                true);

        runOperation(op, project);

        Project expectedProject = createProject(
                new String[] { "Key", "Value" },
                new Serializable[][] {
                        { "Record_1", "one:two;three four;fiveS" },
                        { null, "ix S" },
                        { null, "evèË" },
                        { null, "ight;niné91011twelve thirteen 14À" },
                        { null, "ifteen" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void testSplitMultiValuedCellsTextNumber() throws Exception {
        AbstractOperation op = new MultiValuedCellSplitOperation(
                "Value",
                "Key",
                "(?<=\\p{Digit}|[\\p{Digit}][\\s])(?=\\p{L})",
                true);

        runOperation(op, project);

        Project expectedProject = createProject(
                new String[] { "Key", "Value" },
                new Serializable[][] {
                        { "Record_1", "one:two;three four;fiveSix SevèËight;niné91011" },
                        { null, "twelve thirteen 14" },
                        { null, "Àifteen" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void testSplitMultiValuedCellsTextNumberReverse() throws Exception {
        AbstractOperation op = new MultiValuedCellSplitOperation(
                "Value",
                "Key",
                "(?<=\\p{L}|[\\p{L}][\\s])(?=\\p{Digit})",
                true);

        runOperation(op, project);

        Project expectedProject = createProject(
                new String[] { "Key", "Value" },
                new Serializable[][] {
                        { "Record_1", "one:two;three four;fiveSix SevèËight;niné" },
                        { null, "91011twelve thirteen " },
                        { null, "14Àifteen" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void testSplit() throws Exception {
        AbstractOperation SUT = new MultiValuedCellSplitOperation("foo", "key", "|", false);

        runOperation(SUT, biggerProject);

        Project expected = createProject(
                new String[] { "key", "foo", "bar" },
                new Serializable[][] {
                        { "record1", "a", "c" },
                        { null, "", null },
                        { null, "b", null },
                        { null, "c", "e" },
                        { null, "d", null },
                        { null, 12, "f" },
                        { "record2", "", "g" },
                        { null, "h", "" },
                        { null, "i", "j" },
                        { null, null, null }
                });

        assertProjectEquals(biggerProject, expected);
    }

    @Test
    public void testSplitRespectsKeyColumn() throws Exception {
        AbstractOperation SUT = new MultiValuedCellSplitOperation("foo", "bar", "|", false);

        runOperation(SUT, biggerProject);

        Project expected = createProject(
                new String[] { "key", "foo", "bar" },
                new Serializable[][] {
                        { "record1", "a", "c" },
                        { null, "", null },
                        { null, "b", null },
                        { null, "c", "e" },
                        { null, "d", null },
                        { null, 12, "f" },
                        { "record2", "", "g" },
                        { null, "h", "" },
                        { null, "i", null },
                        { null, null, "j" },
                        { null, null, null }
                });

        assertProjectEquals(biggerProject, expected);
    }

}
