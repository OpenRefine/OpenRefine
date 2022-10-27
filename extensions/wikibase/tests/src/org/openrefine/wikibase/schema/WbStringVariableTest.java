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

package org.openrefine.wikibase.schema;

import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

import com.google.refine.model.Cell;

public class WbStringVariableTest extends WbVariableTest<StringValue> {

    @Override
    public WbVariableExpr<StringValue> initVariableExpr() {
        return new WbStringVariable();
    }

    @Test
    public void testEmpty() {
        isSkipped("");
    }

    /**
     * This should not normally happen: cell values should never be null (only whole cells can be null). But better safe
     * than sorry!
     */
    @Test
    public void testNullStringValue() {
        isSkipped((String) null);
    }

    @Test
    public void testNullCell() {
        isSkipped((Cell) null);
    }

    @Test
    public void testSimpleString() {
        evaluatesTo(Datamodel.makeStringValue("apfelstrudel"), "apfelstrudel");
    }

    /**
     * The evaluator cleans up leading and trailing whitespace, but not duplicate spaces
     */
    @Test
    public void testTrailingWhitespace() {
        evaluatesTo(Datamodel.makeStringValue("dirty"), "dirty \t");
    }

    /**
     * Test that integers are correctly converted to strings
     */
    @Test
    public void testInteger() {
        evaluatesTo(Datamodel.makeStringValue("45"), new Cell(45, null));
    }

    /**
     * Test that floating point numbers with no decimal part are also converted
     */
    @Test
    public void testDoubleInteger() {
        evaluatesTo(Datamodel.makeStringValue("45"), new Cell(45.0, null));
    }

    /**
     * Test that large doubles are correctly converted to strings
     */
    @Test
    public void testLargeDouble() {
        evaluatesTo(Datamodel.makeStringValue("14341937500"), new Cell(14341937500d, null));
    }

    /**
     * Test that large doubles are correctly converted to strings
     */
    @Test
    public void testLong() {
        evaluatesTo(Datamodel.makeStringValue("14341937500"), new Cell(14341937500L, null));
    }

    @Test
    public void testLeadingWhitespace() {
        evaluatesTo(Datamodel.makeStringValue("dirty"), " dirty");
    }

    @Test
    public void testDoubleWhitespace() {
        evaluatesTo(Datamodel.makeStringValue("very  dirty"), "very  dirty");
    }
}
