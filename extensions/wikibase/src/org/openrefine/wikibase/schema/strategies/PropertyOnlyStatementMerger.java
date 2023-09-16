
package org.openrefine.wikibase.schema.strategies;

import org.wikidata.wdtk.datamodel.interfaces.Statement;

/**
 * Merging strategy which only looks at the main property of the statements to determine if they match.
 * 
 * @author Antonin Delpeuch
 *
 */
public class PropertyOnlyStatementMerger implements StatementMerger {

    @Override
    public boolean match(Statement existing, Statement added) {
        // deliberately only looking at raw property ids (not siteIRI) to
        // avoid spurious matches due to federation
        return existing.getMainSnak().getPropertyId().getId()
                .equals(added.getMainSnak().getPropertyId().getId());
    }

    @Override
    public Statement merge(Statement existing, Statement added) {
        return existing;
    }

    @Override
    public String toString() {
        return "PropertyOnlyStatementMerger";
    }

    @Override
    public int hashCode() {
        return 893;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        return getClass() == obj.getClass();
    }
}
