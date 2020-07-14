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

import org.openrefine.wikidata.qa.ConstraintFetcher;
import org.openrefine.wikidata.testing.TestingData;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
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
import static org.openrefine.wikidata.qa.scrutinizers.FormatScrutinizer.FORMAT_CONSTRAINT_QID;
import static org.openrefine.wikidata.qa.scrutinizers.FormatScrutinizer.FORMAT_REGEX_PID;

public class FormatScrutinizerTest extends ScrutinizerTest {

    public static PropertyIdValue propertyIdValue = Datamodel.makeWikidataPropertyIdValue("P18");
    public static Value completeMatchValue = Datamodel.makeStringValue("image.png");
    public static Value noMatchValue = Datamodel.makeStringValue("image");
    public static Value incompleteMatchValue = Datamodel.makeStringValue(".jpg");
    public static String regularExpression = "(?i).+\\.(jpg|jpeg|jpe|png|svg|tif|tiff|gif|xcf|pdf|djvu|webp)";

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
        ItemUpdate updateA = new ItemUpdateBuilder(idA).addStatement(statement).build();

        Snak qualifierSnak = Datamodel.makeValueSnak(regularExpressionParameter, regularExpressionFormat);
        List<Snak> qualifierSnakList = Collections.singletonList(qualifierSnak);
        SnakGroup qualifierSnakGroup = Datamodel.makeSnakGroup(qualifierSnakList);
        List<SnakGroup> snakGroupList = Collections.singletonList(qualifierSnakGroup);
        List<Statement> statementList = constraintParameterStatementList(entityIdValue, snakGroupList);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, FORMAT_CONSTRAINT_QID)).thenReturn(statementList);
        when(fetcher.findValues(snakGroupList, FORMAT_REGEX_PID)).thenReturn(Collections.singletonList(regularExpressionFormat));
        setFetcher(fetcher);
        scrutinize(updateA);
        assertWarningsRaised(FormatScrutinizer.type);
    }

    @Test
    public void testNoIssue() {
        ItemIdValue idA = TestingData.existingId;
        ValueSnak value = Datamodel.makeValueSnak(propertyIdValue, completeMatchValue);
        Statement statement = new StatementImpl("P18", value, idA);
        ItemUpdate updateA = new ItemUpdateBuilder(idA).addStatement(statement).build();

        Snak qualifierSnak = Datamodel.makeValueSnak(regularExpressionParameter, regularExpressionFormat);
        List<Snak> qualifierSnakList = Collections.singletonList(qualifierSnak);
        SnakGroup qualifierSnakGroup = Datamodel.makeSnakGroup(qualifierSnakList);
        List<SnakGroup> snakGroupList = Collections.singletonList(qualifierSnakGroup);
        List<Statement> statementList = constraintParameterStatementList(entityIdValue, snakGroupList);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, FORMAT_CONSTRAINT_QID)).thenReturn(statementList);
        when(fetcher.findValues(snakGroupList, FORMAT_REGEX_PID)).thenReturn(Collections.singletonList(regularExpressionFormat));
        setFetcher(fetcher);
        scrutinize(updateA);
        assertNoWarningRaised();
    }

    @Test
    public void testIncompleteMatch() {
        ItemIdValue idA = TestingData.existingId;
        ValueSnak value = Datamodel.makeValueSnak(propertyIdValue, incompleteMatchValue);
        Statement statement = new StatementImpl("P18", value, idA);
        ItemUpdate updateA = new ItemUpdateBuilder(idA).addStatement(statement).build();

        Snak qualifierSnak = Datamodel.makeValueSnak(regularExpressionParameter, regularExpressionFormat);
        List<Snak> qualifierSnakList = Collections.singletonList(qualifierSnak);
        SnakGroup qualifierSnakGroup = Datamodel.makeSnakGroup(qualifierSnakList);
        List<SnakGroup> snakGroupList = Collections.singletonList(qualifierSnakGroup);
        List<Statement> statementList = constraintParameterStatementList(entityIdValue, snakGroupList);

        ConstraintFetcher fetcher = mock(ConstraintFetcher.class);
        when(fetcher.getConstraintsByType(propertyIdValue, FORMAT_CONSTRAINT_QID)).thenReturn(statementList);
        when(fetcher.findValues(snakGroupList, FORMAT_REGEX_PID)).thenReturn(Collections.singletonList(regularExpressionFormat));
        setFetcher(fetcher);
        scrutinize(updateA);
        assertWarningsRaised(FormatScrutinizer.type);
    }

}
