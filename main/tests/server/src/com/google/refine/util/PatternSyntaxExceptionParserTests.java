/*

Copyright 2017, Owen Stephens.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of the copyright holder nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.util;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;

public class PatternSyntaxExceptionParserTests extends RefineTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Test
    public void unmatchedOpeningParenthesisTest() {
        String s = "(abc";
        try {
            Pattern.compile(s);
            Assert.assertTrue(false, "Test pattern successfully compiled when it should fail");
        } catch (PatternSyntaxException err) {
            PatternSyntaxExceptionParser e = new PatternSyntaxExceptionParser(err);
            Assert.assertEquals(e.getUserMessage(),
                    "The regular expression is missing a closing ')' character.");
        }
    }

    @Test
    public void unmatchedClosingParenthesisTest() {
        String s = "abc)";
        try {
            Pattern.compile(s);
            Assert.assertTrue(false, "Test pattern successfully compiled when it should fail");
        } catch (PatternSyntaxException err) {
            PatternSyntaxExceptionParser e = new PatternSyntaxExceptionParser(err);
            Assert.assertEquals(e.getUserMessage(),
                    "The regular expression is missing a opening '(' character.");
        }
    }

    @Test
    public void unmatchedOpeningSquareBracketTest() {
        String s = "[abc";
        try {
            Pattern.compile(s);
        } catch (PatternSyntaxException err) {
            PatternSyntaxExceptionParser e = new PatternSyntaxExceptionParser(err);
            Assert.assertEquals(e.getUserMessage(),
                    "The regular expression is missing a closing ']' character, or has an empty pair of square brackets '[]'.");
        }
    }

    @Test
    public void danglingBackslashTest() {
        String s = "abc\\";
        try {
            Pattern.compile(s);
            Assert.assertTrue(false, "Test pattern successfully compiled when it should fail");
        } catch (PatternSyntaxException err) {
            PatternSyntaxExceptionParser e = new PatternSyntaxExceptionParser(err);
            Assert.assertEquals(e.getUserMessage(),
                    "The regular expression has a backslash '\\' at the end.");
        }
    }

    @Test
    public void unmatchedOpeningCurlyBracketTest() {
        String s = "abc{3";
        try {
            Pattern.compile(s);
        } catch (PatternSyntaxException err) {
            PatternSyntaxExceptionParser e = new PatternSyntaxExceptionParser(err);
            Assert.assertEquals(e.getUserMessage(),
                    "The regular expression is missing a closing '}' character, or has an incorrect quantifier statement in curly brackets '{}'.");
        }
    }

    @Test
    public void illegalQuantifierStatement() {
        String s = "abc{";
        try {
            Pattern.compile(s);
        } catch (PatternSyntaxException err) {
            PatternSyntaxExceptionParser e = new PatternSyntaxExceptionParser(err);
            Assert.assertEquals(e.getUserMessage(),
                    "The regular expression has an incomplete or incorrect quantifier statement in curly brackets '{}'.");
        }
    }

    @Test
    public void quantifierTargetValidityTest() {
        String s = "abc+*";
        try {
            Pattern.compile(s);
            Assert.assertTrue(false, "Test pattern successfully compiled when it should fail");
        } catch (PatternSyntaxException err) {
            PatternSyntaxExceptionParser e = new PatternSyntaxExceptionParser(err);
            Assert.assertEquals(e.getUserMessage(),
                    "The regular expression has a '*','+' or '?' in the wrong place.");
        }
    }

    @Test
    public void quantifierMagnitudeTest() {
        String s = "a{4,3}";
        try {
            Pattern.compile(s);
            Assert.assertTrue(false, "Test pattern successfully compiled when it should fail");
        } catch (PatternSyntaxException err) {
            PatternSyntaxExceptionParser e = new PatternSyntaxExceptionParser(err);
            Assert.assertEquals(e.getUserMessage(),
                    "The regular expression has a quantifier statement where the minimum is larger than the maximum (e.g. {4,3}).");
        }
    }

    @Test
    public void rangeOrderTest() {
        String s = "abc[9-0]";
        try {
            Pattern.compile(s);
            Assert.assertTrue(false, "Test pattern successfully compiled when it should fail");
        } catch (PatternSyntaxException err) {
            PatternSyntaxExceptionParser e = new PatternSyntaxExceptionParser(err);
            Assert.assertEquals(e.getUserMessage(),
                    "The regular expression has a range statement which is incomplete or has the characters in the incorrect order (e.g. [9-0])");
        }
    }

}
