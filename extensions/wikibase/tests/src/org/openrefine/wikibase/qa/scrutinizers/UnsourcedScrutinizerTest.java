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
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UnsourcedScrutinizerTest extends StatementScrutinizerTest {

    private static final String CITATION_NEEDED_QID = "Q54554025";

    public static PropertyIdValue propertyIdValue = Datamodel.makeWikidataPropertyIdValue("P2302");
    public static PropertyIdValue referenceProperty = Datamodel.makeWikidataPropertyIdValue("P143");
    public static ItemIdValue referenceValue = Datamodel.makeWikidataItemIdValue("Q348");
    public static ItemIdValue entityIdValue = Datamodel.makeWikidataItemIdValue(CITATION_NEEDED_QID);

    @Override
    public EditScrutinizer getScrutinizer() {
        return new UnsourcedScrutinizer();
    }

    @Test
    public void testWithoutConstraint() {
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(any(), eq(CITATION_NEEDED_QID))).thenReturn(Collections.emptyList());
        setFetcher(fetcher);
        scrutinize(TestingData.generateStatement(TestingData.existingId, TestingData.matchedId));
        assertWarningsRaised(UnsourcedScrutinizer.generalType);
    }

    @Test
    public void testTrigger() {
        ItemIdValue id = TestingData.existingId;
        Snak mainSnak = Datamodel.makeSomeValueSnak(propertyIdValue);
        Statement statement = new StatementImpl("P172", mainSnak, id);
        TermedStatementEntityEdit update = new ItemEditBuilder(id).addStatement(add(statement)).build();

        List<Statement> constraintDefinitions = constraintParameterStatementList(entityIdValue, Collections.emptyList());
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, CITATION_NEEDED_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(update);
        assertWarningsRaised(UnsourcedScrutinizer.constraintItemType);
    }

    @Test
    public void testNoIssue() {
        ItemIdValue id = TestingData.existingId;
        Snak referenceSnak = Datamodel.makeValueSnak(referenceProperty, referenceValue);
        List<SnakGroup> constraintQualifiers = makeSnakGroupList(referenceSnak);
        List<Statement> itemStatementList = constraintParameterStatementList(entityIdValue, constraintQualifiers);
        Statement statement = itemStatementList.get(0);
        TermedStatementEntityEdit update = new ItemEditBuilder(id).addStatement(add(statement)).build();

        List<Statement> constraintDefinitions = constraintParameterStatementList(entityIdValue, Collections.emptyList());
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, CITATION_NEEDED_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(update);
        assertNoWarningRaised();
    }
}
