
package org.openrefine.wikibase.schema.strategies;

import org.wikidata.wdtk.datamodel.interfaces.Value;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Determines if two statement values should be considered identical or not.
 * 
 * @author Antonin Delpeuch
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @Type(value = StrictValueMatcher.class, name = "strict"),
        @Type(value = LaxValueMatcher.class, name = "lax"), })
public interface ValueMatcher {

    /**
     * Compare two values and return true if they should be treated as identical.
     * 
     * @param existing
     *            the existing value on the entity
     * @param added
     *            the value to add/remove
     * @return
     */
    public boolean match(Value existing, Value added);
}
