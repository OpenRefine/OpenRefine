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

import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.refine.expr.functions.strings.MD5;
import com.google.refine.tests.util.TestUtils;

public class MD5Tests {
    @Test
    public void serializeMD5() {
        String json = "{\"description\":\"Returns the MD5 hash of o\",\"params\":\"object o\",\"returns\":\"string\"}";
        TestUtils.isSerializedTo(new MD5(), json);
    }

    @Test
    public void datatypeMD5() {
        final Properties bindings = new Properties();

        Object[] args = new Object[] { "Hello World" };
        Assert.assertEquals(new MD5().call(bindings, args), "b10a8db164e0754105b7a99be72e3fe5");

        args = new Object[] { "" };
        Assert.assertEquals(new MD5().call(bindings, args), "d41d8cd98f00b204e9800998ecf8427e");

        args = new Object[] { "true" };
        Assert.assertEquals(new MD5().call(bindings, args), "b326b5062b2f0e69046810717534cb09");

        args = new Object[] { true };
        Assert.assertEquals(new MD5().call(bindings, args), "6eecea2d58328aed389f54154b27490c");

        args = new Object[] { "5" };
        Assert.assertEquals(new MD5().call(bindings, args), "e4da3b7fbbce2345d7772b0674a318d5");

        args = new Object[] { 5 };
        Assert.assertEquals(new MD5().call(bindings, args), "ce4be10c948828d74af398c6a1cbd05f");

        args = new Object[] { "-4294967296" };
        Assert.assertEquals(new MD5().call(bindings, args), "116a205770f6012a834522b7b838c021");

        args = new Object[] { -4294967296L };
        Assert.assertEquals(new MD5().call(bindings, args), "2376c50888d841be9c3dfb53bf4d809e");

        args = new Object[] { "3.141592653589793" };
        Assert.assertEquals(new MD5().call(bindings, args), "2836dd7cd23c875da1039ba2b4b4ccd0");

        args = new Object[] { 3.141592653589793 };
        Assert.assertEquals(new MD5().call(bindings, args), "4326508c2cf285fdc4595bad0f53d3a5");

        args = new Object[] { new Object[] { "1", "2", "3" } };
        Assert.assertEquals(new MD5().call(bindings, args), "b9cf86c4fd3e91634c4d04597c976c33");

        args = new Object[] { new Object[] { 1L, 2L, 3L } };
        Assert.assertEquals(new MD5().call(bindings, args), "4aa306167163be5d7681a39df12ecb3b");
    }
}
