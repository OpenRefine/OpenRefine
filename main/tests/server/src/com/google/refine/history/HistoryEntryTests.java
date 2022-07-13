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

package com.google.refine.history;

import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.column.ColumnAdditionOperation;
import com.google.refine.util.TestUtils;

public class HistoryEntryTests extends RefineTest {

    public static final String fullJson = "{"
            + "\"id\":1533633623158,"
            + "\"description\":\"Create new column uri based on column country by filling 269 rows with grel:\\\"https://www.wikidata.org/wiki/\\\"+cell.recon.match.id\","
            + "\"time\":\"2018-08-07T09:06:37Z\","
            + "\"operation\":{\"op\":\"core/column-addition\","
            + "   \"description\":\"Create column uri at index 2 based on column country using expression grel:\\\"https://www.wikidata.org/wiki/\\\"+cell.recon.match.id\","
            + "   \"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
            + "   \"newColumnName\":\"uri\","
            + "   \"columnInsertIndex\":2,"
            + "   \"baseColumnName\":\"country\","
            + "   \"expression\":\"grel:\\\"https://www.wikidata.org/wiki/\\\"+cell.recon.match.id\","
            + "   \"onError\":\"set-to-blank\"}"
            + "}";

    public static final String unknownOperationJson = "{"
            + "\"id\":1533633623158,"
            + "\"description\":\"some mysterious operation\","
            + "\"time\":\"2018-08-07T09:06:37Z\","
            + "\"operation\":{\"op\":\"someextension/unknown-operation\","
            + "   \"description\":\"some mysterious operation\","
            + "   \"some_parameter\":234\n"
            + "}\n"
            + "}";

    Project project;

    @BeforeTest
    public void register() {
        OperationRegistry.registerOperation(getCoreModule(), "column-addition", ColumnAdditionOperation.class);
    }

    @BeforeMethod
    public void setUp() {
        project = mock(Project.class);
    }

    @Test
    public void serializeHistoryEntry() throws Exception {
        String json = "{\"id\":1533651837506,"
                + "\"description\":\"Discard recon judgment for single cell on row 76, column organization_name, containing \\\"Catholic University Leuven\\\"\","
                + "\"time\":\"2018-08-07T14:18:29Z\"}";
        TestUtils.isSerializedTo(HistoryEntry.load(project, json), json);
    }

    @Test
    public void serializeHistoryEntryWithOperation() throws Exception {
        String jsonSimple = "{"
                + "\"id\":1533633623158,"
                + "\"description\":\"Create new column uri based on column country by filling 269 rows with grel:\\\"https://www.wikidata.org/wiki/\\\"+cell.recon.match.id\","
                + "\"time\":\"2018-08-07T09:06:37Z\"}";

        HistoryEntry historyEntry = HistoryEntry.load(project, fullJson);
        TestUtils.isSerializedTo(historyEntry, jsonSimple, false);
        TestUtils.isSerializedTo(historyEntry, fullJson, true);
    }

    @Test
    public void deserializeUnknownOperation() throws IOException {
        // Unknown operations are serialized back as they were parsed
        HistoryEntry entry = HistoryEntry.load(project, unknownOperationJson);
        TestUtils.isSerializedTo(entry, unknownOperationJson, true);
    }
}
