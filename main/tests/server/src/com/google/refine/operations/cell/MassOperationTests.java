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

package com.google.refine.operations.cell;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.refine.RefineTest;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.cell.MassEditOperation.Edit;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class MassOperationTests extends RefineTest {

    private List<Edit> editList;
    private String editsString;

    @BeforeSuite
    public void setUp() {
        OperationRegistry.registerOperation(getCoreModule(), "mass-edit", MassEditOperation.class);
    }

    @Test
    public void serializeMassEditOperation() throws Exception {
        String json = "{\"op\":\"core/mass-edit\","
                + "\"description\":\"Mass edit cells in column my column\","
                + "\"engineConfig\":{\"mode\":\"record-based\",\"facets\":[]},"
                + "\"columnName\":\"my column\",\"expression\":\"value\","
                + "\"edits\":[{\"fromBlank\":false,\"fromError\":false,\"from\":[\"String\"],\"to\":\"newString\"}]}";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, MassEditOperation.class), json);
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

}
