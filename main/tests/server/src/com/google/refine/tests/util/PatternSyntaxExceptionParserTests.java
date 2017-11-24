/*

Copyright 2010, Google Inc.
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
    * Neither the name of Google Inc. nor the names of its
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

package com.google.refine.tests.util;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.google.refine.tests.RefineTest;
import com.google.refine.util.PatternSyntaxExceptionParser;

public class PatternSyntaxExceptionParserTests extends RefineTest {
    
    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }
    
/* 
Potential errors and error messages from PatternSyntaxException
groupopen:"Unmatched opening parenthesis."
    Unclosed group near index 1 ( ^
groupclose:"Unmatched closing parenthesis."
    Unmatched closing ')' )
setopen:"Unmatched opening square bracket."
    Unclosed character class near index 0 [ ^
quanttarg:"Invalid target for quantifier."
    Dangling meta character '+' near index 0 +{4} ^
    Dangling meta character '*' near index 0 * ^
    Dangling meta character '?' near index 0 ? ^
esccharopen:"Dangling backslash."
    Unexpected internal error near index 1 \ ^
quantrev:"Quantifier minimum is greater than maximum."
    Illegal repetition range near index 5 a{3,2} ^
rangerev:"Range values reversed. Start char is greater than end char."
    Illegal character range near index 3 [9-0] ^
esccharbad:"Invalid escape sequence."
    Illegal control escape sequence: \c
    Illegal/unsupported escape sequence: \g \i \j \l \m \o \q \y
    \k is not followed by '<' for named capturing group: \k
    Unknown character property name {}: \p
    Illegal Unicode escape sequence: {backslash}u
    Illegal hexadecimal escape sequence: \x
    Illegal octal escape sequence: \0
invalidnamegroup:
    named capturing group is missing trailing '>' near index 5 (?<as?>a) ^
*/

    @Test
    public void unmatchedOpeningParenthesisTest(){
        String s = "(abc";
        try {
            Pattern pattern = Pattern.compile(s);
            Assert.assertTrue(false,"Test pattern successfully compiled when it should fail");
        } catch (PatternSyntaxException err) {
            PatternSyntaxExceptionParser e = new PatternSyntaxExceptionParser(err);
            Assert.assertEquals(e.getUserMessage(),
                                "The regular expression is missing a closing ')' character.");
        }
    }

    @Test
    public void unmatchedClosingParenthesisTest(){
        String s = "abc)";
        try {
            Pattern pattern = Pattern.compile(s);
            Assert.assertTrue(false,"Test pattern successfully compiled when it should fail");
        } catch (PatternSyntaxException err) {
            PatternSyntaxExceptionParser e = new PatternSyntaxExceptionParser(err);
            Assert.assertEquals(e.getUserMessage(),
                                "The regular expression is missing a opening '(' character.");
        }
    }

    @Test
    public void unmatchedOpeningSquareBracketTest(){
        String s = "[abc";
        try {
            Pattern pattern = Pattern.compile(s);
        } catch (PatternSyntaxException err) {
            PatternSyntaxExceptionParser e = new PatternSyntaxExceptionParser(err);
            Assert.assertEquals(e.getUserMessage(),
                                "The regular expression is missing a closing ']' character.");
        }
    }

    @Test
    public void quantifierTargetValidityTest(){
        String s = "abc+*";
        try {
            Pattern pattern = Pattern.compile(s);
            Assert.assertTrue(false,"Test pattern successfully compiled when it should fail");
        } catch (PatternSyntaxException err) {
            PatternSyntaxExceptionParser e = new PatternSyntaxExceptionParser(err);
            Assert.assertEquals(e.getUserMessage(),
                                "The regular expression has a '*','+' or '?' in the wrong place");
        }
    }

    @Test
    public void danglingBackslashTest(){
        String s = "abc\\";
        try {
            Pattern pattern = Pattern.compile(s);
            Assert.assertTrue(false,"Test pattern successfully compiled when it should fail");
        } catch (PatternSyntaxException err) {
            PatternSyntaxExceptionParser e = new PatternSyntaxExceptionParser(err);
            Assert.assertEquals(e.getUserMessage(),
                                "The regular expression has a backslash '\\' at the end.");
        }
    }

    @Test
    public void quantifierMagnitudeTest(){
        String s = "a{4,3}";
        try {
            Pattern pattern = Pattern.compile(s);
            Assert.assertTrue(false,"Test pattern successfully compiled when it should fail");
        } catch (PatternSyntaxException err) {
            PatternSyntaxExceptionParser e = new PatternSyntaxExceptionParser(err);
            Assert.assertEquals(e.getUserMessage(),
                                "The regular expression has a quantifier statement where the minimum is larger than the maximum (e.g. {4,3}).");
        }
    }

    @Test
    public void rangeOrderTest(){
        String s = "abc[9-0]";
        try {
            Pattern pattern = Pattern.compile(s);
            Assert.assertTrue(false,"Test pattern successfully compiled when it should fail");
        } catch (PatternSyntaxException err) {
            PatternSyntaxExceptionParser e = new PatternSyntaxExceptionParser(err);
            Assert.assertEquals(e.getUserMessage(),
                                "The regular expression has a range statement with the characters in the incorrect order (e.g. [9-0])");
        }
    }

/* This needs to be different to the others - see all the variations on invalid escape sequences
    @Test
    public void escapeSequenceValidityTest(){
        String s = "";
        try {
            Pattern pattern = Pattern.compile(s);
            Assert.assertTrue(false,"Test pattern successfully compiled when it should fail");
        } catch (PatternSyntaxException err) {
            PatternSyntaxExceptionParser e = new PatternSyntaxExceptionParser(err);
            Assert.assertEquals(e.getUserMessage(),
                                "Invalid escape sequence.");
        }
    }
*/
}
