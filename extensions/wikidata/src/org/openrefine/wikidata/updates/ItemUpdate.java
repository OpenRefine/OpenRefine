/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package org.openrefine.wikidata.updates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.helper.Validate;
import org.wikidata.wdtk.datamodel.implementation.StatementGroupImpl;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A class to plan an update of an item, after evaluating the statements but
 * before fetching the current content of the item (this is why it does not
 * extend StatementsUpdate).
 * 
 * @author Antonin Delpeuch
 */
public class ItemUpdate {

    private final ItemIdValue qid;
    private final List<Statement> addedStatements;
    private final Set<Statement> deletedStatements;
    private final Set<MonolingualTextValue> labels;
    private final Set<MonolingualTextValue> descriptions;
    private final Set<MonolingualTextValue> aliases;

    /**
     * Constructor.
     * 
     * @param qid
     *            the subject of the document. It can be a reconciled item value for
     *            new items.
     * @param addedStatements
     *            the statements to add on the item. They should be distinct. They
     *            are modelled as a list because their insertion order matters.
     * @param deletedStatements
     *            the statements to remove from the item
     * @param labels
     *            the labels to add on the item
     * @param descriptions
     *            the descriptions to add on the item
     * @param aliases
     *            the aliases to add on the item. In theory their order should
     *            matter but in practice people rarely rely on the order of aliases
     *            so this is just kept as a set for simplicity.
     */
    @JsonCreator
    public ItemUpdate(@JsonProperty("subject") ItemIdValue qid,
            @JsonProperty("addedStatements") List<Statement> addedStatements,
            @JsonProperty("deletedStatements") Set<Statement> deletedStatements,
            @JsonProperty("labels") Set<MonolingualTextValue> labels,
            @JsonProperty("descriptions") Set<MonolingualTextValue> descriptions,
            @JsonProperty("addedAliases") Set<MonolingualTextValue> aliases) {
        Validate.notNull(qid);
        this.qid = qid;
        if (addedStatements == null) {
            addedStatements = Collections.emptyList();
        }
        this.addedStatements = addedStatements;
        if (deletedStatements == null) {
            deletedStatements = Collections.emptySet();
        }
        this.deletedStatements = deletedStatements;
        if (labels == null) {
            labels = Collections.emptySet();
        }
        this.labels = labels;
        if (descriptions == null) {
            descriptions = Collections.emptySet();
        }
        this.descriptions = descriptions;
        if (aliases == null) {
            aliases = Collections.emptySet();
        }
        this.aliases = aliases;
    }

    /**
     * @return the subject of the item
     */
    @JsonProperty("subject")
    public ItemIdValue getItemId() {
        return qid;
    }

    /**
     * Added statements are recorded as a list because their order of insertion
     * matters.
     * 
     * @return the list of all added statements
     */
    @JsonProperty("addedStatements")
    public List<Statement> getAddedStatements() {
        return addedStatements;
    }

    /**
     * @return the list of all deleted statements
     */
    @JsonProperty("deletedStatements")
    public Set<Statement> getDeletedStatements() {
        return deletedStatements;
    }

    /**
     * @return the list of updated labels
     */
    @JsonProperty("labels")
    public Set<MonolingualTextValue> getLabels() {
        return labels;
    }

    /**
     * @return the list of updated descriptions
     */
    @JsonProperty("descriptions")
    public Set<MonolingualTextValue> getDescriptions() {
        return descriptions;
    }

    /**
     * @return the list of updated aliases
     */
    @JsonProperty("addedAliases")
    public Set<MonolingualTextValue> getAliases() {
        return aliases;
    }

    /**
     * @return true when this change is empty and its subject is not new
     */
    @JsonIgnore
    public boolean isNull() {
        return isEmpty() && !isNew();
    }

    /**
     * @return true when this change leaves the content of the document untouched
     */
    @JsonIgnore
    public boolean isEmpty() {
        return (addedStatements.isEmpty() && deletedStatements.isEmpty() && labels.isEmpty() && descriptions.isEmpty()
                && aliases.isEmpty());
    }

    /**
     * Merges all the changes in other into this instance. Both updates should have
     * the same subject.
     * 
     * @param other
     *            the other change that should be merged
     */
    public ItemUpdate merge(ItemUpdate other) {
        Validate.isTrue(qid.equals(other.getItemId()));
        List<Statement> newAddedStatements = new ArrayList<>(addedStatements);
        for (Statement statement : other.getAddedStatements()) {
            if (!newAddedStatements.contains(statement)) {
                newAddedStatements.add(statement);
            }
        }
        Set<Statement> newDeletedStatements = new HashSet<>(deletedStatements);
        newDeletedStatements.addAll(other.getDeletedStatements());
        Set<MonolingualTextValue> newLabels = new HashSet<>(labels);
        newLabels.addAll(other.getLabels());
        Set<MonolingualTextValue> newDescriptions = new HashSet<>(descriptions);
        newDescriptions.addAll(other.getDescriptions());
        Set<MonolingualTextValue> newAliases = new HashSet<>(aliases);
        newAliases.addAll(other.getAliases());
        return new ItemUpdate(qid, newAddedStatements, newDeletedStatements, newLabels, newDescriptions, newAliases);
    }

