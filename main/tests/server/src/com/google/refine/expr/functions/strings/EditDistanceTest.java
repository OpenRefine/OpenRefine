
package com.google.refine.expr.functions.strings;
import com.google.refine.RefineTest;
import org.junit.Assert;
import org.testng.annotations.Test;
public class EditDistanceTest extends RefineTest{
    @Test
    public void testDifferentStrings{
        Assert.assertTrue(
        invoke("editDistance", "New York","newyork"),3);
    }
}