
package org.openrefine.wikibase.updates;

import java.util.List;
import java.util.stream.Collectors;

import org.openrefine.wikibase.schema.strategies.StatementEditingMode;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A candidate edit on an entity which can bear statements.
 * 
 * @author Antonin Delpeuch
 *
 */
public interface StatementEntityEdit extends EntityEdit {

    /**
     * Edits made to statements
     */
    @JsonIgnore
    List<StatementEdit> getStatementEdits();

    /**
     * Groups added statements in {@link StatementGroupEdit} objects.
     */
    @JsonProperty("statementGroups")
    List<StatementGroupEdit> getStatementGroupEdits();

    /**
     * @return the statements which should be added or merged with the existing ones on the item.
     */
    @JsonIgnore
    public default List<Statement> getAddedStatements() {
        return getStatementEdits().stream()
                .filter(statement -> statement.getMode() != StatementEditingMode.DELETE)
                .map(StatementEdit::getStatement)
                .collect(Collectors.toList());
    }

    /**
     * @return the statements which should be deleted from the item.
     */
    @JsonIgnore
    public default List<Statement> getDeletedStatements() {
        return getStatementEdits().stream()
                .filter(statement -> statement.getMode() == StatementEditingMode.DELETE)
                .map(StatementEdit::getStatement)
                .collect(Collectors.toList());
    }

}
