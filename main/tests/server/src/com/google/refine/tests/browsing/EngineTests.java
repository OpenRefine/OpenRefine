package com.google.refine.tests.browsing;

import static org.mockito.Mockito.mock;
import org.testng.annotations.Test;

import com.google.refine.browsing.Engine;
import com.google.refine.model.Project;
import com.google.refine.tests.util.TestUtils;

// TODO Engine and engine config should be separated
// create an EngineConfig class that can be used in operations directly (to avoid manipulating JSONObject)

public class EngineTests {
    @Test
    public void serializeEngine() {
        Project project = mock(Project.class);
        Engine engine = new Engine(project);
        TestUtils.isSerializedTo(engine, "{\"mode\":\"row-based\",\"facets\":[]}");
    }
}
