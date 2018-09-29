package com.google.refine.tests.preference;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.json.JSONObject;
import org.testng.annotations.Test;

import com.google.refine.preference.PreferenceStore;
import com.google.refine.tests.util.TestUtils;

public class PreferenceStoreTests {
    @Test
    public void serializePreferenceStore() {
        String json = "{"
                + "\"entries\":{"
                + "   \"reconciliation.standardServices\":["
                + "           {\"propose_properties\":{\"service_url\":\"https://tools.wmflabs.org/openrefine-wikidata\",\"service_path\":\"/en/propose_properties\"},\"preview\":{\"width\":320,\"url\":\"https://tools.wmflabs.org/openrefine-wikidata/en/preview?id={{id}}\",\"height\":90},\"view\":{\"url\":\"https://www.wikidata.org/wiki/{{id}}\"},\"ui\":{\"handler\":\"ReconStandardServicePanel\"},\"identifierSpace\":\"http://www.wikidata.org/entity/\",\"name\":\"Wikidata Reconciliation for OpenRefine (en)\",\"suggest\":{\"property\":{\"flyout_service_path\":\"/en/flyout/property?id=${id}\",\"service_url\":\"https://tools.wmflabs.org/openrefine-wikidata\",\"service_path\":\"/en/suggest/property\"},\"type\":{\"flyout_service_path\":\"/en/flyout/type?id=${id}\",\"service_url\":\"https://tools.wmflabs.org/openrefine-wikidata\",\"service_path\":\"/en/suggest/type\"},\"entity\":{\"flyout_service_path\":\"/en/flyout/entity?id=${id}\",\"service_url\":\"https://tools.wmflabs.org/openrefine-wikidata\",\"service_path\":\"/en/suggest/entity\"}},\"defaultTypes\":[{\"name\":\"entity\",\"id\":\"Q35120\"}],\"url\":\"https://tools.wmflabs.org/openrefine-wikidata/en/api\",\"schemaSpace\":\"http://www.wikidata.org/prop/direct/\"}"
                + "        ],"
                + "   \"scripting.starred-expressions\":{\"class\":\"com.google.refine.preference.TopList\",\"top\":2147483647,\"list\":[]},"
                + "   \"scripting.expressions\":{\"class\":\"com.google.refine.preference.TopList\",\"top\":100,\"list\":[]}"
                + "}}";
        String jsonAfter = "{"
                + "\"entries\":{"
                + "   \"reconciliation.standardServices\":["
                + "           {\"propose_properties\":{\"service_url\":\"https://tools.wmflabs.org/openrefine-wikidata\",\"service_path\":\"/en/propose_properties\"},\"preview\":{\"width\":320,\"url\":\"https://tools.wmflabs.org/openrefine-wikidata/en/preview?id={{id}}\",\"height\":90},\"view\":{\"url\":\"https://www.wikidata.org/wiki/{{id}}\"},\"ui\":{\"handler\":\"ReconStandardServicePanel\"},\"identifierSpace\":\"http://www.wikidata.org/entity/\",\"name\":\"Wikidata Reconciliation for OpenRefine (en)\",\"suggest\":{\"property\":{\"flyout_service_path\":\"/en/flyout/property?id=${id}\",\"service_url\":\"https://tools.wmflabs.org/openrefine-wikidata\",\"service_path\":\"/en/suggest/property\"},\"type\":{\"flyout_service_path\":\"/en/flyout/type?id=${id}\",\"service_url\":\"https://tools.wmflabs.org/openrefine-wikidata\",\"service_path\":\"/en/suggest/type\"},\"entity\":{\"flyout_service_path\":\"/en/flyout/entity?id=${id}\",\"service_url\":\"https://tools.wmflabs.org/openrefine-wikidata\",\"service_path\":\"/en/suggest/entity\"}},\"defaultTypes\":[{\"name\":\"entity\",\"id\":\"Q35120\"}],\"url\":\"https://tools.wmflabs.org/openrefine-wikidata/en/api\",\"schemaSpace\":\"http://www.wikidata.org/prop/direct/\"}"
                + "        ],"
                + "   \"scripting.starred-expressions\":{\"class\":\"com.google.refine.preference.TopList\",\"top\":2147483647,\"list\":[]},"
                + "   \"scripting.expressions\":{\"class\":\"com.google.refine.preference.TopList\",\"top\":100,\"list\":[]},"
                + "   \"mypreference\":\"myvalue\""
                + "}}";
        PreferenceStore prefStore = new PreferenceStore();
        prefStore.load(new JSONObject(json));
        assertFalse(prefStore.isDirty());
        prefStore.put("mypreference", "myvalue");
        assertTrue(prefStore.isDirty());
        TestUtils.isSerializedTo(prefStore, jsonAfter);
        assertFalse(prefStore.isDirty());
    }
    
}
