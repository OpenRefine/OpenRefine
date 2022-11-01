
package org.openrefine.wikibase.schema.strategies;

import org.wikidata.wdtk.datamodel.interfaces.Value;

/**
 * Simple value matching strategy which only treats value as identical when they are strictly equal.
 * 
 * @author Antonin Delpeuch
 *
 */
public class StrictValueMatcher implements ValueMatcher {

    @Override
    public boolean match(Value existing, Value added) {
        return existing.equals(added);
    }

    @Override
    public String toString() {
        return "StrictValueMatcher";
    }

    @Override
    public int hashCode() {
        // constant because this object has no fields
        return 39834347;
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
