package org.openrefine.wikidata.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.HashSet;

import org.jsoup.helper.Validate;
import org.wikidata.wdtk.datamodel.implementation.StatementGroupImpl;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;


/**
 * A class to plan an update of an item, after evaluating the statements
 * but before fetching the current content of the item (this is why it does not
 * extend StatementsUpdate).
 * 
 * @author Antonin Delpeuch
 */
public class ItemUpdate {
    private ItemIdValue qid;
    private Set<Statement> addedStatements;
    private Set<Statement> deletedStatements;
    private Set<MonolingualTextValue> labels;
    private Set<MonolingualTextValue> descriptions;
    private Set<MonolingualTextValue> aliases;
    
    /**
     * Constructor.
     * 
     * @param qid
     *      the subject of the document. It can be a reconciled item value for new items.
     */
    public ItemUpdate(ItemIdValue qid) {
        Validate.notNull(qid);
        this.qid = qid;
        this.addedStatements = new HashSet<>();
        this.deletedStatements = new HashSet<Statement>();
        this.labels = new HashSet<MonolingualTextValue>();
        this.descriptions = new HashSet<MonolingualTextValue>();
        this.aliases = new HashSet<MonolingualTextValue>();
    }
    
    /**
     * Mark a statement for insertion. If it matches an existing
     * statement, it will update the statement instead.
     * 
     * @param statement
     *      the statement to add or update
     */
    public void addStatement(Statement statement) {
        addedStatements.add(statement);
    }
    
    /**
     * Mark a statement for deletion. If no such statement exists,
     * nothing will be deleted.
     * 
     * @param statement
     *          the statement to delete
     */
    public void deleteStatement(Statement statement) {
        deletedStatements.add(statement);
    }
    
    /**
     * Add a list of statement, as in {@link addStatement}.
     * 
     * @param statements
     *        the statements to add
     */
    public void addStatements(Set<Statement> statements) {
        addedStatements.addAll(statements);
    }
    
    /**
     * Delete a list of statements, as in {@link deleteStatement}.
     * 
     * @param statements
     *          the statements to delete
     */
    public void deleteStatements(Set<Statement> statements) {
        deletedStatements.addAll(statements);
    }
    
    /**
     * @return the subject of the item
     */
    public ItemIdValue getItemId() {
        return qid;
    }
    
    /**
     * @return the set of all added statements
     */
    public Set<Statement> getAddedStatements() {
        return addedStatements;
    }
    
    /**
     * @return the list of all deleted statements
     */
    public Set<Statement> getDeletedStatements() {
        return deletedStatements;
    }

    /**
     * Merges all the changes in other into this instance.
     * Both updates should have the same subject.
     * 
     * @param other
     *          the other change that should be merged
     */
    public void merge(ItemUpdate other) {
        Validate.isTrue(qid.equals(other.getItemId()));
        addStatements(other.getAddedStatements());
        deleteStatements(other.getDeletedStatements());
        labels.addAll(other.getLabels());
        descriptions.addAll(other.getDescriptions());
        aliases.addAll(other.getAliases());
    }

    /**
     * @return true when this change is empty
     *          (no statements or terms changed)
     */
    public boolean isNull() {
        return (addedStatements.isEmpty()
                && deletedStatements.isEmpty()
                && labels.isEmpty()
                && descriptions.isEmpty()
                && aliases.isEmpty());
    }

    /**
     * Adds a label to the item. It will override any
     * existing label in this language.
     * 
     * @param label
     *      the label to add
     */
    public void addLabel(MonolingualTextValue label) {
        labels.add(label);
    }

    /**
     * Adds a description to the item. It will override any existing
     * description in this language.
     * 
     * @param description
     *      the description to add
     */
    public void addDescription(MonolingualTextValue description) {
        descriptions.add(description);
    }

    /**
     * Adds an alias to the item. It will be added to any existing
     * aliases in that language.
     * 
     * @param alias
     *      the alias to add
     */
    public void addAlias(MonolingualTextValue alias) {
        aliases.add(alias);        
    }
    
    /**
     * @return the list of updated labels
     */
    public Set<MonolingualTextValue> getLabels() {
        return labels;
    }
    
