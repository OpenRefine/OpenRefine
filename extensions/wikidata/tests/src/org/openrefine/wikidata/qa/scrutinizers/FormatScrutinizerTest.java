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
package org.openrefine.wikidata.qa.scrutinizers;

import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;

public class FormatScrutinizerTest extends ValueScrutinizerTest {

    @Override
    public EditScrutinizer getScrutinizer() {
        return new FormatScrutinizer();
    }

    @Test
    public void testTrigger() {
        scrutinize(Datamodel.makeStringValue("not a number"));
        assertWarningsRaised(FormatScrutinizer.type);
    }

    @Test
    public void testNoIssue() {
        scrutinize(Datamodel.makeStringValue("1234"));
        assertNoWarningRaised();
    }

    @Test
    public void testIncompleteMatch() {
        scrutinize(Datamodel.makeStringValue("42 is a number"));
        assertWarningsRaised(FormatScrutinizer.type);
    }

}
