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

import java.util.Map;

import org.wikidata.wdtk.datamodel.interfaces.DatatypeIdValue;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;

import org.openrefine.wikibase.qa.QAWarning;
import org.openrefine.wikibase.utils.EntityCache;

/**
 * Checks entity-valued snaks against the property's intrinsic datatype, independently of property constraints.
 */
public class PropertyDatatypeScrutinizer extends SnakScrutinizer {

    public static final String type = "property-datatype-mismatch";

    private static final Map<String, String> expectedEntityTypes = Map.of(
            DatatypeIdValue.DT_ITEM, EntityIdValue.ET_ITEM,
            DatatypeIdValue.DT_PROPERTY, EntityIdValue.ET_PROPERTY,
            DatatypeIdValue.DT_LEXEME, EntityIdValue.ET_LEXEME,
            DatatypeIdValue.DT_FORM, EntityIdValue.ET_FORM,
            DatatypeIdValue.DT_SENSE, EntityIdValue.ET_SENSE);

    private static final Map<String, String> entityTypeNames = Map.of(
            EntityIdValue.ET_ITEM, "Item",
            EntityIdValue.ET_PROPERTY, "Property",
            EntityIdValue.ET_LEXEME, "Lexeme",
            EntityIdValue.ET_FORM, "Form",
            EntityIdValue.ET_SENSE, "Sense");

    private final EntityCache entityCache;

    public PropertyDatatypeScrutinizer(EntityCache entityCache) {
        this.entityCache = entityCache;
    }

    @Override
    public boolean prepareDependencies() {
        return entityCache != null;
    }

    @Override
    public void scrutinize(Snak snak, EntityIdValue entityId, boolean added) {
        if (!added || !(snak instanceof ValueSnak)) {
            return;
        }
        Value value = ((ValueSnak) snak).getValue();
        if (!(value instanceof EntityIdValue)) {
            return;
        }
        String actualEntityType = ((EntityIdValue) value).getEntityType();
        String actualEntityTypeName = entityTypeNames.get(actualEntityType);
        if (actualEntityTypeName == null) {
            return;
        }

        PropertyIdValue propertyId = snak.getPropertyId();
        PropertyDocument property = (PropertyDocument) entityCache.get(propertyId);
        DatatypeIdValue datatype = property.getDatatype();
        if (datatype == null) {
            return;
        }
        String expectedEntityType = expectedEntityTypes.get(datatype.getIri());
        if (expectedEntityType == null || expectedEntityType.equals(actualEntityType)) {
            return;
        }

        String expectedEntityTypeName = entityTypeNames.get(expectedEntityType);
        String bucketId = propertyId.getId() + "_" + expectedEntityTypeName + "_" + actualEntityTypeName;
        QAWarning warning = new QAWarning(type, bucketId, QAWarning.Severity.IMPORTANT, 1);
        warning.setProperty("property_entity", propertyId);
        warning.setProperty("example_entity", entityId);
        warning.setProperty("expected_entity_type", expectedEntityTypeName);
        warning.setProperty("actual_entity_type", actualEntityTypeName);
        addIssue(warning);
    }
}
