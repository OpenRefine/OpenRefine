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

import org.openrefine.wikibase.testing.TestingData;
import org.openrefine.wikibase.updates.ItemEditBuilder;

public class NoEditsMadeScrutinizerTest extends ScrutinizerTest {

    @Override
    public EditScrutinizer getScrutinizer() {
        return new NoEditsMadeScrutinizer();
    }

    @Test
    public void testTrigger() {
        scrutinize();
        assertWarningsRaised(NoEditsMadeScrutinizer.type);
    }

    @Test
    public void testNonNull() {
        scrutinize(new ItemEditBuilder(TestingData.newIdA).addContributingRowId(123).build());
        assertNoWarningRaised();
    }

    @Test
    public void testNull() {
        scrutinize(new ItemEditBuilder(TestingData.existingId).addContributingRowId(123).build());
        assertWarningsRaised(NoEditsMadeScrutinizer.type);
    }
}
