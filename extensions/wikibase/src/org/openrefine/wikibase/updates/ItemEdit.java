
package org.openrefine.wikibase.updates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.helper.Validate;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.AliasUpdate;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.EntityUpdate;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.StatementUpdate;
import org.wikidata.wdtk.datamodel.interfaces.TermUpdate;

/**
 * Represents a candidate edit on an existing item or the creation of a new one. This is the representation before any
 * comparison with existing data.
 * 
 * @author Antonin Delpeuch
 *
 */
public class ItemEdit extends TermedStatementEntityEdit {

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
     * @param descriptions
     *            the descriptions to add on the item, overriding any existing one in that language
     * @param descriptionsIfNew
     *            the descriptions to add on the item, only if no description for that language exists
     * @param aliases
     *            the aliases to add on the item. In theory their order should matter but in practice people rarely rely
     *            on the order of aliases so this is just kept as a set for simplicity.
     */
    public ItemEdit(
            EntityIdValue id,
            List<StatementEdit> statements,
            Set<MonolingualTextValue> labels,
            Set<MonolingualTextValue> labelsIfNew,
            Set<MonolingualTextValue> descriptions,
            Set<MonolingualTextValue> descriptionsIfNew,
            Set<MonolingualTextValue> aliases) {
        super(id, statements, labels, labelsIfNew, descriptions, descriptionsIfNew, aliases);
        Validate.isTrue(id instanceof ItemIdValue, "the entity id must be an ItemIdValue");
    }

    /**
     * Protected constructor to avoid re-constructing term maps when merging two entity updates.
     * 
     * No validation is done on the arguments, they all have to be non-null.
     * 
     * @param id
     *            the subject of the update
     * @param statements
     *            the statements to add or delete
     * @param labels
     *            the labels to add on the entity, overriding any existing one in that language
     * @param labelsIfNew
     *            the labels to add on the entity, only if no label for that language exists
     * @param descriptions
     *            the descriptions to add on the item, overriding any existing one in that language
     * @param descriptionsIfNew
     *            the descriptions to add on the item, only if no description for that language exists
     * @param aliases
     *            the aliases to add
     */
    protected ItemEdit(EntityIdValue id, List<StatementEdit> statements, Map<String, MonolingualTextValue> labels,
            Map<String, MonolingualTextValue> labelsIfNew, Map<String, MonolingualTextValue> descriptions,
            Map<String, MonolingualTextValue> descriptionsIfNew, Map<String, List<MonolingualTextValue>> aliases) {
        super(id, statements, labels, labelsIfNew, descriptions, descriptionsIfNew, aliases);
    }

    /**
     * Merges all the changes in other with this instance. Both updates should have the same subject. Changes coming
     * from `other` have priority over changes from this instance. This instance is not modified, the merged update is
     * returned instead.
     * 
     * @param otherEdit
     *            the other change that should be merged
     */
    @Override
    public ItemEdit merge(EntityEdit otherEdit) {
        Validate.isTrue(id.equals(otherEdit.getEntityId()));
        Validate.isTrue(otherEdit instanceof ItemEdit);
        ItemEdit other = (ItemEdit) otherEdit;
        List<StatementEdit> newStatements = new ArrayList<>(statements);
        for (StatementEdit statement : other.getStatementEdits()) {
            if (!newStatements.contains(statement)) {
                newStatements.add(statement);
            }
        }
        Map<String, MonolingualTextValue> newLabels = new HashMap<>(labels);
        Map<String, MonolingualTextValue> newLabelsIfNew = new HashMap<>(labelsIfNew);
        mergeSingleTermMaps(newLabels, newLabelsIfNew, other.getLabels(), other.getLabelsIfNew());
        Map<String, MonolingualTextValue> newDescriptions = new HashMap<>(descriptions);
        Map<String, MonolingualTextValue> newDescriptionsIfNew = new HashMap<>(descriptionsIfNew);
        mergeSingleTermMaps(newDescriptions, newDescriptionsIfNew, other.getDescriptions(), other.getDescriptionsIfNew());
        Map<String, List<MonolingualTextValue>> newAliases = new HashMap<>(aliases);
        for (MonolingualTextValue alias : other.getAliases()) {
            List<MonolingualTextValue> aliases = newAliases.get(alias.getLanguageCode());
            if (aliases == null) {
                aliases = new LinkedList<>();
                newAliases.put(alias.getLanguageCode(), aliases);
            }
            if (!aliases.contains(alias)) {
                aliases.add(alias);
            }
        }
        return new ItemEdit(id, newStatements, newLabels, newLabelsIfNew, newDescriptions, newDescriptionsIfNew, newAliases);
    }

