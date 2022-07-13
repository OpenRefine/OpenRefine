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

package com.google.refine.expr.functions.strings;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.util.TestUtils;

/**
 * Tests for the range function.
 */
public class RangeTests extends RefineTest {

    private static final Integer[] EMPTY_ARRAY = new Integer[0];

    private static final Integer[] ONE_AND_THREE = new Integer[] { 1, 3 };
    private static final Integer[] FIVE_AND_THREE = new Integer[] { 5, 3 };

    private static final Integer[] ZERO_TO_TWO = new Integer[] { 0, 1, 2 };
    private static final Integer[] ONE_TO_FOUR = new Integer[] { 1, 2, 3, 4 };
    private static final Integer[] FIVE_TO_TWO = new Integer[] { 5, 4, 3, 2 };

    private static final Integer[] NEGATIVE_ONE_TO_FOUR = new Integer[] { -1, 0, 1, 2, 3, 4 };
    private static final Integer[] ONE_TO_NEGATIVE_FOUR = new Integer[] { 1, 0, -1, -2, -3, -4 };

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Test
    public void testRangeInvalidParams() {
        // Test number of arguments
        Assert.assertTrue(invoke("range") instanceof EvalError);
        Assert.assertTrue(invoke("range", "") instanceof EvalError);
        Assert.assertTrue(invoke("range", "1, 2, 3, 4") instanceof EvalError);
        Assert.assertTrue(invoke("range", "1, 2, 3", "4") instanceof EvalError);
        Assert.assertTrue(invoke("range", "1", "2, 3, 4") instanceof EvalError);
        Assert.assertTrue(invoke("range", "1, 2", "3", "4") instanceof EvalError);
        Assert.assertTrue(invoke("range", 1, 2, 3, 4) instanceof EvalError);

        // Test invalid single string argument types
        Assert.assertTrue(invoke("range", "null") instanceof EvalError);
        Assert.assertTrue(invoke("range", "a") instanceof EvalError);

        // Test invalid single string numeric arguments
        Assert.assertTrue(invoke("range", "1,") instanceof EvalError);
        Assert.assertTrue(invoke("range", ",") instanceof EvalError);
        Assert.assertTrue(invoke("range", ",2") instanceof EvalError);
        Assert.assertTrue(invoke("range", "1.5") instanceof EvalError);
        Assert.assertTrue(invoke("range", ",12.3, 2") instanceof EvalError);

        // Test invalid double string arguments
        Assert.assertTrue(invoke("range", "1", "") instanceof EvalError);
        Assert.assertTrue(invoke("range", "", "1") instanceof EvalError);

        Assert.assertTrue(invoke("range", "1,", "2") instanceof EvalError);
        Assert.assertTrue(invoke("range", "1", "2,") instanceof EvalError);
        Assert.assertTrue(invoke("range", "1.5", "3") instanceof EvalError);
        Assert.assertTrue(invoke("range", "1", "3.5") instanceof EvalError);

        // Test invalid triple string arguments
        Assert.assertTrue(invoke("range", "", "", "") instanceof EvalError);
        Assert.assertTrue(invoke("range", "1", "", "") instanceof EvalError);
        Assert.assertTrue(invoke("range", "", "1", "") instanceof EvalError);
        Assert.assertTrue(invoke("range", "", "", "1") instanceof EvalError);
        Assert.assertTrue(invoke("range", "1", "2", "") instanceof EvalError);
        Assert.assertTrue(invoke("range", "", "1", "2") instanceof EvalError);

        Assert.assertTrue(invoke("range", "1,", "2", "1") instanceof EvalError);
        Assert.assertTrue(invoke("range", "1", "2,", "1") instanceof EvalError);
        Assert.assertTrue(invoke("range", "1", "2", "1,") instanceof EvalError);
        Assert.assertTrue(invoke("range", "1.5", "3", "1") instanceof EvalError);
        Assert.assertTrue(invoke("range", "1", "3.5", "1") instanceof EvalError);
        Assert.assertTrue(invoke("range", "1", "3,", "1.5") instanceof EvalError);

        // Test invalid numeric arguments
        Assert.assertTrue(invoke("range", 1.2) instanceof EvalError);
        Assert.assertTrue(invoke("range", 1.2, 4.5) instanceof EvalError);
        Assert.assertTrue(invoke("range", 1.2, 5, 3) instanceof EvalError);

        // Test invalid mixed arguments
        Assert.assertTrue(invoke("range", 1, "") instanceof EvalError);
        Assert.assertTrue(invoke("range", "", 1) instanceof EvalError);
        Assert.assertTrue(invoke("range", 1, "a") instanceof EvalError);
        Assert.assertTrue(invoke("range", "a", 1) instanceof EvalError);
        Assert.assertTrue(invoke("range", 1, "", "") instanceof EvalError);
        Assert.assertTrue(invoke("range", "", 1, "") instanceof EvalError);
        Assert.assertTrue(invoke("range", "", "", 1) instanceof EvalError);
        Assert.assertTrue(invoke("range", 1.5, "2", 1) instanceof EvalError);
    }

