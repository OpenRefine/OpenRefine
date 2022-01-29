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

import org.testng.Assert;
import org.testng.annotations.Test;
import com.google.refine.RefineTest;
import com.google.refine.util.TestUtils;
import com.google.refine.expr.EvalError;

public class PhoneticTests extends RefineTest {

    @Test
    public void testtoPhoneticInvalidParams() {
        Assert.assertTrue(invoke("phonetic") instanceof EvalError); // if no arguments are provided
        Assert.assertTrue(invoke("phonetic", (Object[]) null) instanceof EvalError); // if first argument(value) is null
        Assert.assertTrue(invoke("phonetic", "one", (Object[]) null) instanceof EvalError); // if second
                                                                                            // argument(encoding type)
                                                                                            // is null
        Assert.assertTrue(invoke("phonetic", "one", "other") instanceof EvalError); // if second argument(encoding type)
                                                                                    // is not a valid encoding type
        Assert.assertTrue(invoke("phonetic", "one", "metaphone3", "three") instanceof EvalError); // if more than 2
                                                                                                  // arguments are
                                                                                                  // provided
    }
}
