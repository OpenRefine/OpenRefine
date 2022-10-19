
package org.openrefine.wikibase.schema.strategies;

import org.wikidata.wdtk.datamodel.interfaces.Statement;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

/**
 * Object which determines how uploaded statements are matched with existing statements on the edited entity.
 * 
 * @author Antonin Delpeuch
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @Type(value = SnakOnlyStatementMerger.class, name = "snak"),
        @Type(value = PropertyOnlyStatementMerger.class, name = "property"),
        @Type(value = QualifiersStatementMerger.class, name = "qualifiers"), })
public interface StatementMerger {

    /**
     * Default strategy used in previous versions of the tool, where no strategy could be specified. TODO make it
     * faithful to what it was before.
     */
    StatementMerger FORMER_DEFAULT_STRATEGY = new QualifiersStatementMerger(new StrictValueMatcher(), null);

    /**
     * Determines if the existing statement matches the statement to add (or remove)
     * 
     * @param existing
     *            the statement currently on the entity
     * @param added
     *            the statement to add or remove
     * @return
     */
    public boolean match(Statement existing, Statement added);

    /**
     * Return the result of merging the statement to add with the existing statement. This method can assume that the
     * two statements are matching (i.e. the method above has returned true on them).
     * 
     * @param existing
     *            the statement currently on the entity
     * @param added
     *            the statement to add or remove
     * @return the merged statement obtained out of the two
     */
    public Statement merge(Statement existing, Statement added);
}
