package com.google.refine.tests.preference;

import java.io.IOException;

import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.refine.preference.TopList;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.ParsingUtilities;

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
        TestUtils.isSerializedTo(
                ParsingUtilities.mapper.readValue(json, TopList.class),
                json);
    }
}