    @Test
    public void testRangeValidSingleStringParams() {
        // Test valid single string containing one arg
        Assert.assertEquals(((Integer[]) invoke("range", "3")), ZERO_TO_TWO);
        Assert.assertEquals(((Integer[]) (invoke("range", " 3  "))), ZERO_TO_TWO);

        // Test valid single string containing two args
        Assert.assertEquals(((Integer[]) (invoke("range", "1, 1"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "5, 1"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "1, 5"))), ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", "   1   ,5"))), ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", "1,      5     "))), ONE_TO_FOUR);

        // Test valid single string containing three args
        Assert.assertEquals(((Integer[]) (invoke("range", "1, 1, 0"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "1, 1, 1"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "1, 5, -1"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "1, 5, 0"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "1, 5, 1"))), ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", "1, 5, 2"))), ONE_AND_THREE);
        Assert.assertEquals(((Integer[]) (invoke("range", "5, 1, -2"))), FIVE_AND_THREE);
        Assert.assertEquals(((Integer[]) (invoke("range", "5, 1, -1"))), FIVE_TO_TWO);
        Assert.assertEquals(((Integer[]) (invoke("range", "5, 1, 0"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "5, 1, 1"))), EMPTY_ARRAY);

        Assert.assertEquals(((Integer[]) (invoke("range", "  1  , 5, 1"))), ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", "1,  5  ,1"))), ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", "1, 5,   1  "))), ONE_TO_FOUR);
    }

    @Test
    public void testRangeValidDoubleStringParams() {
        // Test valid double string containing two args
        Assert.assertEquals(((Integer[]) (invoke("range", "1", "-1"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "1", "0"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "1", "1"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "2", "1"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "-1", "1"))), new Integer[] { -1, 0 });
        Assert.assertEquals(((Integer[]) (invoke("range", "1", "5"))), ONE_TO_FOUR);

        Assert.assertEquals(((Integer[]) (invoke("range", "  -1   ", "1"))), new Integer[] { -1, 0 });
        Assert.assertEquals(((Integer[]) (invoke("range", "1", " 5  "))), ONE_TO_FOUR);

        // Test valid double string containing three args
        Assert.assertEquals(((Integer[]) (invoke("range", "-1", "5, 0"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "1", "-5, 0"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "-1", "5, -1"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "1", "-5, 1"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "-1", "5, 1"))), NEGATIVE_ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", "1", "-5, -1"))), ONE_TO_NEGATIVE_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", "-1", "5, 2"))), new Integer[] { -1, 1, 3 });
        Assert.assertEquals(((Integer[]) (invoke("range", "1", "-5, -2"))), new Integer[] { 1, -1, -3 });
        Assert.assertEquals(((Integer[]) (invoke("range", "-1", "5, 10"))), new Integer[] { -1 });
        Assert.assertEquals(((Integer[]) (invoke("range", "1", "-5, -10"))), new Integer[] { 1 });

        Assert.assertEquals(((Integer[]) (invoke("range", "-1, 5", "0"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "1, -5", "0"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "-1, 5", "-1"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "1, -5", "1"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "-1, 5", "1"))), NEGATIVE_ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", "1, -5", "-1"))), ONE_TO_NEGATIVE_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", "-1, 5", "2"))), new Integer[] { -1, 1, 3 });
        Assert.assertEquals(((Integer[]) (invoke("range", "1, -5", "-2"))), new Integer[] { 1, -1, -3 });
        Assert.assertEquals(((Integer[]) (invoke("range", "-1, 5", "10"))), new Integer[] { -1 });
        Assert.assertEquals(((Integer[]) (invoke("range", "1, -5", "-10"))), new Integer[] { 1 });

        Assert.assertEquals(((Integer[]) (invoke("range", "  -1  , 5", "1"))), NEGATIVE_ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", "-1,   5", "1"))), NEGATIVE_ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", "-1, 5", " 1   "))), NEGATIVE_ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", "  -1  ", "5, 1"))), NEGATIVE_ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", "-1", "  5  , 1"))), NEGATIVE_ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", "  -1  ", "5,    1   "))), NEGATIVE_ONE_TO_FOUR);
    }

    @Test
    public void testRangeValidTripleStringParams() {
        // Test valid triple string containing three arguments
        Assert.assertEquals(((Integer[]) (invoke("range", "-1", "5", "0"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "1", "-5", "0"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "-1", "5", "-1"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "1", "-5", "1"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "-1", "5", "1"))), NEGATIVE_ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", "1", "-5", "-1"))), ONE_TO_NEGATIVE_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", "-1", "5", "2"))), new Integer[] { -1, 1, 3 });
        Assert.assertEquals(((Integer[]) (invoke("range", "1", "-5", "-2"))), new Integer[] { 1, -1, -3 });
        Assert.assertEquals(((Integer[]) (invoke("range", "-1", "5", "10"))), new Integer[] { -1 });
        Assert.assertEquals(((Integer[]) (invoke("range", "1", "-5", "-10"))), new Integer[] { 1 });

        Assert.assertEquals(((Integer[]) (invoke("range", "  -1  , 5, 1"))), NEGATIVE_ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", "-1,   5  , 1"))), NEGATIVE_ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", "-1, 5,   1   "))), NEGATIVE_ONE_TO_FOUR);
    }

    @Test
    public void testRangeValidIntegerParams() {
        // Test valid single integer argument
        Assert.assertEquals(((Integer[]) (invoke("range", 0))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", 5))), new Integer[] { 0, 1, 2, 3, 4 });

        // Test valid double integer arguments
        Assert.assertEquals(((Integer[]) (invoke("range", -1, 5))), NEGATIVE_ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", 5, 1))), EMPTY_ARRAY);

        // Test valid triple integer arguments
        Assert.assertEquals(((Integer[]) (invoke("range", 1, 5, -1))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", 1, 5, 0))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", 5, 1, 1))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", 1, 5, 1))), ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", 1, 5, 2))), ONE_AND_THREE);
        Assert.assertEquals(((Integer[]) (invoke("range", 5, 1, -2))), FIVE_AND_THREE);
    }

    @Test
    public void testRangeValidMixedParams() {
        // Test two valid arguments, with a single string arg (containing one arg) and a single Integer arg
        Assert.assertEquals(((Integer[]) (invoke("range", "5", 1))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "1", 5))), ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", 5, "1"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", 1, "5"))), ONE_TO_FOUR);

        // Test two valid arguments, with a single string arg (containing two args) and a single Integer arg
        Assert.assertEquals(((Integer[]) (invoke("range", "1, 5", -1))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "1, 5", 0))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "1, 5", 1))), ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", "1, 5", 2))), ONE_AND_THREE);

        Assert.assertEquals(((Integer[]) (invoke("range", 1, "5, -1"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", 1, "5, 0"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", 1, "5, 1"))), ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", 1, "5, 2"))), ONE_AND_THREE);

        // Test three valid arguments, with a single string arg (containing one arg) and two Integer args
        Assert.assertEquals(((Integer[]) (invoke("range", "1", 5, -1))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "1", 5, 0))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "1", 5, 1))), ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", "1", 5, 2))), ONE_AND_THREE);

        Assert.assertEquals(((Integer[]) (invoke("range", "5", 1, 1))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "5", 1, 0))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "5", 1, -1))), FIVE_TO_TWO);
        Assert.assertEquals(((Integer[]) (invoke("range", "5", 1, -2))), FIVE_AND_THREE);

        Assert.assertEquals(((Integer[]) (invoke("range", 1, "5", -1))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", 1, "5", 0))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", 1, "5", 1))), ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", 1, "5", 2))), ONE_AND_THREE);

        Assert.assertEquals(((Integer[]) (invoke("range", 5, "1", 1))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", 5, "1", 0))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", 5, "1", -1))), FIVE_TO_TWO);
        Assert.assertEquals(((Integer[]) (invoke("range", 5, "1", -2))), FIVE_AND_THREE);

        Assert.assertEquals(((Integer[]) (invoke("range", 1, 5, "-1"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", 1, 5, "0"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", 1, 5, "1"))), ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", 1, 5, "2"))), ONE_AND_THREE);

        Assert.assertEquals(((Integer[]) (invoke("range", 5, 1, "1"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", 5, 1, "0"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", 5, 1, "-1"))), FIVE_TO_TWO);
        Assert.assertEquals(((Integer[]) (invoke("range", 5, 1, "-2"))), FIVE_AND_THREE);

        // Test three valid arguments, with two string args and one Integer arg
        Assert.assertEquals(((Integer[]) (invoke("range", "1", "5", -1))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "1", "5", 0))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "1", "5", 1))), ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", "1", "5", 2))), ONE_AND_THREE);

        Assert.assertEquals(((Integer[]) (invoke("range", "5", "1", 1))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "5", "1", 0))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "5", "1", -1))), FIVE_TO_TWO);
        Assert.assertEquals(((Integer[]) (invoke("range", "5", "1", -2))), FIVE_AND_THREE);

        Assert.assertEquals(((Integer[]) (invoke("range", "1", 5, "-1"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "1", 5, "0"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "1", 5, "1"))), ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", "1", 5, "2"))), ONE_AND_THREE);

        Assert.assertEquals(((Integer[]) (invoke("range", "5", 1, "1"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "5", 1, "0"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", "5", 1, "-1"))), FIVE_TO_TWO);
        Assert.assertEquals(((Integer[]) (invoke("range", "5", 1, "-2"))), FIVE_AND_THREE);

        Assert.assertEquals(((Integer[]) (invoke("range", 1, "5", "-1"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", 1, "5", "0"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", 1, "5", "1"))), ONE_TO_FOUR);
        Assert.assertEquals(((Integer[]) (invoke("range", 1, "5", "2"))), ONE_AND_THREE);

        Assert.assertEquals(((Integer[]) (invoke("range", 5, "1", "1"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", 5, "1", "0"))), EMPTY_ARRAY);
        Assert.assertEquals(((Integer[]) (invoke("range", 5, "1", "-1"))), FIVE_TO_TWO);
        Assert.assertEquals(((Integer[]) (invoke("range", 5, "1", "-2"))), FIVE_AND_THREE);
    }

}
