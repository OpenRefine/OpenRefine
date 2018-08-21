package com.google.refine.tests.preference;

import org.json.JSONObject;
import org.testng.annotations.Test;

import com.google.refine.preference.TopList;
import com.google.refine.tests.util.TestUtils;

public class TopListTests {
    @Test
    public void serializeTopList() {
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
        TestUtils.isSerializedTo(
                TopList.load(new JSONObject(json)),
                json);
    }
}
