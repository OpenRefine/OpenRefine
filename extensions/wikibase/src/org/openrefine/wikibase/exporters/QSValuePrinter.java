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

package org.openrefine.wikibase.exporters;

import java.math.BigDecimal;
import java.util.Locale;

import org.openrefine.wikibase.schema.entityvalues.ReconEntityIdValue;
import org.openrefine.wikibase.updates.scheduler.QuickStatementsUpdateScheduler;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;
import org.wikidata.wdtk.datamodel.interfaces.UnsupportedValue;
import org.wikidata.wdtk.datamodel.interfaces.ValueVisitor;

/**
 * Prints a Wikibase value as a string as required by QuickStatements. Format documentation:
 * https://www.wikidata.org/wiki/Help:QuickStatements
 * 
 * Any new entity id will be assumed to be the last one created, represented with "LAST". It is fine to do this
 * assumption because we are working on edit batches previously scheduled by {@link QuickStatementsUpdateScheduler}.
 * 
 * @author Antonin Delpeuch
 *
 */
public class QSValuePrinter implements ValueVisitor<String> {

    @Override
    public String visit(EntityIdValue value) {
        if (ReconEntityIdValue.class.isInstance(value) && ((ReconEntityIdValue) value).isNew()) {
            return "LAST";
        }
        return value.getId();
    }

    @Override
    public String visit(GlobeCoordinatesValue value) {
        return String.format(Locale.US, "@%f/%f", value.getLatitude(), value.getLongitude());
    }

    @Override
    public String visit(MonolingualTextValue value) {
        return String.format("%s:\"%s\"", value.getLanguageCode(), value.getText());
    }

    @Override
    public String visit(QuantityValue value) {
        ItemIdValue unit = value.getUnitItemId();
        String unitRepresentation = "", boundsRepresentation = "";
        if (unit != null) {
            unitRepresentation = "U" + unit.getId().substring(1);
        }
        if (value.getLowerBound() != null) {
            // bounds are always null at the same time so we know they are both not null
            BigDecimal lowerBound = value.getLowerBound();
            BigDecimal upperBound = value.getUpperBound();
            boundsRepresentation = String.format(Locale.US, "[%s,%s]", lowerBound.toString(), upperBound.toString());
        }
        return String.format(Locale.US, "%s%s%s", value.getNumericValue().toString(), boundsRepresentation,
                unitRepresentation);
    }

    @Override
    public String visit(StringValue value) {
        return "\"" + value.getString() + "\"";
    }

    @Override
    public String visit(TimeValue value) {
        return String.format("+%04d-%02d-%02dT%02d:%02d:%02dZ/%d", value.getYear(), value.getMonth(), value.getDay(),
                value.getHour(), value.getMinute(), value.getSecond(), value.getPrecision());
    }

    @Override
    public String visit(UnsupportedValue value) {
        // we know this cannot happen, since UnsupportedValues cannot be generated in OpenRefine
        return "<UNSUPPORTED>";
    }
}
