package org.openrefine;

import org.testng.Assert;
import org.testng.annotations.Test;

public class RefineModelTests {
    /**
     * Tests that the appropriate aliases are in place to load classes
     * by their old qualified name. This ensures backwards compatibility
     * with previous versions.
     * 
     * @throws ClassNotFoundException
     */
    @Test
    public void loadChangeClassByOldName() throws ClassNotFoundException {
        Class klass = RefineModel.getClass("com.google.refine.history.Change");
        Assert.assertEquals(klass.getName(), "org.openrefine.history.Change");
    }
}
