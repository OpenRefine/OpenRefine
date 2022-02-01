/*

Copyright 2010, Google Inc.
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

package com.google.refine.history;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.RefineTest;
import com.google.refine.model.Project;
import com.google.refine.util.TestUtils;

public class HistoryTests extends RefineTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // System Under Test
    History SUT;

    // dependencies
    Project proj;
    ProjectMetadata projectMetadata;
    ProjectManager projectManager;
    HistoryEntryManager historyEntryManager;

    @BeforeMethod
    public void SetUp() {
        projectManager = mock(ProjectManager.class);
        historyEntryManager = mock(HistoryEntryManager.class);
        ProjectManager.singleton = projectManager;

        proj = new Project();
        projectMetadata = mock(ProjectMetadata.class);

        when(projectManager.getProject(Mockito.anyLong())).thenReturn(proj);
        when(projectManager.getProjectMetadata(Mockito.anyLong())).thenReturn(projectMetadata);
        when(projectManager.getHistoryEntryManager()).thenReturn(historyEntryManager);

        SUT = new History(proj);
    }

    @AfterMethod
    public void TearDown() {
        SUT = null;
        proj = null;
    }

    @Test
    public void canAddEntry() {
        // local dependencies
        HistoryEntry entry = mock(HistoryEntry.class);

        SUT.addEntry(entry);

        verify(projectManager, times(1)).getProject(Mockito.anyLong());
        verify(entry, times(1)).apply(proj);
        verify(projectMetadata, times(1)).updateModified();
        Assert.assertEquals(SUT.getLastPastEntries(1).get(0), entry);
    }

    @Test
    public void serializeHistory() throws Exception {
        String json1 = "{\"id\":1533650900300,"
                + "\"description\":\"Reconcile cells in column organization_name to type Q43229\","
                + "\"time\":\"2018-08-07T13:57:17Z\","
                + "\"operation\":{"
                + "    \"op\":\"core/recon\","
                + "    \"description\":\"Reconcile cells in column organization_name to type Q43229\","
                + "    \"columnName\":\"organization_name\","
                + "    \"config\":{"
                + "        \"mode\":\"standard-service\","
                + "        \"service\":\"https://tools.wmflabs.org/openrefine-wikidata/en/api\","
                + "        \"identifierSpace\":\"http://www.wikidata.org/entity/\","
                + "        \"schemaSpace\":\"http://www.wikidata.org/prop/direct/\","
                + "        \"type\":{\"id\":\"Q43229\",\"name\":\"organization\"},"
                + "        \"autoMatch\":true,"
                + "        \"columnDetails\":[],"
                + "        \"limit\":0},"
                + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]}}}";
        String json1simple = "{\"id\":1533650900300,"
                + "\"description\":\"Reconcile cells in column organization_name to type Q43229\","
                + "\"time\":\"2018-08-07T13:57:17Z\"}";
        String json2 = "{\"id\":1533651586483,"
                + "\"description\":\"Edit single cell on row 94, column organization_id\","
                + "\"time\":\"2018-08-07T14:18:21Z\"}";

        String targetJson = "{\"past\":[" + json1simple + "," + json2 + "],\"future\":[]}";

        com.google.refine.history.Change dummyChange = mock(Change.class);

        HistoryEntry firstEntry = HistoryEntry.load(proj, json1);
        firstEntry.setChange(dummyChange);
        HistoryEntry secondEntry = HistoryEntry.load(proj, json2);
        secondEntry.setChange(dummyChange);
        SUT.addEntry(firstEntry);
        SUT.addEntry(secondEntry);
        TestUtils.isSerializedTo(SUT, targetJson);
    }
}
