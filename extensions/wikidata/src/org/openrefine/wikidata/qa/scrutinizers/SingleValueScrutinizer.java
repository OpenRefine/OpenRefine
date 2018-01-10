package org.openrefine.wikidata.qa.scrutinizers;

import java.util.HashSet;
import java.util.Set;

import org.openrefine.wikidata.qa.ConstraintFetcher;
import org.openrefine.wikidata.schema.ItemUpdate;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

/**
 * For now this scrutinizer only checks for uniqueness at
 * the item level (it ignores qualifiers and references).
 * @author antonin
 *
 */
public class SingleValueScrutinizer extends ItemEditScrutinizer {
    
    private ConstraintFetcher _fetcher;
    
    public SingleValueScrutinizer() {
        _fetcher = new ConstraintFetcher();
    }

    @Override
    public void scrutinize(ItemUpdate update) {
        Set<PropertyIdValue> seenSingleProperties = new HashSet<>();
        
        for(Statement statement : update.getAddedStatements()) {
            PropertyIdValue pid = statement.getClaim().getMainSnak().getPropertyId();
            if (seenSingleProperties.contains(pid)) {
                warning("single-valued-property-added-more-than-once");
            } else if (_fetcher.hasSingleValue(pid)) {
                seenSingleProperties.add(pid);
            }
        }
    }

}
