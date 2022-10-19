
package org.openrefine.wikibase.exporters;

import org.wikidata.wdtk.datamodel.interfaces.NoValueSnak;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.SnakVisitor;
import org.wikidata.wdtk.datamodel.interfaces.SomeValueSnak;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;
import org.wikidata.wdtk.datamodel.interfaces.ValueVisitor;

/**
 * Represents a snak in QuickStatements format.
 * 
 * @author Antonin Delpeuch
 *
 */
public class QSSnakPrinter implements SnakVisitor<String> {

    protected final boolean reference;
    protected final ValueVisitor<String> valuePrinter = new QSValuePrinter();

    /**
     * @param reference
     *            indicates whether to print snaks as reference, or as main/qualifier snaks
     */
    public QSSnakPrinter(boolean reference) {
        this.reference = reference;
    }

    @Override
    public String visit(ValueSnak snak) {
        String valStr = snak.getValue().accept(valuePrinter);
        return toQS(snak.getPropertyId(), valStr);
    }

    @Override
    public String visit(SomeValueSnak snak) {
        return toQS(snak.getPropertyId(), "somevalue");
    }

    @Override
    public String visit(NoValueSnak snak) {
        return toQS(snak.getPropertyId(), "novalue");
    }

    protected String toQS(PropertyIdValue property, String value) {
        String pid = property.getId();
        if (reference) {
            pid = pid.replace('P', 'S');
        }
        return "\t" + pid + "\t" + value;
    }

}
