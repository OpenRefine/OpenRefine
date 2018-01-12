package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.schema.ItemUpdate;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.Value;

/**
 * A scrutinizer that inspects the values of snaks and terms
 * @author antonin
 *
 */
public abstract class ValueScrutinizer extends SnakScrutinizer {
    
    @Override
    public void scrutinize(ItemUpdate update) {
        super.scrutinize(update);
        
        for(MonolingualTextValue label : update.getLabels()) {
            scrutinize(label);
        }
        for(MonolingualTextValue alias : update.getAliases()) {
            scrutinize(alias);
        }
        for(MonolingualTextValue description : update.getDescriptions()) {
            scrutinize(description);
        }
    }

    public abstract void scrutinize(Value value);

    @Override
    public void scrutinize(Snak snak, EntityIdValue entityId, boolean added) {
        scrutinize(snak.getValue());
    }

}
