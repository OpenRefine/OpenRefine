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

import org.openrefine.wikidata.qa.MockConstraintFetcher;
import org.openrefine.wikidata.testing.TestingData;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

public class InverseConstaintScrutinizerTest extends StatementScrutinizerTest {

    private ItemIdValue idA = TestingData.existingId;
    private ItemIdValue idB = TestingData.newIdB;
    private PropertyIdValue pidWithInverse = MockConstraintFetcher.pidWithInverse;
    private PropertyIdValue inversePid = MockConstraintFetcher.inversePid;
    private PropertyIdValue symmetricPid = MockConstraintFetcher.symmetricPid;

    @Override
    public EditScrutinizer getScrutinizer() {
        return new InverseConstraintScrutinizer();
    }

    @Test
    public void testTrigger() {
        ItemUpdate update = new ItemUpdateBuilder(idA)
                .addStatement(TestingData.generateStatement(idA, pidWithInverse, idB)).build();
        scrutinize(update);
        assertWarningsRaised(InverseConstraintScrutinizer.type);
    }
    
    @Test
    public void testSymmetric() {
        ItemUpdate update = new ItemUpdateBuilder(idA)
                .addStatement(TestingData.generateStatement(idA, symmetricPid, idB)).build();
        scrutinize(update);
        assertWarningsRaised(InverseConstraintScrutinizer.type);
    }

    @Test
    public void testNoSymmetricClosure() {
        ItemUpdate update = new ItemUpdateBuilder(idA).addStatement(TestingData.generateStatement(idA, inversePid, idB))
                .build();
        scrutinize(update);
        assertNoWarningRaised();
    }

}
