
package org.openrefine.model.local.util;

import org.testng.Assert;
import org.testng.annotations.Test;

public class QueryTreeTests {

    @Test
    public void testToString() {
        QueryTree SUT = new QueryTree(1, "root",
                new QueryTree(2, "child",
                        new QueryTree(3, "first",
                                new QueryTree(4, "leaf")),
                        new QueryTree(5, "second",
                                new QueryTree(6, "branch",
                                        new QueryTree(7, "otherLeaf")))));

        String expectedRepresentation = "" +
                "└─root (1)\n" +
                "  └─child (2)\n" +
                "    ├─first (3)\n" +
                "    │ └─leaf (4)\n" +
                "    └─second (5)\n" +
                "      └─branch (6)\n" +
                "        └─otherLeaf (7)";
        Assert.assertEquals(SUT.toString(), expectedRepresentation);
    }
}
