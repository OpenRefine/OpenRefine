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
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QualifierCompatibilityScrutinizerTest extends StatementScrutinizerTest {

    public static final String ALLOWED_QUALIFIERS_CONSTRAINT_QID = "Q21510851";
    public static final String MANDATORY_QUALIFIERS_CONSTRAINT_QID = "Q21510856";
    public static final String ALLOWED_QUALIFIERS_CONSTRAINT_PID = "P2306";

    public static ItemIdValue allowedQualifierEntity = Datamodel.makeWikidataItemIdValue(ALLOWED_QUALIFIERS_CONSTRAINT_QID);
    public static ItemIdValue mandatoryQualifierEntity = Datamodel.makeWikidataItemIdValue(MANDATORY_QUALIFIERS_CONSTRAINT_QID);
    public static PropertyIdValue propertyParameterPID = Datamodel.makeWikidataPropertyIdValue(ALLOWED_QUALIFIERS_CONSTRAINT_PID);
    public static PropertyIdValue propertyParameterValue = Datamodel.makeWikidataPropertyIdValue("P585");

    public static PropertyIdValue allowedPropertyIdValue = Datamodel.makeWikidataPropertyIdValue("P2196");
    public static PropertyIdValue mandatoryPropertyIdValue = Datamodel.makeWikidataPropertyIdValue("P2196");
    public static PropertyIdValue qualifierProperty = Datamodel.makeWikidataPropertyIdValue("P585");
    public static PropertyIdValue disallowedQualifierProperty = Datamodel.makeWikidataPropertyIdValue("P586");

    @Override
    public EditScrutinizer getScrutinizer() {
        return new QualifierCompatibilityScrutinizer();
    }

    @Test
    public void testDisallowedQualifier() {
        ItemIdValue idA = TestingData.existingId;
        Snak mainSnak = Datamodel.makeSomeValueSnak(allowedPropertyIdValue);
        Snak qualifierSnak = Datamodel.makeSomeValueSnak(disallowedQualifierProperty);
        Statement statement = makeStatement(mainSnak, qualifierSnak);
        TermedStatementEntityEdit update = new ItemEditBuilder(idA)
                .addStatement(add(statement))
                .build();

        Snak constraintQualifierSnak = Datamodel.makeValueSnak(propertyParameterPID, propertyParameterValue);
        List<Snak> qualifierList = Collections.singletonList(constraintQualifierSnak);
        SnakGroup qualifierSnakGroup = Datamodel.makeSnakGroup(qualifierList);
        List<SnakGroup> constraintQualifiers = Collections.singletonList(qualifierSnakGroup);
        List<Statement> constraintDefinitions = constraintParameterStatementList(allowedQualifierEntity, constraintQualifiers);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(allowedPropertyIdValue, ALLOWED_QUALIFIERS_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(update);
        assertWarningsRaised(QualifierCompatibilityScrutinizer.disallowedQualifiersType);
    }

    @Test
    public void testMissingQualifier() {
        ItemIdValue idA = TestingData.existingId;
        Snak mainSnak = Datamodel.makeSomeValueSnak(mandatoryPropertyIdValue);
        Statement statement = makeStatement(mainSnak);
        TermedStatementEntityEdit update = new ItemEditBuilder(idA)
                .addStatement(add(statement))
                .build();

        Snak constraintQualifierSnak = Datamodel.makeValueSnak(propertyParameterPID, propertyParameterValue);
        List<Snak> qualifierList = Collections.singletonList(constraintQualifierSnak);
        SnakGroup qualifierSnakGroup = Datamodel.makeSnakGroup(qualifierList);
        List<SnakGroup> constraintQualifiers = Collections.singletonList(qualifierSnakGroup);
        List<Statement> constraintDefinitions = constraintParameterStatementList(mandatoryQualifierEntity, constraintQualifiers);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(mandatoryPropertyIdValue, MANDATORY_QUALIFIERS_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(update);
        assertWarningsRaised(QualifierCompatibilityScrutinizer.missingMandatoryQualifiersType);
    }

    @Test
    public void testGoodEdit() {
        ItemIdValue idA = TestingData.existingId;
        Snak mainSnak = Datamodel.makeSomeValueSnak(allowedPropertyIdValue);
        Snak qualifierSnak = Datamodel.makeSomeValueSnak(qualifierProperty);
        Statement statement = makeStatement(mainSnak, qualifierSnak);
        TermedStatementEntityEdit update = new ItemEditBuilder(idA)
                .addStatement(add(statement))
                .build();

        Snak constraintQualifierSnak = Datamodel.makeValueSnak(propertyParameterPID, propertyParameterValue);
        List<Snak> qualifierList = Collections.singletonList(constraintQualifierSnak);
        SnakGroup qualifierSnakGroup = Datamodel.makeSnakGroup(qualifierList);
        List<SnakGroup> constraintQualifiers = Collections.singletonList(qualifierSnakGroup);
        List<Statement> constraintDefinitions = constraintParameterStatementList(allowedQualifierEntity, constraintQualifiers);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(allowedPropertyIdValue, ALLOWED_QUALIFIERS_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);

        scrutinize(update);
        assertNoWarningRaised();
    }

    private Statement makeStatement(Snak mainSnak, Snak... qualifiers) {
        Claim claim = Datamodel.makeClaim(TestingData.existingId,
                mainSnak, makeQualifiers(qualifiers));
        return Datamodel.makeStatement(claim, Collections.emptyList(), StatementRank.NORMAL, "");
    }

    private List<SnakGroup> makeQualifiers(Snak[] qualifiers) {
        List<Snak> snaks = Arrays.asList(qualifiers);
        return snaks.stream().map((Snak q) -> Datamodel.makeSnakGroup(Collections.<Snak> singletonList(q)))
                .collect(Collectors.toList());
    }

}