    /**
     * Group added statements in StatementGroups: useful if the item is new.
     * 
     * @return a grouped version of getAddedStatements()
     */
    public List<StatementGroup> getAddedStatementGroups() {
        Map<PropertyIdValue, List<Statement>> map = new HashMap<>();
        for (Statement statement : getAddedStatements()) {
            PropertyIdValue propertyId = statement.getClaim().getMainSnak().getPropertyId();
            if (!map.containsKey(propertyId)) {
                map.put(propertyId, new ArrayList<Statement>());
            }
            map.get(propertyId).add(statement);
        }
        List<StatementGroup> result = new ArrayList<>();
        for (Map.Entry<PropertyIdValue, List<Statement>> entry : map.entrySet()) {
            result.add(new StatementGroupImpl(entry.getValue()));
        }
        return result;
    }

    /**
     * Group a list of ItemUpdates by subject: this is useful to make one single
     * edit per item.
     * 
     * @param itemDocuments
     * @return a map from item ids to merged ItemUpdate for that id
     */
    public static Map<EntityIdValue, ItemUpdate> groupBySubject(List<ItemUpdate> itemDocuments) {
        Map<EntityIdValue, ItemUpdate> map = new HashMap<>();
        for (ItemUpdate update : itemDocuments) {
            if (update.isNull()) {
                continue;
            }

            ItemIdValue qid = update.getItemId();
            if (map.containsKey(qid)) {
                ItemUpdate oldUpdate = map.get(qid);
                map.put(qid, oldUpdate.merge(update));
            } else {
                map.put(qid, update);
            }
        }
        return map;
    }

    /**
     * Is this update about a new item?
     */
    public boolean isNew() {
        return EntityIdValue.SITE_LOCAL.equals(getItemId().getSiteIri());
    }

    /**
     * This should only be used when creating a new item. This ensures that we never
     * add an alias without adding a label in the same language.
     */
    public ItemUpdate normalizeLabelsAndAliases() {
        // Ensure that we are only adding aliases with labels
        Set<String> labelLanguages = labels.stream().map(l -> l.getLanguageCode()).collect(Collectors.toSet());

        Set<MonolingualTextValue> filteredAliases = new HashSet<>();
        Set<MonolingualTextValue> newLabels = new HashSet<>(labels);
        for (MonolingualTextValue alias : aliases) {
            if (!labelLanguages.contains(alias.getLanguageCode())) {
                labelLanguages.add(alias.getLanguageCode());
                newLabels.add(alias);
            } else {
                filteredAliases.add(alias);
            }
        }
        return new ItemUpdate(qid, addedStatements, deletedStatements, newLabels, descriptions, filteredAliases);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !ItemUpdate.class.isInstance(other)) {
            return false;
        }
        ItemUpdate otherUpdate = (ItemUpdate) other;
        return qid.equals(otherUpdate.getItemId()) && addedStatements.equals(otherUpdate.getAddedStatements())
                && deletedStatements.equals(otherUpdate.getDeletedStatements())
                && labels.equals(otherUpdate.getLabels()) && descriptions.equals(otherUpdate.getDescriptions())
                && aliases.equals(otherUpdate.getAliases());
    }

    @Override
    public int hashCode() {
        return qid.hashCode() + addedStatements.hashCode() + deletedStatements.hashCode() + labels.hashCode()
                + descriptions.hashCode() + aliases.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("<Update on ");
        builder.append(qid);
        if (!labels.isEmpty()) {
            builder.append("\n  Labels: ");
            builder.append(labels);
        }
        if (!descriptions.isEmpty()) {
            builder.append("\n  Descriptions: ");
            builder.append(descriptions);
        }
        if (!aliases.isEmpty()) {
            builder.append("\n  Aliases: ");
            builder.append(aliases);
        }
        if (!addedStatements.isEmpty()) {
            builder.append("\n  Added statements: ");
            builder.append(addedStatements);
        }
        if (!deletedStatements.isEmpty()) {
            builder.append("\n  Deleted statements: ");
            builder.append(deletedStatements);
        }
        if (isNull()) {
            builder.append(" (null update)");
        }
        builder.append("\n>");
        return builder.toString();
    }

}
