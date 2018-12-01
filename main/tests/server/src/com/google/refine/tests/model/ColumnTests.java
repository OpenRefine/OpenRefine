package com.google.refine.tests.model;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.testng.annotations.Test;

import com.google.refine.model.Column;
import com.google.refine.model.recon.ReconConfig;
import com.google.refine.model.recon.StandardReconConfig;
import com.google.refine.tests.util.TestUtils;

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
                + "\"type\":\"\","
                + "\"format\":\"default\","
                + "\"title\":\"\","
                + "\"description\":\"\","
                + "\"constraints\":\"{}\","
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
