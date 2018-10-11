package com.google.refine.tests.browsing;

import static org.mockito.Mockito.mock;
import org.testng.annotations.Test;

import com.google.refine.browsing.Engine;
import com.google.refine.model.Project;
import com.google.refine.tests.util.TestUtils;


public class EngineTests {
    @Test
    public void serializeEngine() {
        Project project = mock(Project.class);
        Engine engine = new Engine(project);
        TestUtils.isSerializedTo(engine, "{\"engine-mode\":\"row-based\",\"facets\":[]}");
    }
}
