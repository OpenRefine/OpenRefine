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
import org.wikidata.wdtk.datamodel.interfaces.Value;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DistinctValuesScrutinizerTest extends StatementScrutinizerTest {

    public static String DISTINCT_VALUES_CONSTRAINT_QID = "Q21502410";

    public static PropertyIdValue propertyIdValue = Datamodel.makeWikidataPropertyIdValue("P163");
    public static Value value1 = Datamodel.makeWikidataItemIdValue("Q41673");
    public static Value value2 = Datamodel.makeWikidataItemIdValue("Q43175");

    public static ItemIdValue entityIdValue = Datamodel.makeWikidataItemIdValue(DISTINCT_VALUES_CONSTRAINT_QID);

    @Override
    public EditScrutinizer getScrutinizer() {
        return new DistinctValuesScrutinizer();
    }

    @Test
    public void testTrigger() {
        ItemIdValue idA = TestingData.existingId;
        Snak mainSnak = Datamodel.makeValueSnak(propertyIdValue, value1);
        Statement statement1 = new StatementImpl("P163", mainSnak, idA);
        Statement statement2 = new StatementImpl("P163", mainSnak, idA);

        TermedStatementEntityEdit update = new ItemEditBuilder(idA)
                .addStatement(add(statement1))
                .addStatement(add(statement2))
                .build();

        List<SnakGroup> constraintQualifiers = new ArrayList<>();
        List<Statement> constraintDefinitions = constraintParameterStatementList(entityIdValue, constraintQualifiers);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, DISTINCT_VALUES_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(update);
        assertWarningsRaised(DistinctValuesScrutinizer.type);
    }

    @Test
    public void testDeletedStatement() {
        ItemIdValue idA = TestingData.existingId;
        Snak mainSnak = Datamodel.makeValueSnak(propertyIdValue, value1);
        Statement statement1 = new StatementImpl("P163", mainSnak, idA);
        Statement statement2 = new StatementImpl("P163", mainSnak, idA);

        TermedStatementEntityEdit update = new ItemEditBuilder(idA)
                .addStatement(delete(statement1))
                .addStatement(delete(statement2))
                .build();

        List<SnakGroup> constraintQualifiers = new ArrayList<>();
        List<Statement> constraintDefinitions = constraintParameterStatementList(entityIdValue, constraintQualifiers);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, DISTINCT_VALUES_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(update);
        assertNoWarningRaised();
    }

    @Test
    public void testNoIssue() {
        ItemIdValue idA = TestingData.existingId;
        Snak snak1 = Datamodel.makeValueSnak(propertyIdValue, value1);
        Snak snak2 = Datamodel.makeValueSnak(propertyIdValue, value2);
        Statement statement1 = new StatementImpl("P163", snak1, idA);
        Statement statement2 = new StatementImpl("P163", snak2, idA);

        TermedStatementEntityEdit update = new ItemEditBuilder(idA)
                .addStatement(add(statement1))
                .addStatement(add(statement2))
                .build();

        List<SnakGroup> constraintQualifiers = new ArrayList<>();
        List<Statement> constraintDefinitions = constraintParameterStatementList(entityIdValue, constraintQualifiers);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, DISTINCT_VALUES_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(update);
        assertNoWarningRaised();
    }
}
