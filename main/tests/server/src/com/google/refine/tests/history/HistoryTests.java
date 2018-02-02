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

package com.google.refine.tests.history;

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
import com.google.refine.history.History;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Project;
import com.google.refine.model.medadata.ProjectMetadata;
import com.google.refine.tests.RefineTest;


public class HistoryTests extends RefineTest {
    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    //System Under Test
    History SUT;

    //dependencies
    Project proj;
    ProjectManager projectManager;

    @BeforeMethod
    public void SetUp(){
        projectManager = mock(ProjectManager.class);
        ProjectManager.singleton = projectManager;
        proj = new Project();
        SUT = new History(proj);
    }

    @AfterMethod
    public void TearDown(){
        SUT = null;
        proj = null;
    }

    @Test
    public void canAddEntry(){
        //local dependencies
        HistoryEntry entry = mock(HistoryEntry.class);
        Project project = mock(Project.class);
        ProjectMetadata projectMetadata = mock(ProjectMetadata.class);

        when(projectManager.getProject(Mockito.anyLong())).thenReturn(project);
        when(projectManager.getProjectMetadata(Mockito.anyLong())).thenReturn(projectMetadata);

        SUT.addEntry(entry);

        verify(projectManager, times(1)).getProject(Mockito.anyLong());
        verify(entry, times(1)).apply(project);
        verify(projectMetadata, times(1)).updateModified();
        Assert.assertEquals(SUT.getLastPastEntries(1).get(0), entry);
    }
}