    /**
     * @return the list of updated descriptions
     */
    public Set<MonolingualTextValue> getDescriptions() {
        return descriptions;
    }
    
    /**
     * @return the list of updated aliases
     */
    public Set<MonolingualTextValue> getAliases() {
        return aliases;
    }
    
    /**
     * Group added statements in StatementGroups: useful if the
     * item is new.
     * 
     * @return a grouped version of getAddedStatements()
     */
    public List<StatementGroup> getAddedStatementGroups() {
        Map<PropertyIdValue, List<Statement>> map = new HashMap<>();
        for(Statement statement : getAddedStatements()) {
            PropertyIdValue propertyId = statement.getClaim().getMainSnak().getPropertyId();
            if (!map.containsKey(propertyId)) {
                map.put(propertyId, new ArrayList<Statement>());
            }
            map.get(propertyId).add(statement);
        }
        List<StatementGroup> result = new ArrayList<>();
        for(Map.Entry<PropertyIdValue, List<Statement>> entry : map.entrySet()) {
            result.add(new StatementGroupImpl(entry.getValue()));
        }
        return result;
    }

    /**
     * Group a list of ItemUpdates by subject: this is useful to make one single edit
     * per item.
     * 
     * @param itemDocuments
     * @return a map from item ids to merged ItemUpdate for that id
     */
    public static Map<EntityIdValue, ItemUpdate> groupBySubject(List<ItemUpdate> itemDocuments) {
        Map<EntityIdValue, ItemUpdate> map = new HashMap<EntityIdValue, ItemUpdate>();
        for(ItemUpdate update : itemDocuments) {
            if (update.isNull()) {
                continue;
            }
            
            ItemIdValue qid = update.getItemId();
            if (map.containsKey(qid)) {
                ItemUpdate oldUpdate = map.get(qid);
                oldUpdate.merge(update);
            } else {
                map.put(qid, update);
            }
        }
        return map;
    }

    /**
     * This should only be used when creating a new item.
     * This ensures that we never add an alias without adding
     * a label in the same language.
     */
    public void normalizeLabelsAndAliases() {
        // Ensure that we are only adding aliases with labels
        Set<String> labelLanguages = labels.stream()
                .map(l -> l.getLanguageCode())
                .collect(Collectors.toSet());
        System.out.println(labelLanguages);

        Set<MonolingualTextValue> filteredAliases = new HashSet<>();
        for(MonolingualTextValue alias : aliases) {
            if(!labelLanguages.contains(alias.getLanguageCode())) {
                labelLanguages.add(alias.getLanguageCode());
                labels.add(alias);
            } else {
                filteredAliases.add(alias);
            }
        }
        aliases = filteredAliases;
    }

    /**
     * Is this update about a new item?
     */
    public boolean isNew() {
        return "Q0".equals(getItemId().getId());
    }
    
    @Override
    public boolean equals(Object other) {
        if(other == null || !ItemUpdate.class.isInstance(other)) {
            return false;
        }
        ItemUpdate otherUpdate = (ItemUpdate)other;
        return qid.equals(otherUpdate.getItemId())&&
                addedStatements.equals(otherUpdate.getAddedStatements()) &&
                deletedStatements.equals(otherUpdate.getDeletedStatements()) &&
                labels.equals(otherUpdate.getLabels()) &&
                descriptions.equals(otherUpdate.getDescriptions()) &&
                aliases.equals(otherUpdate.getAliases());
    }
    
    @Override
    public int hashCode() {
        return qid.hashCode() + addedStatements.hashCode() + deletedStatements.hashCode() +
                labels.hashCode() + descriptions.hashCode() + aliases.hashCode();
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("<Update on ");
        builder.append(qid);
        builder.append("\n  Labels: ");
        builder.append(labels);
        builder.append("\n  Descriptions: ");
        builder.append(descriptions);
        builder.append("\n  Aliases: ");
        builder.append(aliases);
        builder.append("\n  Added statements: ");
        builder.append(addedStatements);
        builder.append("\n Deleted statements: ");
        builder.append(deletedStatements);
        builder.append("\n>");
        return builder.toString();
    }
}
