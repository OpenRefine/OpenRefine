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

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.util.Pool;
import com.google.refine.util.TestUtils;

public class CellTests {

    String reconJson = "{\"id\":1533649346002675326,"
            + "\"judgmentHistoryEntry\":1530278634724,"
            + "\"service\":\"https://tools.wmflabs.org/openrefine-wikidata/en/api\","
            + "\"identifierSpace\":\"http://www.wikidata.org/entity/\","
            + "\"schemaSpace\":\"http://www.wikidata.org/prop/direct/\","
            + "\"j\":\"matched\","
            + "\"m\":{\"id\":\"Q551479\",\"name\":\"La Monnaie\",\"score\":100,\"types\":[\"Q153562\"]},"
            + "\"c\":[{\"id\":\"Q551479\",\"name\":\"La Monnaie\",\"score\":100,\"types\":[\"Q153562\"]}],"
            + "\"f\":[false,false,34,0],\"judgmentAction\":\"auto\",\"judgmentBatchSize\":1,\"matchRank\":0}";

    Pool pool = mock(Pool.class);
    Recon recon = null;

    @Test
    public void serializeCellWithRecon() throws Exception {
        recon = Recon.loadStreaming(reconJson);
        when(pool.getRecon("1533649346002675326")).thenReturn(recon);
        String json = "{\"v\":\"http://www.wikidata.org/entity/Q41522540\",\"r\":\"1533649346002675326\"}";

        Cell c = Cell.loadStreaming(json, pool);
        TestUtils.isSerializedTo(c, json);
    }

    @Test
    public void serializeCellWithString() throws Exception {
        String json = "{\"v\":\"0000-0002-5022-0488\"}";
        Cell c = Cell.loadStreaming(json, pool);
        TestUtils.isSerializedTo(c, json);
    }

    @Test
    public void serializeNullCell() throws Exception {
        String json = "null";
        Cell c = Cell.loadStreaming(json, pool);
        assertNull(c);
    }

    @Test
    public void serializeEmptyStringCell() throws Exception {
        String json = "{\"v\":\"\"}";
        Cell c = Cell.loadStreaming(json, pool);
        TestUtils.isSerializedTo(c, json);
    }

    @Test
    public void serializeErrorCell() throws Exception {
        String json = "{\"e\":\"HTTP 403\"}";
        Cell c = Cell.loadStreaming(json, pool);
        TestUtils.isSerializedTo(c, json);
    }

    @Test
    public void getMessageFromErrorCell() throws Exception {
        String errorMessage = "Sample error message";
        EvalError err = new EvalError(errorMessage);
        Cell c = new Cell(err, null);
        assertEquals(c.getField("errorMessage", null), errorMessage);
        assertEquals(c.getField("value", null), err);
    }

    @Test
    public void serializeDateCell() throws Exception {
        String json = "{\"v\":\"2018-03-04T08:09:10Z\",\"t\":\"date\"}";
        TestUtils.isSerializedTo(Cell.loadStreaming(json, pool), json);
    }

    @Test
    public void serializeNumberCell() throws Exception {
        String json = "{\"v\": 1}";
        Cell c = Cell.loadStreaming(json, pool);
        TestUtils.isSerializedTo(c, json);
    }

    @Test
    public void serializeBooleanCell() throws Exception {
        String json = "{\"v\": true}";
        Cell c = Cell.loadStreaming(json, pool);
        TestUtils.isSerializedTo(c, json);
    }

    @Test
    public void serializeDatewithOffset() throws Exception {
        OffsetDateTime dateTimeValue = OffsetDateTime.parse("2017-05-12T05:45:00+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        Cell c = new Cell(dateTimeValue, null);
        String json = "{\"v\":\"2017-05-12T04:45:00Z\",\"t\":\"date\"}";
        TestUtils.isSerializedTo(c, json);
    }

    @Test
    public void serializeLocalDate() throws Exception {
        LocalDateTime dateTimeValue = LocalDateTime.of(2017, 5, 12, 0, 0, 0);
        Cell c = new Cell(dateTimeValue, null);
        String json = "{\"v\":\"2017-05-12T00:00:00Z\",\"t\":\"date\"}";
        TestUtils.isSerializedTo(c, json);
    }

    @Test
    public void serializeDoubleNan() throws Exception {
        double dn = Double.NaN;
        Cell c = new Cell(dn, null);
        String json = "{\"v\":\"NaN\"}";
        TestUtils.isSerializedTo(c, json);
    }

    @Test
    public void serializeFloatNan() throws Exception {
        Float fn = Float.NaN;
        Cell c = new Cell(fn, null);
        String json = "{\"v\":\"NaN\"}";
        TestUtils.isSerializedTo(c, json);
    }

    @Test
    public void serializeDoubleInfinity() throws Exception {
        double di = Double.POSITIVE_INFINITY;
        Cell c = new Cell(di, null);
        String json = "{\"v\":\"Infinity\"}";
        TestUtils.isSerializedTo(c, json);
    }

    @Test
    public void serializeFloatInfinity() throws Exception {
        Float fi = Float.POSITIVE_INFINITY;
        Cell c = new Cell(fi, null);
        String json = "{\"v\":\"Infinity\"}";
        TestUtils.isSerializedTo(c, json);
    }
}
