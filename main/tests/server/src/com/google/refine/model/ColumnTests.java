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

package com.google.refine.model;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.testng.annotations.Test;

import com.google.refine.model.recon.ReconConfig;
import com.google.refine.model.recon.StandardReconConfig;
import com.google.refine.util.TestUtils;

import edu.mit.simile.butterfly.ButterflyModule;

public class ColumnTests {

    @Test
    public void serializeColumn() throws Exception {
        ButterflyModule core = mock(ButterflyModule.class);
        when(core.getName()).thenReturn("core");
        ReconConfig.registerReconConfig(core, "standard-service", StandardReconConfig.class);
        String json = "{\"cellIndex\":4,"
                + "\"originalName\":\"name\","
                + "\"name\":\"organization_name\","
                + "\"reconConfig\":{"
                + "   \"mode\":\"standard-service\","
                + "   \"service\":\"https://tools.wmflabs.org/openrefine-wikidata/en/api\","
                + "   \"identifierSpace\":\"http://www.wikidata.org/entity/\","
                + "   \"schemaSpace\":\"http://www.wikidata.org/prop/direct/\","
                + "   \"type\":{\"id\":\"Q43229\",\"name\":\"organization\"},"
                + "   \"autoMatch\":true,"
                + "   \"columnDetails\":["
                + "      {\"column\":\"organization_country\",\"propertyName\":\"SPARQL: P17/P297\",\"propertyID\":\"P17/P297\"},"
                + "      {\"column\":\"organization_id\",\"propertyName\":\"SPARQL: P3500|P2427\",\"propertyID\":\"P3500|P2427\"}"
                + "    ],"
                + "    \"limit\":0},"
                + "\"reconStats\":{"
                + "    \"nonBlanks\":299,"
                + "    \"newTopics\":0,"
                + "    \"matchedTopics\":222"
                + "}}";
        TestUtils.isSerializedTo(Column.load(json), json);
    }
}
