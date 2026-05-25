/*

Copyright 2025, OpenRefine contributors
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
 * Neither the name of the copyright holder nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

package com.google.refine.browsing.facets;

import java.io.Serializable;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.browsing.RowFilter;
import com.google.refine.browsing.facets.TextSearchFacet.TextSearchFacetConfig;
import com.google.refine.model.Project;

public class TextSearchFacetTests extends RefineTest {

    private Project sampleProject() {
        return createProject(
                "text-search-facet-test",
                new String[] { "name" },
                new Serializable[][] {
                        { "Hello World" },
                        { "hello there" },
                        { "Foo Bar" },
                        { "foobar" },
                        { null }
                });
    }

    private TextSearchFacetConfig makeConfig(String query, String mode, boolean caseSensitive, boolean invert) {
        TextSearchFacetConfig config = new TextSearchFacetConfig();
        config._name = "name";
        config._columnName = "name";
        config._query = query;
        config._mode = mode;
        config._caseSensitive = caseSensitive;
        config._invert = invert;
        return config;
    }

    private boolean[] applyFilter(Project project, TextSearchFacetConfig config) {
        TextSearchFacet facet = config.apply(project);
        RowFilter filter = facet.getRowFilter(project);
        Assert.assertNotNull(filter, "Expected a non-null row filter for the given config");
        boolean[] matched = new boolean[project.rows.size()];
        for (int i = 0; i < project.rows.size(); i++) {
            matched[i] = filter.filterRow(project, i, project.rows.get(i));
        }
        return matched;
    }

    @Test
    public void testPlainSubstringCaseInsensitive() {
        Project project = sampleProject();
        boolean[] matched = applyFilter(project, makeConfig("hello", "text", false, false));
        Assert.assertEquals(matched, new boolean[] { true, true, false, false, false });
    }

    @Test
    public void testPlainSubstringCaseSensitive() {
        Project project = sampleProject();
        boolean[] matched = applyFilter(project, makeConfig("Hello", "text", true, false));
        Assert.assertEquals(matched, new boolean[] { true, false, false, false, false });
    }

    @Test
    public void testRegexCaseInsensitive() {
        Project project = sampleProject();
        boolean[] matched = applyFilter(project, makeConfig("^foo", "regex", false, false));
        Assert.assertEquals(matched, new boolean[] { false, false, true, true, false });
    }

    @Test
    public void testRegexCaseSensitive() {
        Project project = sampleProject();
        boolean[] matched = applyFilter(project, makeConfig("^foo", "regex", true, false));
        Assert.assertEquals(matched, new boolean[] { false, false, false, true, false });
    }

    @Test
    public void testInvertFlag() {
        Project project = sampleProject();
        boolean[] matched = applyFilter(project, makeConfig("hello", "text", false, true));
        Assert.assertEquals(matched, new boolean[] { false, false, true, true, true });
    }

    @Test
    public void testEmptyQueryReturnsNullFilter() {
        Project project = sampleProject();
        TextSearchFacet facet = makeConfig("", "text", false, false).apply(project);
        Assert.assertNull(facet.getRowFilter(project));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidRegexThrows() {
        Project project = sampleProject();
        makeConfig("[", "regex", false, false).apply(project);
    }

    @Test
    public void testMissingColumnReturnsNullFilter() {
        Project project = sampleProject();
        TextSearchFacetConfig config = makeConfig("hello", "text", false, false);
        config._columnName = "does-not-exist";
        TextSearchFacet facet = config.apply(project);
        Assert.assertNull(facet.getRowFilter(project));
    }
}
