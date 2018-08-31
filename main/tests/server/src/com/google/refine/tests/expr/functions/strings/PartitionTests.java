package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.Partition;
import com.google.refine.tests.util.TestUtils;

public class PartitionTests {
    @Test
    public void serializePartition() {
        String json = "{\"description\":\"Returns an array of strings [a,frag,b] where a is the string part before the first occurrence of frag in s and b is what's left. If omitFragment is true, frag is not returned.\",\"params\":\"string s, string or regex frag, optional boolean omitFragment\",\"returns\":\"array\"}";
        TestUtils.isSerializedTo(new Partition(), json);
    }
}

