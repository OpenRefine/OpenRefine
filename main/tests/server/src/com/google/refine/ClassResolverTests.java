
package com.google.refine;

import org.testng.annotations.Test;

import com.google.refine.preference.TopList;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class ClassResolverTests {

    String oldJson = "{\"class\":\"com.google.gridworks.preference.TopList\",\"top\":100,\"list\":[]}";
    String newJson = "{\"class\":\"com.google.refine.preference.TopList\",\"top\":100,\"list\":[]}";

    @Test
    public void deserializeClassWithOldName() throws Exception {
        TopList topList = ParsingUtilities.mapper.readValue(oldJson, TopList.class);

        TestUtils.isSerializedTo(topList, newJson);
    }

    @Test
    public void deserializeClassWithNewName() throws Exception {
        TopList topList = ParsingUtilities.mapper.readValue(newJson, TopList.class);

        TestUtils.isSerializedTo(topList, newJson);
    }

}
