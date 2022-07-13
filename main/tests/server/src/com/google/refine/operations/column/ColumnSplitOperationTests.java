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

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class ColumnSplitOperationTests extends RefineTest {

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "column-split", ColumnSplitOperation.class);
    }

    @Test
    public void serializeColumnSplitOperationBySeparator() throws Exception {
        String json = "{\n" +
                "    \"op\": \"core/column-split\",\n" +
                "    \"description\": \"Split column ea by separator\",\n" +
                "    \"engineConfig\": {\n" +
                "      \"mode\": \"row-based\",\n" +
                "      \"facets\": []\n" +
                "    },\n" +
                "    \"columnName\": \"ea\",\n" +
                "    \"guessCellType\": true,\n" +
                "    \"removeOriginalColumn\": true,\n" +
                "    \"mode\": \"separator\",\n" +
                "    \"separator\": \"e\",\n" +
                "    \"regex\": false,\n" +
                "    \"maxColumns\": 0\n" +
                "  }";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ColumnSplitOperation.class), json);
    }

    @Test
    public void serializeColumnSplitOperationByLengths() throws Exception {
        String json = "{\n" +
                "    \"op\": \"core/column-split\",\n" +
                "    \"description\": \"Split column ea by field lengths\",\n" +
                "    \"engineConfig\": {\n" +
                "      \"mode\": \"row-based\",\n" +
                "      \"facets\": []\n" +
                "    },\n" +
                "    \"columnName\": \"ea\",\n" +
                "    \"guessCellType\": true,\n" +
                "    \"removeOriginalColumn\": true,\n" +
                "    \"mode\": \"lengths\",\n" +
                "    \"fieldLengths\": [1,1]\n" +
                "  }";
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ColumnSplitOperation.class), json);
    }
}
