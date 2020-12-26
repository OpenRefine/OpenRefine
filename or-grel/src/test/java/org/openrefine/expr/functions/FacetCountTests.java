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

package org.openrefine.expr.functions;

import java.io.Serializable;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.expr.EvalError;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.functions.FacetCount;
import org.openrefine.grel.FunctionTestBase;
import org.openrefine.grel.Parser;
import org.openrefine.model.Project;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class FacetCountTests extends FunctionTestBase {

    Project project = null;

    @BeforeMethod
    public void setUpProject() {
        MetaParser.registerLanguageParser("grel", "General Refine Expression Language", Parser.grelParser, "value");
        project = createProject(new String[] { "Column A" }, new Serializable[][] {
                { "a" }, { "a" }, { "a" }, { 1 }, { 1 }, { true }
        });
        bindings = new Properties();
        bindings.put("project", project);
    }

    @Test
    public void testInvalidParams() {
        Assert.assertTrue(invoke("facetCount") instanceof EvalError);
        Assert.assertTrue(invoke("facetCount", "one", "two", "three") instanceof EvalError);
        Assert.assertTrue(invoke("facetCount", "one", "bad(", "Column A") instanceof EvalError);
    }

    @Test
    public void testFacetCountString() {
        Assert.assertEquals(invoke("facetCount", "a", "value", "Column A"), Long.valueOf(3));
    }

    @Test
    public void testExpressionChange() {
        Assert.assertEquals(invoke("facetCount", new Integer(1), "value", "Column A"), Long.valueOf(2));
        Assert.assertEquals(invoke("facetCount", new Integer(2), "value+1", "Column A"), Long.valueOf(2));
    }

    @Test
    public void serializeFacetCount() {
        String json = "{\"description\":\"Returns the facet count corresponding to the given choice value\",\"params\":\"choiceValue, string facetExpression, string columnName\",\"returns\":\"number\"}";
        TestUtils.isSerializedTo(new FacetCount(), json, ParsingUtilities.defaultWriter);
    }
}
