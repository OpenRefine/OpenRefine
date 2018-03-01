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
 * @author Antonin Delpeuch
 *
 */
public class QSValuePrinter implements ValueVisitor<String> {
    
    private final ReconEntityIdValue lastCreatedEntityIdValue;
    
    /**
     * Constructor.
     * 
     * Creates a printer for a context where no entity was previously
     * created with the "CREATE" command. Any new entity id will not
     * be printed.
     */
    public QSValuePrinter() {
        lastCreatedEntityIdValue = null;
    }
    
    /**
     * Creates a printer for a context where an entity was previously
     * created with the "CREATE" command. If this id is encountered,
     * it will be printed as "LAST".
     * 
     * @param lastCreatedEntityIdValue
     *      the virtual id of the last created entity
     */
    public QSValuePrinter(ReconEntityIdValue lastCreatedEntityIdValue) {
        this.lastCreatedEntityIdValue = lastCreatedEntityIdValue;
    }

    @Override
    public String visit(DatatypeIdValue value) {
        // unsupported according to
        // https://tools.wmflabs.org/wikidata-todo/quick_statements.php?
        return null;
    }

    @Override
    public String visit(EntityIdValue value) {
        if (lastCreatedEntityIdValue != null && lastCreatedEntityIdValue.equals(value)) {
            return "LAST";
        } else if (ReconEntityIdValue.class.isInstance(value)) {
            // oops, we are trying to print another newly created entity (not the last one)
            return null;
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
