package org.openrefine.wikidata.exporters;

import java.math.BigDecimal;
import java.util.Locale;

import org.openrefine.wikidata.schema.entityvalues.ReconEntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.DatatypeIdValue;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;
import org.wikidata.wdtk.datamodel.interfaces.ValueVisitor;

/**
 * Prints a Wikibase value as a string as required by QuickStatements.
 * Format documentation:
 * https://www.wikidata.org/wiki/Help:QuickStatements
 * 
 * Any new entity id will be
 * assumed to be the last one created, represented with "LAST". It is
 * fine to do this assumption because we are working on edit batches
 * previously scheduled by {@link QuickStatementsUpdateScheduler}.
 * 
 * @author Antonin Delpeuch
 *
 */
public class QSValuePrinter implements ValueVisitor<String> {

    @Override
    public String visit(DatatypeIdValue value) {
        // unsupported according to
        // https://tools.wmflabs.org/wikidata-todo/quick_statements.php?
        return null;
    }

    @Override
    public String visit(EntityIdValue value) {
        if (ReconEntityIdValue.class.isInstance(value) && ((ReconEntityIdValue)value).isNew()) {
            return "LAST";
        }
        return value.getId();
    }

    @Override
    public String visit(GlobeCoordinatesValue value) {
        return String.format(
            Locale.US,
            "@%f/%f",
            value.getLatitude(),
            value.getLongitude());
    }

    @Override
    public String visit(MonolingualTextValue value) {
        return String.format(
             "%s:\"%s\"",
             value.getLanguageCode(),
             value.getText());
    }

    @Override
    public String visit(QuantityValue value) {
        String unitPrefix = "http://www.wikidata.org/entity/Q";
        String unitIri = value.getUnit();
        String unitRepresentation = "", boundsRepresentation = "";
        if (!unitIri.isEmpty()) {
            if (!unitIri.startsWith(unitPrefix))
                return null; // QuickStatements only accepts Qids as units
            unitRepresentation = "U"+unitIri.substring(unitPrefix.length());
        }
        if (value.getLowerBound() != null) {
            // bounds are always null at the same time so we know they are both not null
            BigDecimal lowerBound = value.getLowerBound();
            BigDecimal upperBound = value.getUpperBound();
            boundsRepresentation = String.format(Locale.US, "[%s,%s]",
                    lowerBound.toString(), upperBound.toString());
        }
        return String.format(
                Locale.US,
                "%s%s%s",
                value.getNumericValue().toString(),
                boundsRepresentation,
                unitRepresentation);
    }

    @Override
    public String visit(StringValue value) {
        return "\"" + value.getString() + "\"";
    }

    @Override
    public String visit(TimeValue value) {
        return String.format(
            "+%04d-%02d-%02dT%02d:%02d:%02dZ/%d",
            value.getYear(),
            value.getMonth(),
            value.getDay(),
            value.getHour(),
            value.getMinute(),
            value.getSecond(),
            value.getPrecision());
    }
}
