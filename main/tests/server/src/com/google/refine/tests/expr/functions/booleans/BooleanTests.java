/*

Copyright 2013, Thomas F. Morris
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
 * Neither the name of Google Inc. nor the names of its
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

package com.google.refine.tests.expr.functions.booleans;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.browsing.Engine;
import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.io.FileProjectManager;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;


public class BooleanTests extends RefineTest {

    private static String TRUTH_TABLE[][] = {
        {"and","true","true","true","true"},
        {"and","false","false","false","false"},
        {"and","true","false","false","false"},
        {"and","false","true","true","false"},

        {"or","true","true","true","true"},
        {"or","false","false","false","false"},
        {"or","true","false","false","true"},
        {"or","false","true","true","true"},

        {"xor","true","true","true","false"},
        {"xor","false","false","false","false"},
        {"xor","true","false","false","true"},
        {"xor","false","true","false","true"},
    };


    static Properties bindings;
    Project project;
    Properties options;
    JSONObject engine_config;
    Engine engine;


    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void SetUp() throws IOException, ModelException {
        bindings = new Properties();

        File dir = TestUtils.createTempDirectory("openrefine-test-workspace-dir");
        FileProjectManager.initialize(dir);
        project = new Project();
        ProjectMetadata pm = new ProjectMetadata();
        pm.setName("TNG Test Project");
        ProjectManager.singleton.registerProject(project, pm);

        int index = project.columnModel.allocateNewCellIndex();
        Column column = new Column(index,"Column A");
        project.columnModel.addColumn(index, column, true);

        options = mock(Properties.class);

        bindings.put("project", project);

        // Five rows of a's and five of 1s
        for (int i = 0; i < 10; i++) {
            Row row = new Row(1);
            row.setCell(0, new Cell(i < 5 ? "a":new Integer(1), null));
            project.rows.add(row);
        }
    }


    @AfterMethod
    public void TearDown() {
        bindings = null;
    }

    /**
     * Lookup a control function by name and invoke it with a variable number of args
     */
    private static Object invoke(String name,Object... args) {
        // registry uses static initializer, so no need to set it up
        Function function = ControlFunctionRegistry.getFunction(name);
        if (function == null) {
            throw new IllegalArgumentException("Unknown function "+name);
        }
        if (args == null) {
            return function.call(bindings,new Object[0]);
        } else {
            return function.call(bindings,args);
        }
    }

    @Test
    public void testInvalidParams() {
        for (String op : new String[] {"and","or","xor"}) {
        Assert.assertTrue(invoke(op) instanceof EvalError);
        Assert.assertTrue(invoke(op, Boolean.TRUE, Integer.valueOf(1)) instanceof EvalError);
        Assert.assertTrue(invoke(op, Integer.valueOf(1), Boolean.TRUE) instanceof EvalError);
        Assert.assertTrue(invoke(op, Boolean.TRUE,"foo") instanceof EvalError);
        Assert.assertTrue(invoke(op, "foo", Boolean.TRUE) instanceof EvalError);
        Assert.assertTrue(invoke(op, Boolean.TRUE) instanceof EvalError);
        }
        String op = "not";
        Assert.assertTrue(invoke(op) instanceof EvalError);
        Assert.assertTrue(invoke(op, Boolean.TRUE, Boolean.TRUE) instanceof EvalError);
        Assert.assertTrue(invoke(op, Integer.valueOf(1)) instanceof EvalError);
        Assert.assertTrue(invoke(op, "foo") instanceof EvalError);
     }

    @Test
    public void testBinary() {
        for (String[] test : TRUTH_TABLE) {
            String operator = test[0];
            Boolean op1 = Boolean.valueOf(test[1]);
            Boolean op2 = Boolean.valueOf(test[2]);
            Boolean op3 = Boolean.valueOf(test[3]);
            Boolean result = Boolean.valueOf(test[4]);
            Assert.assertEquals(invoke(operator, op1, op2, op3),result);
        }
        Assert.assertEquals(invoke("not", Boolean.TRUE),Boolean.FALSE);
        Assert.assertEquals(invoke("not", Boolean.FALSE),Boolean.TRUE);
    }
}
