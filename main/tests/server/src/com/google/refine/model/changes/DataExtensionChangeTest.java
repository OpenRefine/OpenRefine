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

package com.google.refine.model.changes;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.history.Change;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.util.Pool;

public class DataExtensionChangeTest extends RefineTest {

    Project project;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void SetUp()
            throws IOException, ModelException {
        project = createCSVProject(
                "reconciled\n" +
                        "some item");
    }

    @Test
    public void testApplyOldChange() throws Exception {
        Pool pool = new Pool();
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream("changes/data_extension_2.8.txt");
        LineNumberReader lineReader = new LineNumberReader(new InputStreamReader(in));
        // skip the header
        lineReader.readLine();
        lineReader.readLine();
        Change change = DataExtensionChange.load(lineReader, pool);
        change.apply(project);
        assertEquals("Wikimedia content project", project.rows.get(0).getCell(1).value);
    }

    @Test
    public void testApplyNewChange() throws Exception {
        Pool pool = new Pool();
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream("changes/data_extension_3.0.txt");
        LineNumberReader lineReader = new LineNumberReader(new InputStreamReader(in));
        // skip the header
        lineReader.readLine();
        lineReader.readLine();
        Change change = DataExtensionChange.load(lineReader, pool);
        change.apply(project);
        assertEquals("Wikimedia content project", project.rows.get(0).getCell(1).value);
    }
}