    /**
     * In case the subject id is not new, returns the corresponding update given the current state of the entity.
     */
    @Override
    public EntityUpdate toEntityUpdate(EntityDocument entityDocument) {
        Validate.isFalse(isNew(), "Cannot create a corresponding entity update for a creation of a new entity.");
        ItemDocument itemDocument = (ItemDocument) entityDocument;
        // Labels
        List<MonolingualTextValue> labels = getLabels().stream().collect(Collectors.toList());
        labels.addAll(getLabelsIfNew().stream()
                .filter(label -> !itemDocument.getLabels().containsKey(label.getLanguageCode())).collect(Collectors.toList()));
        TermUpdate labelUpdate = Datamodel.makeTermUpdate(labels, Collections.emptyList());

        // Descriptions
        List<MonolingualTextValue> descriptions = getDescriptions().stream().collect(Collectors.toList());
        descriptions.addAll(getDescriptionsIfNew().stream()
                .filter(desc -> !itemDocument.getDescriptions().containsKey(desc.getLanguageCode())).collect(Collectors.toList()));
        TermUpdate descriptionUpdate = Datamodel.makeTermUpdate(descriptions, Collections.emptyList());

        // Aliases
        Set<MonolingualTextValue> aliases = getAliases();
        Map<String, List<MonolingualTextValue>> aliasesMap = aliases.stream()
                .collect(Collectors.groupingBy(MonolingualTextValue::getLanguageCode));
        Map<String, AliasUpdate> aliasMap = aliasesMap.entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, e -> Datamodel.makeAliasUpdate(e.getValue(), Collections.emptyList())));

        // Statements
        StatementUpdate statementUpdate = toStatementUpdate(itemDocument);

        return Datamodel.makeItemUpdate(
                // important: use the id from the document, not from the update, as
                // they might not be the same if a redirect has happened
                itemDocument.getEntityId(),
                entityDocument.getRevisionId(),
                labelUpdate,
                descriptionUpdate,
                aliasMap,
                statementUpdate,
                Collections.emptyList(),
                Collections.emptyList());
    }

    /**
     * In case the subject id is new, returns the corresponding new item document to be created.
     */
    @Override
    public ItemDocument toNewEntity() {
        Validate.isTrue(isNew(), "Cannot create a corresponding entity document for an edit on an existing entity.");

        // Ensure that we are only adding aliases with labels
        Set<MonolingualTextValue> filteredAliases = new HashSet<>();
        Map<String, MonolingualTextValue> newLabels = new HashMap<>(labelsIfNew);
        newLabels.putAll(labels);
        for (MonolingualTextValue alias : getAliases()) {
            if (!newLabels.containsKey(alias.getLanguageCode())) {
                newLabels.put(alias.getLanguageCode(), alias);
            } else {
                filteredAliases.add(alias);
            }
        }
        Map<String, MonolingualTextValue> newDescriptions = new HashMap<>(descriptionsIfNew);
        newDescriptions.putAll(descriptions);

        return Datamodel.makeItemDocument((ItemIdValue) id,
                newLabels.values().stream().collect(Collectors.toList()),
                newDescriptions.values().stream().collect(Collectors.toList()),
                filteredAliases.stream().collect(Collectors.toList()),
                getStatementGroupsForNewEntity(),
                Collections.emptyMap());
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !ItemEdit.class.isInstance(other)) {
            return false;
        }
        ItemEdit otherUpdate = (ItemEdit) other;
        return id.equals(otherUpdate.getEntityId()) && statements.equals(otherUpdate.getStatementEdits())
                && getLabels().equals(otherUpdate.getLabels())
                && getDescriptions().equals(otherUpdate.getDescriptions())
                && getAliases().equals(otherUpdate.getAliases());
    }

    @Override
    public int hashCode() {
        return id.hashCode() + statements.hashCode() + labels.hashCode()
                + descriptions.hashCode() + aliases.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("<Update on ");
        builder.append(id);
        if (!labels.isEmpty()) {
            builder.append("\n  Labels (override): ");
            builder.append(labels);
        }
        if (!labelsIfNew.isEmpty()) {
            builder.append("\n  Labels (if new): ");
            builder.append(labelsIfNew);
        }
        if (!descriptions.isEmpty()) {
            builder.append("\n  Descriptions (override): ");
            builder.append(descriptions);
        }
        if (!descriptionsIfNew.isEmpty()) {
            builder.append("\n  Descriptions (if new): ");
            builder.append(descriptionsIfNew);
        }
        if (!aliases.isEmpty()) {
            builder.append("\n  Aliases: ");
            builder.append(aliases);
        }
        if (!statements.isEmpty()) {
            builder.append("\n  Statements: ");
            builder.append(statements);
        }
        if (isNull()) {
            builder.append(" (null update)");
        }
        builder.append("\n>");
        return builder.toString();
    }

}
