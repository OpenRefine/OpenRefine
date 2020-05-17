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

package org.openrefine.history;

import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.ProjectManager;
import org.openrefine.ProjectManagerStub;
import org.openrefine.model.DatamodelRunner;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class HistoryEntryTests {

    public static final String fullJson = "{"
            + "\"id\":1533633623158,"
            + "\"description\":\"Create new column uri based on column country by filling 269 rows with grel:\\\"https://www.wikidata.org/wiki/\\\"+cell.recon.match.id\","
            + "\"time\":\"2018-08-07T09:06:37Z\","
            + "\"change\" : {\n"
            + "  \"type\" : \"org.openrefine.history.ChangeStub\"\n"
            + "},\n"
            + "\"operation\":{\"op\":\"core/my-operation\","
            + "   \"description\":\"some description\"}"
            + "}";

    public static final String unknownOperationJson = "{\n" +
            "  \"change\" : {\n" +
            "    \"type\" : \"org.openrefine.history.ChangeStub\"\n" +
            "  },\n" +
            "  \"description\" : \"some mysterious operation\",\n" +
            "  \"id\" : 1533633623158,\n" +
            "  \"operation\" : {\n" +
            "    \"description\" : \"some mysterious operation\",\n" +
            "    \"op\" : \"someextension/unknown-operation\",\n" +
            "    \"some_parameter\" : 234\n" +
            "  },\n" +
            "  \"time\" : \"2018-08-07T09:06:37Z\"\n" +
            "}";

    @BeforeTest
    public void register() {
        OperationRegistry.registerOperation("core", "my-operation", OperationStub.class);
        ProjectManager.singleton = new ProjectManagerStub(mock(DatamodelRunner.class));
    }

    @Test
    public void serializeHistoryEntry() throws Exception {
        String json = "{\"id\":1533651837506,"
                + "\"description\":\"Discard recon judgment for single cell on row 76, column organization_name, containing \\\"Catholic University Leuven\\\"\","
                + "\"time\":\"2018-08-07T14:18:29Z\"}";
        TestUtils.isSerializedTo(HistoryEntry.load(json), json, ParsingUtilities.defaultWriter);
    }

    @Test
    public void serializeHistoryEntryWithOperation() throws Exception {
        String jsonSimple = "{"
                + "\"id\":1533633623158,"
                + "\"description\":\"Create new column uri based on column country by filling 269 rows with grel:\\\"https://www.wikidata.org/wiki/\\\"+cell.recon.match.id\","
                + "\"time\":\"2018-08-07T09:06:37Z\"}";

        HistoryEntry historyEntry = HistoryEntry.load(fullJson);
        TestUtils.isSerializedTo(historyEntry, jsonSimple, ParsingUtilities.defaultWriter);
        TestUtils.isSerializedTo(historyEntry, fullJson, ParsingUtilities.saveWriter);
    }

    @Test
    public void deserializeUnknownOperation() throws IOException {
        // Unknown operations are serialized back as they were parsed
        HistoryEntry entry = HistoryEntry.load(unknownOperationJson);
        TestUtils.isSerializedTo(entry, unknownOperationJson, ParsingUtilities.saveWriter);
    }
}
