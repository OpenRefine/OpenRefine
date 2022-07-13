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

public class UniquesTests extends RefineTest {

    @Test
    public void uniquesJsonArray() throws ParsingException {
        String[] test = { "'{a:[2,1,1,3]}'.parseJson().a.uniques().toString()", "[2, 1, 3]" };
        parseEval(bindings, test);
        String[] test1 = { "'[2,2,null,null,3,3]'.parseJson().uniques().toString()", "[2, null, 3]" };
        parseEval(bindings, test1);
    }

    @Test
    public void uniquesArray() throws ParsingException {
        String[] test = { "[2,1,1,3].uniques().toString()", "[2, 1, 3]" };
        parseEval(bindings, test);
        String[] test1 = { "[2,2,null,null,3,3,3].uniques().toString()", "[2, null, 3]" };
        parseEval(bindings, test1);

        String[] test2 = { "['z','b','c','c','a'].uniques().toString()", "[z, b, c, a]" };
        parseEval(bindings, test2);
        String[] test3 = { "['z','z',null,'c','a'].uniques().toString()", "[z, null, c, a]" };
        parseEval(bindings, test3);

        String[] test4 = { "[toDate(2020), '2018-03-02'.toDate(), toDate(2020)].uniques().toString()",
                "[2020-01-01T00:00Z, 2018-03-02T00:00Z]" };
        parseEval(bindings, test4);

        String[] test5 = { "[null,null,null].uniques().toString()", "[null]" };
        parseEval(bindings, test5);
    }

    @Test
    public void uniquesMixedArray() throws ParsingException {
        String[] test = { "[2,1.0,3,1.0].uniques().toString()", "[2, 1.0, 3]" };
        parseEval(bindings, test);
        String[] test1 = { "[2,'a',3,3,'a'].uniques().toString()", "[2, a, 3]" };
        parseEval(bindings, test1);
    }

}
