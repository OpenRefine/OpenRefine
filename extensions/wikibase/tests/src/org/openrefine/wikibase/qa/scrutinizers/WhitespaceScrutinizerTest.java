/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package org.openrefine.wikibase.qa.scrutinizers;

import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;

public class WhitespaceScrutinizerTest extends ValueScrutinizerTest {

    @Override
    public EditScrutinizer getScrutinizer() {
        return new WhitespaceScrutinizer();
    }

    @Test
    public void testDuplicateWhitespace() {
        scrutinize(Datamodel.makeStringValue("a\t b"));
        assertWarningsRaised(WhitespaceScrutinizer.duplicateWhitespaceType);
    }

    @Test
    public void testNonPrintableChars() {
        scrutinize(Datamodel.makeStringValue("c\u0003"));
        assertWarningsRaised(WhitespaceScrutinizer.nonPrintableCharsType);
    }

    @Test
    public void testNoIssue() {
        scrutinize(Datamodel.makeStringValue("a b"));
        assertNoWarningRaised();
    }

    @Test
    public void testMultipleIssues() {
        scrutinize(Datamodel.makeStringValue("a\t b\u0003"));
        assertWarningsRaised(WhitespaceScrutinizer.duplicateWhitespaceType, WhitespaceScrutinizer.nonPrintableCharsType);
    }

    @Test
    public void testMonolingualTextValue() {
        scrutinizeLabel(Datamodel.makeMonolingualTextValue("a  b", "fr"));
        assertWarningsRaised(WhitespaceScrutinizer.duplicateWhitespaceType);
    }
}
