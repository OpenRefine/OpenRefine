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

package com.google.refine.expr.functions.arrays;

import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.expr.ParsingException;
import com.google.refine.util.TestUtils;

public class ReverseTests extends RefineTest {

    @Test
    public void reverseJsonArray() throws ParsingException {
        String[] test = { "'[2,1,3]'.parseJson().reverse().toString()", "[3, 1, 2]" };
        parseEval(bindings, test);
        String[] test1 = { "'[2,null,3]'.parseJson().reverse().toString()", "[3, null, 2]" };
        parseEval(bindings, test1);
    }

    @Test
    public void reverseArray() throws ParsingException {
        String[] test = { "[2,1,3].reverse().toString()", "[3, 1, 2]" };
        parseEval(bindings, test);
        String[] test1 = { "[2,null,3].reverse().toString()", "[3, null, 2]" };
        parseEval(bindings, test1);

        String[] test2 = { "['z','b','c','a'].reverse().toString()", "[a, c, b, z]" };
        parseEval(bindings, test2);
        String[] test3 = { "['z',null,'c','a'].reverse().toString()", "[a, c, null, z]" };
        parseEval(bindings, test3);
    }

}
