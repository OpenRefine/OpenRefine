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

import org.openrefine.wikibase.qa.ConstraintFetcher;
import org.openrefine.wikibase.testing.TestingData;
import org.openrefine.wikibase.updates.TermedStatementEntityEdit;
import org.openrefine.wikibase.updates.ItemEditBuilder;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.implementation.StatementImpl;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SingleValueScrutinizerTest extends ScrutinizerTest {

    public static final String SINGLE_VALUE_CONSTRAINT_QID = "Q19474404";

    public static PropertyIdValue propertyIdValue = Datamodel.makeWikidataPropertyIdValue("P21");
    public static Value value1 = Datamodel.makeWikidataItemIdValue("Q6581072");
    public static Value value2 = Datamodel.makeWikidataItemIdValue("Q6581097");

    public static ItemIdValue entityIdValue = Datamodel.makeWikidataItemIdValue(SINGLE_VALUE_CONSTRAINT_QID);

    @Override
    public EditScrutinizer getScrutinizer() {
        return new SingleValueScrutinizer();
    }

    @Test
    public void testTrigger() {
        ItemIdValue idA = TestingData.existingId;
        Snak snak1 = Datamodel.makeValueSnak(propertyIdValue, value1);
        Snak snak2 = Datamodel.makeValueSnak(propertyIdValue, value2);
        Statement statement1 = new StatementImpl("P21", snak1, idA);
        Statement statement2 = new StatementImpl("P21", snak2, idA);
        TermedStatementEntityEdit update = new ItemEditBuilder(idA)
                .addStatement(add(statement1))
                .addStatement(add(statement2))
                .build();

        List<Statement> statementList = constraintParameterStatementList(entityIdValue, new ArrayList<>());

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, SINGLE_VALUE_CONSTRAINT_QID)).thenReturn(statementList);
        setFetcher(fetcher);
        scrutinize(update);
        assertWarningsRaised(SingleValueScrutinizer.type);
    }

    @Test
    public void testNoIssue() {
        ItemIdValue idA = TestingData.existingId;
        Snak snak1 = Datamodel.makeValueSnak(propertyIdValue, value1);
        Statement statement1 = new StatementImpl("P21", snak1, idA);
        TermedStatementEntityEdit updateA = new ItemEditBuilder(idA)
                .addStatement(add(statement1))
                .build();

        List<Statement> statementList = constraintParameterStatementList(entityIdValue, new ArrayList<>());

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, SINGLE_VALUE_CONSTRAINT_QID)).thenReturn(statementList);
        setFetcher(fetcher);
        scrutinize(updateA);
        assertNoWarningRaised();
    }
}
