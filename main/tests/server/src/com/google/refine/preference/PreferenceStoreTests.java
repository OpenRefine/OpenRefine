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

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;

import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class PreferenceStoreTests {

    public static String json = "{"
            + "\"entries\":{"
            + "   \"reconciliation.standardServices\":["
            + "           {\"propose_properties\":{\"service_url\":\"https://tools.wmflabs.org/openrefine-wikidata\",\"service_path\":\"/en/propose_properties\"},\"preview\":{\"width\":320,\"url\":\"https://tools.wmflabs.org/openrefine-wikidata/en/preview?id={{id}}\",\"height\":90},\"view\":{\"url\":\"https://www.wikidata.org/wiki/{{id}}\"},\"ui\":{\"handler\":\"ReconStandardServicePanel\"},\"identifierSpace\":\"http://www.wikidata.org/entity/\",\"name\":\"Wikidata Reconciliation for OpenRefine (en)\",\"suggest\":{\"property\":{\"flyout_service_path\":\"/en/flyout/property?id=${id}\",\"service_url\":\"https://tools.wmflabs.org/openrefine-wikidata\",\"service_path\":\"/en/suggest/property\"},\"type\":{\"flyout_service_path\":\"/en/flyout/type?id=${id}\",\"service_url\":\"https://tools.wmflabs.org/openrefine-wikidata\",\"service_path\":\"/en/suggest/type\"},\"entity\":{\"flyout_service_path\":\"/en/flyout/entity?id=${id}\",\"service_url\":\"https://tools.wmflabs.org/openrefine-wikidata\",\"service_path\":\"/en/suggest/entity\"}},\"defaultTypes\":[{\"name\":\"entity\",\"id\":\"Q35120\"}],\"url\":\"https://tools.wmflabs.org/openrefine-wikidata/en/api\",\"schemaSpace\":\"http://www.wikidata.org/prop/direct/\"}"
            + "        ],"
            + "   \"scripting.starred-expressions\":{\"class\":\"com.google.refine.preference.TopList\",\"top\":2147483647,\"list\":[]},"
            + "   \"scripting.expressions\":{\"class\":\"com.google.refine.preference.TopList\",\"top\":100,\"list\":[]}"
            + "}}";

    @Test
    public void serializePreferenceStore() throws JsonParseException, JsonMappingException, IOException {

        String jsonAfter = "{"
                + "\"entries\":{"
                + "   \"reconciliation.standardServices\":["
                + "           {\"propose_properties\":{\"service_url\":\"https://tools.wmflabs.org/openrefine-wikidata\",\"service_path\":\"/en/propose_properties\"},\"preview\":{\"width\":320,\"url\":\"https://tools.wmflabs.org/openrefine-wikidata/en/preview?id={{id}}\",\"height\":90},\"view\":{\"url\":\"https://www.wikidata.org/wiki/{{id}}\"},\"ui\":{\"handler\":\"ReconStandardServicePanel\"},\"identifierSpace\":\"http://www.wikidata.org/entity/\",\"name\":\"Wikidata Reconciliation for OpenRefine (en)\",\"suggest\":{\"property\":{\"flyout_service_path\":\"/en/flyout/property?id=${id}\",\"service_url\":\"https://tools.wmflabs.org/openrefine-wikidata\",\"service_path\":\"/en/suggest/property\"},\"type\":{\"flyout_service_path\":\"/en/flyout/type?id=${id}\",\"service_url\":\"https://tools.wmflabs.org/openrefine-wikidata\",\"service_path\":\"/en/suggest/type\"},\"entity\":{\"flyout_service_path\":\"/en/flyout/entity?id=${id}\",\"service_url\":\"https://tools.wmflabs.org/openrefine-wikidata\",\"service_path\":\"/en/suggest/entity\"}},\"defaultTypes\":[{\"name\":\"entity\",\"id\":\"Q35120\"}],\"url\":\"https://tools.wmflabs.org/openrefine-wikidata/en/api\",\"schemaSpace\":\"http://www.wikidata.org/prop/direct/\"}"
                + "        ],"
                + "   \"scripting.starred-expressions\":{\"class\":\"com.google.refine.preference.TopList\",\"top\":2147483647,\"list\":[]},"
                + "   \"scripting.expressions\":{\"class\":\"com.google.refine.preference.TopList\",\"top\":100,\"list\":[]},"
                + "   \"mypreference\":\"myvalue\""
                + "}}";
        PreferenceStore prefStore = ParsingUtilities.mapper.readValue(json, PreferenceStore.class);
        assertFalse(prefStore.isDirty());
        prefStore.put("mypreference", "myvalue");
        assertTrue(prefStore.isDirty());
        TestUtils.isSerializedTo(prefStore, jsonAfter);
        assertFalse(prefStore.isDirty());
    }

}
