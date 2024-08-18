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
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class MultiValuedCellJoinOperationTests extends RefineTest {

    Project project;
    Project projectWithRecords;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "multivalued-cell-join", MultiValuedCellJoinOperation.class);
    }

    @BeforeMethod
    public void createProject() {
        project = createProject(
                new String[] { "Key", "Value" },
                new Serializable[][] {
                        { "Record_1", "one" },
                        { null, "two" },
                        { null, "three" },
                        { null, "four" }
                });
        projectWithRecords = createProject(
                new String[] { "key", "foo", "bar" },
                new Serializable[][] {
                        { "record1", "a", "b" },
                        { null, "c", "d" },
                        { "record2", "", "f" },
                        { null, "g", "" },
                        { null, null, null }
                });
    }

    @Test
    public void serializeMultiValuedCellJoinOperation() throws Exception {
        String json = "{\"op\":\"core/multivalued-cell-join\","
                + "\"description\":\"Join multi-valued cells in column value column\","
                + "\"columnName\":\"value column\","
                + "\"keyColumnName\":\"key column\","
                + "\"separator\":\",\"}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, MultiValuedCellJoinOperation.class), json);
    }

    /*
     * Test to demonstrate the intended behaviour of the function
     */

    @Test
    public void testJoinMultiValuedCells() throws Exception {
        AbstractOperation op = new MultiValuedCellJoinOperation(
                "Value",
                "Key",
                ",");

        runOperation(op, project);

        Project expectedProject = createProject(
                new String[] { "Key", "Value" },
                new Serializable[][] {
                        { "Record_1", "one,two,three,four" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void testJoinMultiValuedCellsMultipleSpaces() throws Exception {
        AbstractOperation op = new MultiValuedCellJoinOperation(
                "Value",
                "Key",
                ",     ,");

        runOperation(op, project);

        Project expectedProject = createProject(
                new String[] { "Key", "Value" },
                new Serializable[][] {
                        { "Record_1", "one,     ,two,     ,three,     ,four" },
                });
        assertProjectEquals(project, expectedProject);
    }

    @Test
    public void testJoin() throws Exception {
        AbstractOperation operation = new MultiValuedCellJoinOperation("foo", "key", ",");

        runOperation(operation, projectWithRecords);

        Project expected = createProject(new String[] { "key", "foo", "bar" },
                new Serializable[][] {
                        { "record1", "a,c", "b" },
                        { null, null, "d" },
                        { "record2", "g", "f" },
                });

        assertProjectEquals(projectWithRecords, expected);
    }

    @Test
    public void testCustomKey() throws Exception {
        AbstractOperation operation = new MultiValuedCellJoinOperation("bar", "foo", ",");

        runOperation(operation, projectWithRecords);

        Project expected = createProject(
                new String[] { "key", "foo", "bar" },
                new Serializable[][] {
                        { "record1", "a", "b" },
                        { null, "c", "d,f" },
                        { "record2", "", null },
                        { null, "g", "" },
                });

        assertProjectEquals(projectWithRecords, expected);
    }

}
