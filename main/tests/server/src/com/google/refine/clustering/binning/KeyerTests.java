/*
Copyright 2013 Thomas F. Morris & other contributors
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
    * Neither the names of the project or its
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

package com.google.refine.clustering.binning;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.clustering.binning.FingerprintKeyer;
import com.google.refine.clustering.binning.Keyer;
import com.google.refine.clustering.binning.NGramFingerprintKeyer;


public class KeyerTests extends RefineTest {

    private static Keyer keyer;
    
    private static final String[][] testStrings = {
        {"the multi multi word test","multi test the word"},
        {" école ÉCole ecoLe ", "ecole"},
        {"a b c d","a b c d"},
        {" d c b a ","a b c d"},
        {"\tABC \t DEF ","abc def"}, // test leading and trailing whitespace
        {"bbb\taaa","aaa bbb"},
        {"",""},
        {"",""},
        {"",""},
    };
    
    private static final String[][] testNGramStrings = {
        {"abcdefg","abbccddeeffg"},
        {"",""}, //TODO: add more test cases
        {"",""},
        {"",""},
    };
    
    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }
    
    @BeforeMethod
    public void SetUp() {
         keyer = new FingerprintKeyer();
    }

    @AfterMethod
    public void TearDown() {
        keyer = null;
    }
    
    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testInvalidParams() {        
        keyer.key("test",(Object[])new String[]{"foo"});
    }
    
    @Test
    public void testFingerprintKeyer() {
        for (String[] ss : testStrings) {
            Assert.assertEquals(ss.length,2,"Invalid test"); // Not a valid test
            Assert.assertEquals(keyer.key(ss[0]),ss[1],
                    "Fingerprint for string: " + ss[0] + " failed");
        }
    }
    
    @Test
    public void testNGramKeyer() {    
        keyer = new NGramFingerprintKeyer();
        for (String[] ss : testNGramStrings) {
            Assert.assertEquals(ss.length,2,"Invalid test"); // Not a valid test
            Assert.assertEquals(keyer.key(ss[0]),ss[1],
                    "Fingerprint for string: " + ss[0] + " failed");
        }
    }
    
 

}
