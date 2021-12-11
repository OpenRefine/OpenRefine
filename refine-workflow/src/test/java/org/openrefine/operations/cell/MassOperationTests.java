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

package org.openrefine.operations.cell;

import static org.mockito.Mockito.mock;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.browsing.DecoratedValue;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.ListFacet.ListFacetConfig;
import org.openrefine.expr.EvalError;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.ParsingException;
import org.openrefine.grel.Parser;
import org.openrefine.model.GridState;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.operations.cell.MassEditOperation.Edit;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class MassOperationTests extends RefineTest {

    private List<Edit> editList;
    private String editsString;

    @BeforeSuite
    public void setUp() {
        OperationRegistry.registerOperation("core", "mass-edit", MassEditOperation.class);
    }

    @Test
    public void serializeMassEditOperation() throws Exception {
        String json = "{\"op\":\"core/mass-edit\","
                + "\"description\":\"Mass edit cells in column my column\","
                + "\"engineConfig\":{\"mode\":\"record-based\",\"facets\":[]},"
                + "\"columnName\":\"my column\",\"expression\":\"value\","
                + "\"edits\":[{\"fromBlank\":false,\"fromError\":false,\"from\":[\"String\"],\"to\":\"newString\"}]}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, MassEditOperation.class), json, ParsingUtilities.defaultWriter);
    }

    @Test
    public void testReconstructEditString() throws Exception {
        editsString = "[{\"from\":[\"String\"],\"to\":\"newString\",\"type\":\"text\"}]";

        editList = ParsingUtilities.mapper.readValue(editsString, new TypeReference<List<Edit>>() {
        });

        Assert.assertEquals(editList.get(0).from.size(), 1);
        Assert.assertEquals(editList.get(0).from.get(0), "String");
        Assert.assertEquals(editList.get(0).to, "newString");
        Assert.assertFalse(editList.get(0).fromBlank);
        Assert.assertFalse(editList.get(0).fromError);
    }

    @Test
    public void testReconstructEditMultiString() throws Exception {
        editsString = "[{\"from\":[\"String1\",\"String2\"],\"to\":\"newString\",\"type\":\"text\"}]";

        editList = ParsingUtilities.mapper.readValue(editsString, new TypeReference<List<Edit>>() {
        });

        Assert.assertEquals(editList.get(0).from.size(), 2);
        Assert.assertEquals(editList.get(0).from.get(0), "String1");
        Assert.assertEquals(editList.get(0).from.get(1), "String2");
        Assert.assertEquals(editList.get(0).to, "newString");
        Assert.assertFalse(editList.get(0).fromBlank);
        Assert.assertFalse(editList.get(0).fromError);
    }

    @Test
    public void testReconstructEditBoolean() throws Exception {
        editsString = "[{\"from\":[true],\"to\":\"newString\",\"type\":\"text\"}]";

        editList = ParsingUtilities.mapper.readValue(editsString, new TypeReference<List<Edit>>() {
        });

        Assert.assertEquals(editList.get(0).from.size(), 1);
        Assert.assertEquals(editList.get(0).from.get(0), "true");
        Assert.assertEquals(editList.get(0).to, "newString");
        Assert.assertFalse(editList.get(0).fromBlank);
        Assert.assertFalse(editList.get(0).fromError);
    }

    @Test
    public void testReconstructEditNumber() throws Exception {
        editsString = "[{\"from\":[1],\"to\":\"newString\",\"type\":\"text\"}]";

        editList = ParsingUtilities.mapper.readValue(editsString, new TypeReference<List<Edit>>() {
        });

        Assert.assertEquals(editList.get(0).from.size(), 1);
        Assert.assertEquals(editList.get(0).from.get(0), "1");
        Assert.assertEquals(editList.get(0).to, "newString");
        Assert.assertFalse(editList.get(0).fromBlank);
        Assert.assertFalse(editList.get(0).fromError);
    }

    @Test
    public void testReconstructEditDate() throws Exception {
        editsString = "[{\"from\":[\"2018-10-04T00:00:00Z\"],\"to\":\"newString\",\"type\":\"text\"}]";

        editList = ParsingUtilities.mapper.readValue(editsString, new TypeReference<List<Edit>>() {
        });

        Assert.assertEquals(editList.get(0).from.get(0), "2018-10-04T00:00:00Z");
        Assert.assertEquals(editList.get(0).to, "newString");
        Assert.assertFalse(editList.get(0).fromBlank);
        Assert.assertFalse(editList.get(0).fromError);
    }

    @Test
    public void testReconstructEditEmpty() throws Exception {
        editsString = "[{\"from\":[\"\"],\"to\":\"newString\",\"type\":\"text\"}]";

        editList = ParsingUtilities.mapper.readValue(editsString, new TypeReference<List<Edit>>() {
        });

        Assert.assertEquals(editList.get(0).from.size(), 1);
        Assert.assertEquals(editList.get(0).from.get(0), "");
        Assert.assertEquals(editList.get(0).to, "newString");
        Assert.assertTrue(editList.get(0).fromBlank);
        Assert.assertFalse(editList.get(0).fromError);

    }

    // Not yet testing for mass edit from OR Error

    private GridState initialState;
    private static EngineConfig engineConfig;
    private ListFacetConfig facet;
    private List<Edit> edits;
    private List<Edit> editsWithFromBlank;

    @BeforeTest
    public void setUpInitialState() {
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
        Project project = createProject("my project", new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "v1", "a" },
                        { "v3", "a" },
                        { "", "a" },
                        { "", "b" },
                        { new EvalError("error"), "a" },
                        { "v1", "b" }
                });
        initialState = project.getCurrentGridState();
        facet = new ListFacetConfig();
        facet.columnName = "bar";
        facet.setExpression("grel:value");
        facet.selection = Collections.singletonList(new DecoratedValue("a", "a"));
        engineConfig = new EngineConfig(Arrays.asList(facet), Engine.Mode.RowBased);
        edits = Collections.singletonList(new Edit(Collections.singletonList("v1"), false, false, "v2"));
        editsWithFromBlank = Arrays.asList(edits.get(0), new Edit(Collections.emptyList(), true, false, "hey"));
    }

    @Test
    public void testSimpleReplace() throws DoesNotApplyException, ParsingException {
        Change change = new MassEditOperation(engineConfig, "foo", "grel:value", editsWithFromBlank).createChange();
        GridState applied = change.apply(initialState, mock(ChangeContext.class));
        Row row0 = applied.getRow(0);
        Assert.assertEquals(row0.getCellValue(0), "v2");
        Assert.assertEquals(row0.getCellValue(1), "a");
        Row row1 = applied.getRow(1);
        Assert.assertEquals(row1.getCellValue(0), "v3");
        Assert.assertEquals(row1.getCellValue(1), "a");
        Row row2 = applied.getRow(2);
        Assert.assertEquals(row2.getCellValue(0), "hey");
        Assert.assertEquals(row2.getCellValue(1), "a");
        Row row3 = applied.getRow(3);
        Assert.assertEquals(row3.getCellValue(0), "");
        Assert.assertEquals(row3.getCellValue(1), "b");
        Row row4 = applied.getRow(5);
        Assert.assertEquals(row4.getCellValue(0), "v1");
        Assert.assertEquals(row4.getCellValue(1), "b");
    }

    @Test
    public void testRecordsMode() throws DoesNotApplyException, ParsingException {
        EngineConfig engineConfig = new EngineConfig(Arrays.asList(facet), Engine.Mode.RecordBased);
        Change change = new MassEditOperation(engineConfig, "foo", "grel:value", editsWithFromBlank).createChange();
        GridState applied = change.apply(initialState, mock(ChangeContext.class));
        Row row0 = applied.getRow(0);
        Assert.assertEquals(row0.getCellValue(0), "v2");
        Assert.assertEquals(row0.getCellValue(1), "a");
        Row row1 = applied.getRow(1);
        Assert.assertEquals(row1.getCellValue(0), "v3");
        Assert.assertEquals(row1.getCellValue(1), "a");
        Row row2 = applied.getRow(2);
        Assert.assertEquals(row2.getCellValue(0), "hey");
        Assert.assertEquals(row2.getCellValue(1), "a");
        Row row3 = applied.getRow(3);
        Assert.assertEquals(row3.getCellValue(0), "hey");
        Assert.assertEquals(row3.getCellValue(1), "b");
        Row row4 = applied.getRow(5);
        Assert.assertEquals(row4.getCellValue(0), "v1");
        Assert.assertEquals(row4.getCellValue(1), "b");
    }

}
