
package org.openrefine.model;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.history.History;

public class ProjectTests {

    Project project;
    History history;
    long id;

    @BeforeMethod
    public void setUp() {
        id = 1234L;
        history = mock(History.class);
        project = new Project(id, history);
    }

    @Test
    public void testDispose() {
        project.dispose();

        verify(history, times(1)).dispose();
    }

}
