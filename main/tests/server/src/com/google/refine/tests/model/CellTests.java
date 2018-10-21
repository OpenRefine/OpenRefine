package com.google.refine.tests.model;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.testng.annotations.Test;

import com.google.refine.model.Cell;
import com.google.refine.model.Recon;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.Pool;

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
    public void serializeDateCell() throws Exception {
        String json = "{\"v\":\"2018-03-04T08:09:10Z\",\"t\":\"date\"}";
        TestUtils.isSerializedTo(Cell.loadStreaming(json, pool), json);
    }
}
