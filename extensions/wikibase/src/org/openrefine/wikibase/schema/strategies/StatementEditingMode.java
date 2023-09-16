
package org.openrefine.wikibase.schema.strategies;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * What to do with statements input in the schema.
 * 
 * @author Antonin Delpeuch
 *
 */
public enum StatementEditingMode {
    /**
     * If there are no matching statements on the item, add our statement. Otherwise, leave the item unchanged.
     */
    @JsonProperty("add")
    ADD,
    /**
     * If there are no matching statements on the item, add our statement. Otherwise, merge our statement with the first
     * matching one.
     */
    @JsonProperty("add_or_merge")
    ADD_OR_MERGE,
    /**
     * Delete any statement that matches our statement.
     */
    @JsonProperty("delete")
    DELETE
}
