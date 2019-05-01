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
package com.google.refine.tests.expr.functions.strings;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.SHA1;
import com.google.refine.tests.util.TestUtils;

public class SHA1Tests {
    @Test
    public void serializeSHA1() {
        String json = "{\"description\":\"Returns the SHA-1 hash of o\",\"params\":\"object o\",\"returns\":\"string\"}";
        TestUtils.isSerializedTo(new SHA1(), json);
    }

    @Test
    public void datatypeSHA1() {
        final Properties bindings = new Properties();

        Object[] args = new Object[] { "Hello World" };
        Assert.assertEquals(new SHA1().call(bindings, args), "0a4d55a8d778e5022fab701977c5d840bbc486d0");

        args = new Object[] { "" };
        Assert.assertEquals(new SHA1().call(bindings, args), "da39a3ee5e6b4b0d3255bfef95601890afd80709");

        args = new Object[] { "true" };
        Assert.assertEquals(new SHA1().call(bindings, args), "5ffe533b830f08a0326348a9160afafc8ada44db");

        args = new Object[] { true };
        Assert.assertEquals(new SHA1().call(bindings, args), "6b5508b9d9c25729d877cb0e59d812307befcc95");

        args = new Object[] { "5" };
        Assert.assertEquals(new SHA1().call(bindings, args), "ac3478d69a3c81fa62e60f5c3696165a4e5e6ac4");

        args = new Object[] { 5 };
        Assert.assertEquals(new SHA1().call(bindings, args), "c71d99fce6568d995e19cfdc1d890892b398e470");

        args = new Object[] { "-4294967296" };
        Assert.assertEquals(new SHA1().call(bindings, args), "49eeb7a99616654ee79ed92690c69ca9536917dd");

        args = new Object[] { -4294967296L };
        Assert.assertEquals(new SHA1().call(bindings, args), "c58830256ffb11372597e4fcbfd77809ebd44872");

        args = new Object[] { "3.141592653589793" };
        Assert.assertEquals(new SHA1().call(bindings, args), "29edec2b9cb930ed550e0b10aa74aa47664b2ed2");

        args = new Object[] { 3.141592653589793 };
        Assert.assertEquals(new SHA1().call(bindings, args), "8798c27a6533f85c9a65e3cfe5768680386ba7d2");

        args = new Object[] { new Object[] { "1", "2", "3" } };
        Assert.assertEquals(new SHA1().call(bindings, args), "a2981b7cefe1618f2d37f2394e950d1cd20cc107");

        args = new Object[] { new Object[] { 1L, 2L, 3L } };
        Assert.assertEquals(new SHA1().call(bindings, args), "afb8eabf07f0181deec3976c618b49701acab83b");
    }
}
