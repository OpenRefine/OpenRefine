
package org.openrefine.wikibase.updates;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang.Validate;
import org.openrefine.wikibase.schema.strategies.StatementEditingMode;
import org.openrefine.wikibase.schema.strategies.StatementMerger;
import org.wikidata.wdtk.datamodel.helpers.StatementUpdateBuilder;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A list of statement edits which share the same property, and will therefore affect the same statement group on the
 * target entity.
 * 
 * @author Antonin Delpeuch
 *
 */
public class StatementGroupEdit {

    protected final PropertyIdValue property;
    protected final List<StatementEdit> statementEdits;

    public StatementGroupEdit(List<StatementEdit> statementEdits) {
        Validate.notEmpty(statementEdits, "Attempting to construct an empty statement group");
        this.property = statementEdits.get(0).getPropertyId();
        this.statementEdits = statementEdits;
    }

    @JsonProperty("property")
    public PropertyIdValue getProperty() {
        return property;
    }

    @JsonProperty("statementEdits")
    public List<StatementEdit> getStatementEdits() {
        return statementEdits;
    }

    /**
     * Given an existing statement group on the target entity, translate this edit into concrete changes of statements,
     * by logging them into the supplied builder.
     * 
     * @param builder
     *            the statement update builder in which to add the changes
     * @param statementGroup
     *            the corresponding existing statement group on the entity, or null if there is no such statement yet
     */
    public void contributeToStatementUpdate(StatementUpdateBuilder builder, StatementGroup statementGroup) {
        List<Statement> statements = statementGroup == null ? Collections.emptyList() : statementGroup.getStatements();
        for (StatementEdit edit : statementEdits) {
            StatementMerger merger = edit.getMerger();
            Stream<Statement> matchingStatements = statements
                    .stream()
                    .filter(statement -> {
                        return merger.match(statement, edit.getStatement());
                    });

            StatementEditingMode mode = edit.getMode();
            switch (mode) {
                case ADD:
                    Optional<Statement> anyMatching = matchingStatements.findAny();
                    if (anyMatching.isEmpty()) {
                        builder.add(edit.getStatement());
                    }
                    break;
                case ADD_OR_MERGE:
                    Optional<Statement> firstMatching = matchingStatements.findFirst();
                    if (firstMatching.isEmpty()) {
                        builder.add(edit.getStatement());
                    } else {
                        builder.replace(merger.merge(firstMatching.get(), edit.getStatement()));
                    }
                    break;
                case DELETE:
                    matchingStatements
                            .forEach(matchingStatement -> {
                                builder.remove(matchingStatement.getStatementId());
                            });
                    break;
                default:
                    throw new IllegalStateException("Unsupported statement editing mode " + mode);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof StatementGroupEdit)) {
            return false;
        }
        StatementGroupEdit other = (StatementGroupEdit) obj;
        return Objects.equals(property, other.property) && Objects.equals(statementEdits, other.statementEdits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(property, statementEdits);
    }

    @Override
    public String toString() {
        return statementEdits.toString();
    }

}
