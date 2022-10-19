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
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RestrictedPositionScrutinizerTest extends SnakScrutinizerTest {

    public static final String SCOPE_CONSTRAINT_QID = "Q53869507";
    public static final String SCOPE_CONSTRAINT_PID = "P5314";
    public static final String SCOPE_CONSTRAINT_VALUE_QID = "Q54828448";
    public static final String SCOPE_CONSTRAINT_QUALIFIER_QID = "Q54828449";

    public static PropertyIdValue propertyIdValue = Datamodel.makeWikidataPropertyIdValue("P22");
    public static PropertyIdValue propertyScopeParameter = Datamodel.makeWikidataPropertyIdValue(SCOPE_CONSTRAINT_PID);
    public static ItemIdValue entityIdValue = Datamodel.makeWikidataItemIdValue(SCOPE_CONSTRAINT_QID);
    public static ItemIdValue asMainSnak = Datamodel.makeWikidataItemIdValue(SCOPE_CONSTRAINT_VALUE_QID);
    public static ItemIdValue asQualifier = Datamodel.makeWikidataItemIdValue(SCOPE_CONSTRAINT_QUALIFIER_QID);

    @Override
    public EditScrutinizer getScrutinizer() {
        return new RestrictedPositionScrutinizer();
    }

    @Test
    public void testTriggerMainSnak() {
        ItemIdValue idA = TestingData.existingId;
        Snak mainSnak = Datamodel.makeSomeValueSnak(propertyIdValue);
        Statement statement = new StatementImpl("P22", mainSnak, idA);
        TermedStatementEntityEdit update = new ItemEditBuilder(idA)
                .addStatement(add(statement))
                .build();

        Snak qualifierSnak = Datamodel.makeValueSnak(propertyScopeParameter, asQualifier);
        List<SnakGroup> constraintQualifiers = makeSnakGroupList(qualifierSnak);
        List<Statement> constraintDefinitions = constraintParameterStatementList(entityIdValue, constraintQualifiers);
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, SCOPE_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(update);
        assertWarningsRaised("property-found-in-mainsnak");
    }

    @Test
    public void testNoProblem() {
        ItemIdValue idA = TestingData.existingId;
        Snak mainSnak = Datamodel.makeSomeValueSnak(propertyIdValue);
        Statement statement = new StatementImpl("P22", mainSnak, idA);
        TermedStatementEntityEdit update = new ItemEditBuilder(idA)
                .addStatement(add(statement))
                .build();

        Snak qualifierSnak = Datamodel.makeValueSnak(propertyScopeParameter, asMainSnak);
        List<SnakGroup> constraintQualifiers = makeSnakGroupList(qualifierSnak);
        List<Statement> constraintDefinitions = constraintParameterStatementList(entityIdValue, constraintQualifiers);
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, SCOPE_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(update);
        assertNoWarningRaised();
    }

    @Test
    public void testNotRestricted() {
        ItemIdValue idA = TestingData.existingId;
        Snak mainSnak = Datamodel.makeSomeValueSnak(propertyIdValue);
        Statement statement = new StatementImpl("P22", mainSnak, idA);
        TermedStatementEntityEdit update = new ItemEditBuilder(idA)
                .addStatement(add(statement))
                .build();

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, SCOPE_CONSTRAINT_QID)).thenReturn(new ArrayList<>());
        setFetcher(fetcher);

        scrutinize(update);
        assertNoWarningRaised();
    }

    @Test
    public void testTriggerReference() {
        ItemIdValue idA = TestingData.existingId;
        Snak snak = Datamodel.makeValueSnak(propertyIdValue, idA);
        List<SnakGroup> snakGroups = Collections
                .singletonList(Datamodel.makeSnakGroup(Collections.singletonList(snak)));
        Statement statement = Datamodel.makeStatement(
                TestingData.generateStatement(idA, propertyIdValue, idA).getClaim(),
                Collections.singletonList(Datamodel.makeReference(snakGroups)), StatementRank.NORMAL, "");
        TermedStatementEntityEdit update = new ItemEditBuilder(idA)
                .addStatement(add(statement))
                .build();

        Snak qualifierSnak = Datamodel.makeValueSnak(propertyScopeParameter, asMainSnak);
        List<SnakGroup> constraintQualifiers = makeSnakGroupList(qualifierSnak);
        List<Statement> constraintDefinitions = constraintParameterStatementList(entityIdValue, constraintQualifiers);
        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, SCOPE_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(update);
        assertWarningsRaised("property-found-in-reference");
    }

}
