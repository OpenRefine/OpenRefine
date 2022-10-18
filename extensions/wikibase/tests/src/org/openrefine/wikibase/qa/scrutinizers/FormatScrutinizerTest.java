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
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FormatScrutinizerTest extends ScrutinizerTest {

    public static final String FORMAT_CONSTRAINT_QID = "Q21502404";
    public static final String FORMAT_REGEX_PID = "P1793";

    public static PropertyIdValue propertyIdValue = Datamodel.makeWikidataPropertyIdValue("P18");
    public static Value completeMatchValue = Datamodel.makeStringValue("image.png");
    public static Value noMatchValue = Datamodel.makeStringValue("image");
    public static Value incompleteMatchValue = Datamodel.makeStringValue(".jpg");
    public static String regularExpression = "(?i).+\\.(jpg|jpeg|jpe|png|svg|tif|tiff|gif|xcf|pdf|djvu|webp)";
    public static String invalidRegularExpression = "(?[A-Za-z]+)";

    public static ItemIdValue entityIdValue = Datamodel.makeWikidataItemIdValue(FORMAT_CONSTRAINT_QID);
    public static PropertyIdValue regularExpressionParameter = Datamodel.makeWikidataPropertyIdValue(FORMAT_REGEX_PID);
    public static Value regularExpressionFormat = Datamodel.makeStringValue(regularExpression);

    @Override
    public EditScrutinizer getScrutinizer() {
        return new FormatScrutinizer();
    }

    @Test
    public void testTrigger() {
        ItemIdValue idA = TestingData.existingId;
        ValueSnak value = Datamodel.makeValueSnak(propertyIdValue, noMatchValue);
        Statement statement = new StatementImpl("P18", value, idA);
        TermedStatementEntityEdit updateA = new ItemEditBuilder(idA)
                .addStatement(add(statement))
                .build();

        List<Statement> constraintDefinitions = generateFormatConstraint(regularExpression);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, FORMAT_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);
        scrutinize(updateA);
        assertWarningsRaised(FormatScrutinizer.type);
    }

    @Test
    public void testNoIssue() {
        ItemIdValue idA = TestingData.existingId;
        ValueSnak value = Datamodel.makeValueSnak(propertyIdValue, completeMatchValue);
        Statement statement = new StatementImpl("P18", value, idA);
        TermedStatementEntityEdit updateA = new ItemEditBuilder(idA)
                .addStatement(add(statement))
                .build();

        List<Statement> constraintDefinitions = generateFormatConstraint(regularExpression);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, FORMAT_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);
        scrutinize(updateA);
        assertNoWarningRaised();
    }

    @Test
    public void testIncompleteMatch() {
        ItemIdValue idA = TestingData.existingId;
        ValueSnak value = Datamodel.makeValueSnak(propertyIdValue, incompleteMatchValue);
        Statement statement = new StatementImpl("P18", value, idA);
        TermedStatementEntityEdit updateA = new ItemEditBuilder(idA)
                .addStatement(add(statement))
                .build();

        List<Statement> constraintDefinitions = generateFormatConstraint(regularExpression);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, FORMAT_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);
        scrutinize(updateA);
        assertWarningsRaised(FormatScrutinizer.type);
    }

    @Test
    public void testInvalidRegex() {
        ItemIdValue idA = TestingData.existingId;
        ValueSnak value = Datamodel.makeValueSnak(propertyIdValue, incompleteMatchValue);
        Statement statement = new StatementImpl("P18", value, idA);
        TermedStatementEntityEdit updateA = new ItemEditBuilder(idA)
                .addStatement(add(statement))
                .build();

        List<Statement> constraintDefinitions = generateFormatConstraint(invalidRegularExpression);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, FORMAT_CONSTRAINT_QID)).thenReturn(constraintDefinitions);
        setFetcher(fetcher);
        scrutinize(updateA);
        assertNoWarningRaised();
    }

    protected List<Statement> generateFormatConstraint(String regex) {
        Snak qualifierSnak = Datamodel.makeValueSnak(regularExpressionParameter, Datamodel.makeStringValue(regex));
        List<Snak> qualifierSnakList = Collections.singletonList(qualifierSnak);
        SnakGroup qualifierSnakGroup = Datamodel.makeSnakGroup(qualifierSnakList);
        List<SnakGroup> constraintQualifiers = Collections.singletonList(qualifierSnakGroup);
        return constraintParameterStatementList(entityIdValue, constraintQualifiers);
    }

}
