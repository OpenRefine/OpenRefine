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

package com.google.refine.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.refine.ProjectMetadata;

public class ProjectMetadataTests {

    private String jsonSaveMode = null;
    private String jsonNonSaveMode = null;

    @BeforeSuite
    public void setUpJson() throws IOException {
        InputStream f = ProjectMetadataTests.class.getClassLoader().getResourceAsStream("example_project_metadata.json");
        jsonNonSaveMode = IOUtils.toString(f);
        f = ProjectMetadataTests.class.getClassLoader().getResourceAsStream("example_project_metadata_save_mode.json");
        jsonSaveMode = IOUtils.toString(f);
    }

    @Test
    public void serializeProjectMetadata() throws IOException {
        ProjectMetadata metadata = ParsingUtilities.mapper.readValue(jsonSaveMode, ProjectMetadata.class);
        TestUtils.isSerializedTo(metadata, jsonNonSaveMode);
        TestUtils.isSerializedTo(metadata, jsonSaveMode, true);
    }

    @Test
    public void serializeProjectMetadataInDifferentTimezone() throws JsonParseException, JsonMappingException, IOException {
        TimeZone originalTimeZone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("JST"));
            ProjectMetadata metadata = ParsingUtilities.mapper.readValue(jsonSaveMode, ProjectMetadata.class);
            TestUtils.isSerializedTo(metadata, jsonNonSaveMode);
            TestUtils.isSerializedTo(metadata, jsonSaveMode, true);
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }
}
