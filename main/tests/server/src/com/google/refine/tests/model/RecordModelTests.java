package com.google.refine.tests.model;

import org.testng.annotations.Test;

import com.google.refine.model.Project;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;

public class RecordModelTests extends RefineTest {
    @Test
    public void serializeRecordModel() {
        Project proj = createCSVProject("key,val\n"
                + "34,first\n"
                + ",second"
                );
        TestUtils.isSerializedTo(proj.recordModel, "{\"hasRecords\":true}");
    }
}
