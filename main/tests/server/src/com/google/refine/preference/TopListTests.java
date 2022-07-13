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

package com.google.refine.preference;

import java.io.IOException;
import java.util.Collections;

import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class TopListTests {

    @Test
    public void serializeTopList() throws JsonParseException, JsonMappingException, IOException {
        String json = "{"
                + "\"class\":\"com.google.refine.preference.TopList\","
                + "\"top\":100,"
                + "\"list\":["
                + "   \"grel:value.parseJson()[\\\"end-date\\\"][\\\"year\\\"][\\\"value\\\"]\","
                + "   \"grel:value.parseJson()[\\\"start-date\\\"][\\\"year\\\"][\\\"value\\\"]\","
                + "   \"grel:value.parseJson()[\\\"organization\\\"][\\\"disambiguated-organization\\\"][\\\"disambiguated-organization-identifier\\\"]\","
                + "   \"grel:value.parseJson()[\\\"organization\\\"][\\\"address\\\"][\\\"country\\\"]\",\"grel:value.parseJson()[\\\"organization\\\"][\\\"name\\\"]\","
                + "   \"grel:value.parseJson()[\\\"employment-summary\\\"].join('###')\","
                + "   \"grel:\\\"https://pub.orcid.org/\\\"+value+\\\"/employments\\\"\""
                + "]}";
        PreferenceValue prefValue = ParsingUtilities.mapper.readValue(json, PreferenceValue.class);
        TestUtils.isSerializedTo(
                prefValue,
                json);

        String mapJson = "{\"key\":" + json + "}";
        TestUtils.isSerializedTo(Collections.singletonMap("key", prefValue), mapJson);
    }
}
