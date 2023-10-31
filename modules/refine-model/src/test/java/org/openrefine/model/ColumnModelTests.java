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

package org.openrefine.model;

import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.testng.annotations.Test;

import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class ColumnModelTests {

    ColumnModel SUT = new ColumnModel(
            Arrays.asList(
                    new ColumnMetadata("a", "b", 1234L, null),
                    new ColumnMetadata("c", "d", 5678L, null)));

    @Test
    public void serializeColumnModel() throws ModelException {
        String json = "{\n" +
                "       \"columns\" : [ {\n" +
                "         \"name\" : \"b\",\n" +
                "         \"lastModified\" : 1234,\n" +
                "         \"originalName\" : \"a\"\n" +
                "       }, {\n" +
                "         \"name\" : \"d\",\n" +
                "         \"lastModified\" : 5678,\n" +
                "         \"originalName\" : \"c\"\n" +
                "       } ],\n" +
                "       \"keyCellIndex\" : 0,\n" +
                "       \"keyColumnName\" : \"b\",\n" +
                "       \"hasRecords\": false\n" +
                "     }";
        TestUtils.isSerializedTo(SUT, json, ParsingUtilities.defaultWriter);
    }

    @Test
    public void serializeColumnModelEmpty() {
        String json = "{"
                + "\"columns\":[]," +
                " \"hasRecords\": false"
                + "}";
        ColumnModel m = new ColumnModel(Collections.emptyList());
        TestUtils.isSerializedTo(m, json, ParsingUtilities.defaultWriter);
    }

    @Test
    public void testMerge() {
        ColumnModel columnModelB = new ColumnModel(
                Arrays.asList(
                        new ColumnMetadata("e", "f", 1234L, null),
                        new ColumnMetadata("g", "h", 5678L, null)));
        ColumnModel expected = new ColumnModel(
                Arrays.asList(
                        new ColumnMetadata("a", "b", 1234L, null),
                        new ColumnMetadata("c", "d", 5678L, null)));

        assertEquals(SUT.merge(columnModelB), expected);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testMergeIncompatibleNumberOfColumns() {
        ColumnModel columnModel = new ColumnModel(
                Arrays.asList(new ColumnMetadata("a", "b", 1234L, null)));
        SUT.merge(columnModel);
    }

    @Test
    public void testMarkColumnsAsModified() {
        ColumnModel actual = SUT.markColumnsAsModified(1010L);

        ColumnModel expected = new ColumnModel(
                Arrays.asList(
                        new ColumnMetadata("b", "b", 1010L, null),
                        new ColumnMetadata("d", "d", 1010L, null)));

        assertEquals(actual, expected);
    }
}
