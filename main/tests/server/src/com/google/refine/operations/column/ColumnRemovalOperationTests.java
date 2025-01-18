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

package com.google.refine.operations.column;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

import java.io.Serializable;
import java.util.Set;

import com.fasterxml.jackson.databind.node.TextNode;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.model.ColumnsDiff;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationDescription;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class ColumnRemovalOperationTests extends RefineTest {

    protected Project project;

    @BeforeMethod
    public void setUpInitialState() {
        project = createProject(new String[] { "foo", "bar", "hello" },
                new Serializable[][] {
                        { "v1", "a", "d" },
                        { "v3", "a", "f" },
                        { "", "a", "g" },
                        { "", "b", "h" },
                        { new EvalError("error"), "a", "i" },
                        { "v1", "b", "j" }
                });
    }

    @BeforeSuite
    public void setUp() {
        OperationRegistry.registerOperation(getCoreModule(), "column-removal", ColumnRemovalOperation.class);
    }

    @Test
    public void serializeColumnRemovalOperation() throws Exception {
        String json = "{\"op\":\"core/column-removal\","
                + "\"description\":" + new TextNode(OperationDescription.column_removal_brief("my column")).toString() + ","
                + "\"columnName\":\"my column\"}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ColumnRemovalOperation.class), json);
    }

    @Test
    public void testValidate() {
        ColumnRemovalOperation SUT = new ColumnRemovalOperation(null);
        assertThrows(IllegalArgumentException.class, () -> SUT.validate());
    }

    @Test
    public void testRemoval() throws Exception {
        ColumnRemovalOperation SUT = new ColumnRemovalOperation("foo");
        assertEquals(SUT.getColumnDependencies().get(), Set.of("foo"));
        assertEquals(SUT.getColumnsDiff().get(), ColumnsDiff.builder().deleteColumn("foo").build());

        runOperation(SUT, project);

        Project expected = createProject(
                new String[] { "bar", "hello" },
                new Serializable[][] {
                        { "a", "d" },
                        { "a", "f" },
                        { "a", "g" },
                        { "b", "h" },
                        { "a", "i" },
                        { "b", "j" },
                });
        assertProjectEquals(project, expected);
    }
}
