
package org.openrefine.wikibase.updates;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.jsoup.helper.Validate;
import org.openrefine.wikibase.schema.strategies.StatementEditingMode;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.helpers.StatementUpdateBuilder;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementDocument;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.StatementUpdate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class LabeledStatementEntityEdit implements StatementEntityEdit {

    protected final EntityIdValue id;
    protected final List<StatementEdit> statements;
    protected final Map<String, MonolingualTextValue> labels;
    protected final Map<String, MonolingualTextValue> labelsIfNew;

    /**
     * Constructor.
     * 
     * @param id
     *            the subject of the document. It can be a reconciled entity value for new entities.
     * @param statements
     *            the statements to change on the entity.
     * @param labels
     *            the labels to add on the entity, overriding any existing one in that language
     * @param labelsIfNew
     *            the labels to add on the entity, only if no label for that language exists
     */
    public LabeledStatementEntityEdit(EntityIdValue id, List<StatementEdit> statements,
            Set<MonolingualTextValue> labels, Set<MonolingualTextValue> labelsIfNew) {
        super();
        Validate.notNull(id);
        this.id = id;
        if (statements == null) {
            statements = Collections.emptyList();
        }
        this.labels = new HashMap<>();
        this.labelsIfNew = new HashMap<>();
        mergeSingleTermMaps(this.labels, this.labelsIfNew, labels, labelsIfNew);
        this.statements = statements;
    }

    /**
     * Protected constructor to avoid re-constructing term maps when merging two entity updates.
     * 
     * No validation is done on the arguments, they all have to be non-null.
     * 
     * @param id
     *            the subject of the update
     * @param statements
     *            the statements to edit
     * @param labels
     *            the labels to add on the entity, overriding any existing one in that language
     * @param labelsIfNew
     *            the labels to add on the entity, only if no label for that language exists
     */
    protected LabeledStatementEntityEdit(
            EntityIdValue id,
            List<StatementEdit> statements,
            Map<String, MonolingualTextValue> labels,
            Map<String, MonolingualTextValue> labelsIfNew) {
        super();
        this.id = id;
        this.statements = statements;
        this.labels = labels;
        this.labelsIfNew = labelsIfNew;
    }

    /**
     * @return the subject of the entity
     */
    @Override
    public EntityIdValue getEntityId() {
        return id;
    }

    /**
     * @return the list of updated labels, overriding existing ones
     */
    @JsonProperty("labels")
    public Set<MonolingualTextValue> getLabels() {
        return labels.values().stream().collect(Collectors.toSet());
    }

    /**
     * @return the list of updated labels, only added if new
     */
    @JsonProperty("labelsIfNew")
    public Set<MonolingualTextValue> getLabelsIfNew() {
        return labelsIfNew.values().stream().collect(Collectors.toSet());
    }

    /**
     * @return the list of statement updates
     */
    @JsonIgnore
    @Override
    public List<StatementEdit> getStatementEdits() {
        return statements;
    }

    @Override
    public List<StatementGroupEdit> getStatementGroupEdits() {
        Map<PropertyIdValue, List<StatementEdit>> map = statements.stream()
                .collect(Collectors.groupingBy(su -> su.getPropertyId()));
        List<StatementGroupEdit> result = map.values()
                .stream()
                .map(statements -> new StatementGroupEdit(statements))
                .collect(Collectors.toList());
        return result;
    }

    /**
     * Generates the statement groups which should appear on this entity if it is created as new.
     * 
     * TODO those statements are not currently deduplicated among themselves
     */
    protected List<StatementGroup> getStatementGroupsForNewEntity() {
        Map<PropertyIdValue, List<Statement>> map = statements.stream()
                .filter(statementEdit -> statementEdit.getMode() != StatementEditingMode.DELETE)
                .map(StatementEdit::getStatement)
                .collect(Collectors.groupingBy(s -> s.getMainSnak().getPropertyId()));
        return map.values()
                .stream()
                .map(statements -> Datamodel.makeStatementGroup(statements))
                .collect(Collectors.toList());
    }

    /**
     * Helper function to merge dictionaries of terms to override or provide.
     * 
     * @param currentTerms
     *            current map of terms to override
     * @param currentTermsIfNew
     *            current map of terms to provide if not already there
     * @param newTerms
     *            new terms to override
     * @param newTermsIfNew
     *            new terms to provide if not already there
     */
    protected static void mergeSingleTermMaps(
            Map<String, MonolingualTextValue> currentTerms,
            Map<String, MonolingualTextValue> currentTermsIfNew,
            Set<MonolingualTextValue> newTerms,
            Set<MonolingualTextValue> newTermsIfNew) {
        for (MonolingualTextValue otherLabel : newTerms) {
            String languageCode = otherLabel.getLanguageCode();
            currentTerms.put(languageCode, otherLabel);
            if (currentTermsIfNew.containsKey(languageCode)) {
                currentTermsIfNew.remove(languageCode);
            }
        }
        for (MonolingualTextValue otherLabel : newTermsIfNew) {
            String languageCode = otherLabel.getLanguageCode();
            if (!currentTermsIfNew.containsKey(languageCode) && !currentTerms.containsKey(languageCode)) {
                currentTermsIfNew.put(languageCode, otherLabel);
            }
        }
    }

    /**
     * Generates the statement update given the current statement groups on the entity.
     * 
     * @param currentDocument
     * @return
     */
    protected StatementUpdate toStatementUpdate(StatementDocument currentDocument) {
        Map<PropertyIdValue, List<StatementEdit>> groupedEdits = statements.stream()
                .collect(Collectors.groupingBy(StatementEdit::getPropertyId));
        StatementUpdateBuilder builder = StatementUpdateBuilder.create(currentDocument.getEntityId());

        for (Entry<PropertyIdValue, List<StatementEdit>> entry : groupedEdits.entrySet()) {
            // the id of the current document might be different from the id used to create the statements,
            // in the case of a redirect.
            List<StatementEdit> statementEdits = entry.getValue().stream()
                    .map(statementEdit -> statementEdit.withSubjectId(currentDocument.getEntityId()))
                    .collect(Collectors.toList());
            StatementGroupEdit statementGroupEdit = new StatementGroupEdit(statementEdits);
            StatementGroup statementGroup = currentDocument.findStatementGroup(entry.getKey().getId());
            statementGroupEdit.contributeToStatementUpdate(builder, statementGroup);
        }
        return builder.build();
    }

}
