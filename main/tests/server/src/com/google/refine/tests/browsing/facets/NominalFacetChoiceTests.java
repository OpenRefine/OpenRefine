package com.google.refine.tests.browsing.facets;

import org.testng.annotations.Test;

import com.google.refine.browsing.DecoratedValue;
import com.google.refine.browsing.facets.NominalFacetChoice;
import com.google.refine.tests.util.TestUtils;

public class NominalFacetChoiceTests {
    @Test
    public void serializeNominalFacetChoice() {
        DecoratedValue value = new DecoratedValue("some string", "some string");
        NominalFacetChoice choice = new NominalFacetChoice(value);
        choice.count = 3;
        choice.selected = true;
        TestUtils.isSerializedTo(choice, "{"
                + "\"v\":"
                + "   {\"v\":\"some string\","
                + "    \"l\":\"some string\"},"
                + "\"c\":3,"
                + "\"s\":true}");
    }
}
