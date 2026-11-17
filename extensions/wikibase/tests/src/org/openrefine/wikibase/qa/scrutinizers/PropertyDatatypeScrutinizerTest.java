/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 OpenRefine contributors
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.implementation.StatementImpl;
import org.wikidata.wdtk.datamodel.interfaces.DatatypeIdValue;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import org.openrefine.wikibase.qa.QAWarning;
import org.openrefine.wikibase.testing.TestingData;
import org.openrefine.wikibase.updates.ItemEditBuilder;
import org.openrefine.wikibase.utils.EntityCache;

public class PropertyDatatypeScrutinizerTest extends StatementScrutinizerTest {

    private static final PropertyIdValue formProperty = Datamodel.makeWikidataPropertyIdValue("P8017");
    private static final PropertyIdValue itemProperty = Datamodel.makeWikidataPropertyIdValue("P31");
    private static final EntityIdValue item = Datamodel.makeWikidataItemIdValue("Q5");

    private EntityCache cache;

    @Override
    public EditScrutinizer getScrutinizer() {
        cache = mock(EntityCache.class);
        return new PropertyDatatypeScrutinizer(cache);
    }

    @DataProvider(name = "entityKinds")
    public Object[][] entityKinds() {
        return new Object[][] {
                { "wikibase-item", item, itemProperty, "Item", "Property" },
                { "wikibase-property", itemProperty, item, "Property", "Item" },
                { "wikibase-lexeme", Datamodel.makeWikidataLexemeIdValue("L1"), item, "Lexeme", "Item" },
                { "wikibase-form", Datamodel.makeWikidataFormIdValue("L1-F1"), item, "Form", "Item" },
                { "wikibase-sense", Datamodel.makeWikidataSenseIdValue("L1-S1"), item, "Sense", "Item" }
        };
    }

    @Test(dataProvider = "entityKinds")
    public void testKnownEntityKinds(String datatype, Value matchingValue, Value mismatchingValue,
            String expectedEntityType, String actualEntityType) {
        setDatatype(formProperty, datatype);
        scrutinize(statement(formProperty, matchingValue));
        assertNoWarningRaised();

        scrutinize(statement(formProperty, mismatchingValue));
        assertWarningRaised(expectedWarning(formProperty, expectedEntityType, actualEntityType, 1));
    }

    @Test
    public void testQualifiersAndReferences() {
        setDatatype(itemProperty, "wikibase-item");
        setDatatype(formProperty, "wikibase-form");
        scrutinize(statementWithQualifierAndReference());
        assertWarningRaised(expectedWarning(formProperty, "Form", "Item", 2));
    }

    @Test
    public void testDeletedSnaksAreIgnored() {
        setDatatype(itemProperty, "wikibase-item");
        setDatatype(formProperty, "wikibase-form");
        scrutinize(new ItemEditBuilder(TestingData.existingId)
                .addStatement(delete(statementWithQualifierAndReference()))
                .addContributingRowId(123)
                .build());
        assertNoWarningRaised();
    }

    @Test
    public void testNonValueSnaksAreIgnored() {
        setDatatype(formProperty, "wikibase-form");
        scrutinize(new StatementImpl("", Datamodel.makeSomeValueSnak(formProperty), TestingData.existingId));
        scrutinize(new StatementImpl("", Datamodel.makeNoValueSnak(formProperty), TestingData.existingId));
        assertNoWarningRaised();
    }

    @Test
    public void testNonEntityValuesAreIgnored() {
        setDatatype(formProperty, "wikibase-form");
        scrutinize(statement(formProperty, Datamodel.makeStringValue("synthetic value")));
        assertNoWarningRaised();
    }

    @Test
    public void testOtherDatatypesAreIgnored() {
        setDatatype(formProperty, "string");
        scrutinize(statement(formProperty, item));
        setDatatype(formProperty, "custom-entity");
        scrutinize(statement(formProperty, item));
        assertNoWarningRaised();
    }

    @Test
    public void testDistinctMismatchBuckets() {
        PropertyIdValue otherFormProperty = Datamodel.makeWikidataPropertyIdValue("P999");
        setDatatype(formProperty, "wikibase-form");
        setDatatype(otherFormProperty, "wikibase-form");
        scrutinize(statement(formProperty, item));
        scrutinize(statement(formProperty, itemProperty));
        scrutinize(statement(otherFormProperty, item));
        assertWarningRaised(expectedWarning(formProperty, "Form", "Item", 1));
        assertWarningRaised(expectedWarning(formProperty, "Form", "Property", 1));
        assertWarningRaised(expectedWarning(otherFormProperty, "Form", "Item", 1));
    }

    private void setDatatype(PropertyIdValue propertyId, String datatype) {
        PropertyDocument property = mock(PropertyDocument.class);
        DatatypeIdValue datatypeValue = Datamodel.makeDatatypeIdValueFromJsonString(datatype);
        when(property.getDatatype()).thenReturn(datatypeValue);
        when(cache.get(propertyId)).thenReturn(property);
    }

    private Statement statement(PropertyIdValue propertyId, Value value) {
        return new StatementImpl("", Datamodel.makeValueSnak(propertyId, value), TestingData.existingId);
    }

    private Statement statementWithQualifierAndReference() {
        Snak main = Datamodel.makeValueSnak(itemProperty, item);
        Snak mismatch = Datamodel.makeValueSnak(formProperty, item);
        List<SnakGroup> groups = makeSnakGroupList(mismatch);
        return Datamodel.makeStatement(Datamodel.makeClaim(TestingData.existingId, main, groups),
                Collections.singletonList(Datamodel.makeReference(groups)), StatementRank.NORMAL, "");
    }

    private QAWarning expectedWarning(PropertyIdValue propertyId, String expectedEntityType, String actualEntityType, int count) {
        String bucketId = propertyId.getId() + "_" + expectedEntityType + "_" + actualEntityType;
        QAWarning warning = new QAWarning(PropertyDatatypeScrutinizer.type, bucketId, QAWarning.Severity.IMPORTANT, count);
        warning.setProperty("property_entity", propertyId);
        warning.setProperty("example_entity", TestingData.existingId);
        warning.setProperty("expected_entity_type", expectedEntityType);
        warning.setProperty("actual_entity_type", actualEntityType);
        return warning;
    }
}
