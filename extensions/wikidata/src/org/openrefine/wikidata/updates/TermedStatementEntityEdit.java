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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.NotImplementedException;
import org.jsoup.helper.Validate;
import org.openrefine.wikidata.schema.strategies.StatementEditingMode;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.helpers.StatementUpdateBuilder;
import org.wikidata.wdtk.datamodel.interfaces.AliasUpdate;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.EntityUpdate;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MediaInfoDocument;
import org.wikidata.wdtk.datamodel.interfaces.MediaInfoIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementDocument;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.StatementUpdate;
import org.wikidata.wdtk.datamodel.interfaces.TermUpdate;
import org.wikidata.wdtk.datamodel.interfaces.TermedStatementDocument;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A class to plan an update of an entity, after evaluating the statements but
 * before fetching the current content of the entity (this is why it does not
 * extend StatementsUpdate).
 * 
 * @author Antonin Delpeuch
 */
public class TermedStatementEntityEdit {

    private final EntityIdValue id;
    private final List<StatementEdit> statements;
    private final Map<String, MonolingualTextValue> labels;
    private final Map<String, MonolingualTextValue> labelsIfNew;
    private final Map<String, MonolingualTextValue> descriptions;
    private final Map<String, MonolingualTextValue> descriptionsIfNew;
    private final Map<String, List<MonolingualTextValue>> aliases;

    /**
     * Constructor.
     * 
     * @param id
     *            the subject of the document. It can be a reconciled entity value for
     *            new entities.
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
     *            the aliases to add on the item. In theory their order should
     *            matter but in practice people rarely rely on the order of aliases
     *            so this is just kept as a set for simplicity.
     */
    public TermedStatementEntityEdit(
    		EntityIdValue id,
    		List<StatementEdit> statements,
            Set<MonolingualTextValue> labels,
            Set<MonolingualTextValue> labelsIfNew,
            Set<MonolingualTextValue> descriptions,
            Set<MonolingualTextValue> descriptionsIfNew,
            Set<MonolingualTextValue> aliases) {
        Validate.notNull(id);
        this.id = id;
        if (statements == null) {
        	statements = Collections.emptyList();
        }
        this.statements = statements;
        this.labels = new HashMap<>();
        this.labelsIfNew = new HashMap<>();
        mergeSingleTermMaps(this.labels, this.labelsIfNew, labels, labelsIfNew);
        this.descriptions = new HashMap<>();
        this.descriptionsIfNew = new HashMap<>();
        mergeSingleTermMaps(this.descriptions, this.descriptionsIfNew, descriptions, descriptionsIfNew);
        this.aliases = constructTermListMap(aliases != null ? aliases : Collections.emptyList());
    }
    
    /**
     * Private constructor to avoid re-constructing term maps when
     * merging two entity updates.
     * 
     * No validation is done on the arguments, they all have to be non-null.
     * 
     * @param id
     * 		the subject of the update
     * @param addedStatements
     *      the statements to add
     * @param deletedStatements
     *      the statements to delete
     * @param labels
     *      the labels to add on the entity, overriding any existing one in that language
     * @param labelsIfNew
     *            the labels to add on the entity, only if no label for that language exists
     * @param descriptions
     *            the descriptions to add on the item, overriding any existing one in that language
     * @param descriptionsIfNew
     *            the descriptions to add on the item, only if no description for that language exists
     * @param descriptions
     *      the descriptions to add
     * @param aliases
     *      the aliases to add
     */
    private TermedStatementEntityEdit(
            EntityIdValue id,
    		List<StatementEdit> statements,
    		Map<String, MonolingualTextValue> labels,
    		Map<String, MonolingualTextValue> labelsIfNew,
    		Map<String, MonolingualTextValue> descriptions,
    		Map<String, MonolingualTextValue> descriptionsIfNew,
    		Map<String, List<MonolingualTextValue>> aliases) {
        this.id = id;
    	this.statements = statements;
    	this.labels = labels;
    	this.labelsIfNew = labelsIfNew;
    	this.descriptions = descriptions;
    	this.descriptionsIfNew = descriptionsIfNew;
    	this.aliases = aliases;
    }

    /**
     * @return the subject of the entity
     */
    @JsonProperty("subject")
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
     * @return the list of updated descriptions, overriding existing ones
     */
    @JsonProperty("descriptions")
    public Set<MonolingualTextValue> getDescriptions() {
        return descriptions.values().stream().collect(Collectors.toSet());
    }
    
    /**
     * @return the list of updated descriptions, only added if new
     */
    @JsonProperty("descriptionsIfNew")
    public Set<MonolingualTextValue> getDescriptionsIfNew() {
        return descriptionsIfNew.values().stream().collect(Collectors.toSet());
    }

    /**
     * @return the list of updated aliases
     */
    @JsonProperty("addedAliases")
    public Set<MonolingualTextValue> getAliases() {
        return aliases.values().stream().flatMap(List::stream).collect(Collectors.toSet());
    }

    /**
     * @return the list of statement updates
     */
    @JsonIgnore // because we expose them as StatementGroupEdits later on
    public List<StatementEdit> getStatementEdits() {
    	return statements;
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
        return (statements.isEmpty() &&
        		labels.isEmpty() &&
        		descriptions.isEmpty() &&
        		aliases.isEmpty() &&
        		labelsIfNew.isEmpty() &&
        		descriptionsIfNew.isEmpty());
    }

    /**
     * Merges all the changes in other with this instance. Both updates should have
     * the same subject. Changes coming from `other` have priority over changes
     * from this instance. This instance is not modified, the merged update is returned
     * instead.
     * 
     * @param other
     *            the other change that should be merged
     */
    public TermedStatementEntityEdit merge(TermedStatementEntityEdit other) {
        Validate.isTrue(id.equals(other.getEntityId()));
        List<StatementEdit> newStatements = new ArrayList<>(statements);
        for (StatementEdit statement : other.getStatementEdits()) {
            if (!newStatements.contains(statement)) {
                newStatements.add(statement);
            }
        }
        Map<String,MonolingualTextValue> newLabels = new HashMap<>(labels);
        Map<String,MonolingualTextValue> newLabelsIfNew = new HashMap<>(labelsIfNew);
        mergeSingleTermMaps(newLabels, newLabelsIfNew, other.getLabels(), other.getLabelsIfNew());
        Map<String,MonolingualTextValue> newDescriptions = new HashMap<>(descriptions);
        Map<String,MonolingualTextValue> newDescriptionsIfNew = new HashMap<>(descriptionsIfNew);
        mergeSingleTermMaps(newDescriptions, newDescriptionsIfNew, other.getDescriptions(), other.getDescriptionsIfNew());
        Map<String,List<MonolingualTextValue>> newAliases = new HashMap<>(aliases);
        for(MonolingualTextValue alias : other.getAliases()) {
        	List<MonolingualTextValue> aliases = newAliases.get(alias.getLanguageCode());
        	if(aliases == null) {
        		aliases = new LinkedList<>();
        		newAliases.put(alias.getLanguageCode(), aliases);
        	}
        	if(!aliases.contains(alias)) {
        		aliases.add(alias);
        	}
        }
        return new TermedStatementEntityEdit(id, newStatements, newLabels, newLabelsIfNew, newDescriptions, newDescriptionsIfNew, newAliases);
    }    

    /**
     * Group added statements in StatementGroups: useful if the entity is new.
     * 
     * @return a grouped version of getAddedStatements()
     */
    @JsonProperty("statementGroups")
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
     * @return the statements which should be added or merged with
     * the existing ones on the item.
     */
    @JsonIgnore
    public List<Statement> getAddedStatements() {
    	return statements.stream()
    			.filter(statement -> statement.getMode() != StatementEditingMode.DELETE)
    			.map(StatementEdit::getStatement)
    			.collect(Collectors.toList());
    }
    
    /**
     * @return the statements which should be deleted from the item.
     */
    @JsonIgnore
    public List<Statement> getDeletedStatements() {
    	return statements.stream()
    			.filter(statement -> statement.getMode() == StatementEditingMode.DELETE)
    			.map(StatementEdit::getStatement)
    			.collect(Collectors.toList());
    }
    
    /**
     * Group a list of TermedStatementEntityUpdates by subject: this is useful to make one single
     * edit per entity.
     * 
     * @param entityDocuments
     * @return a map from entity ids to merged TermedStatementEntityUpdate for that id
     */
    public static Map<EntityIdValue, TermedStatementEntityEdit> groupBySubject(List<TermedStatementEntityEdit> entityDocuments) {
        Map<EntityIdValue, TermedStatementEntityEdit> map = new HashMap<>();
        for (TermedStatementEntityEdit update : entityDocuments) {
            if (update.isNull()) {
                continue;
            }

            EntityIdValue qid = update.getEntityId();
            if (map.containsKey(qid)) {
            	TermedStatementEntityEdit oldUpdate = map.get(qid);
                map.put(qid, oldUpdate.merge(update));
            } else {
                map.put(qid, update);
            }
        }
        return map;
    }

    /**
     * Is this update about a new entity?
     */
    @JsonProperty("new")
    public boolean isNew() {
        return EntityIdValue.SITE_LOCAL.equals(getEntityId().getSiteIri());
    }

    /**
     * This should only be used when creating a new entity. This ensures that we never
     * add an alias without adding a label in the same language.
     */
    public TermedStatementEntityEdit normalizeLabelsAndAliases() {
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
        return new TermedStatementEntityEdit(id, statements,
        		newLabels, Collections.emptyMap(), newDescriptions, Collections.emptyMap(),
        		constructTermListMap(filteredAliases));
    }
    
    /**
     * In case the subject id is new, returns the corresponding new item document
     * to be created.
     */
    public TermedStatementDocument toNewEntity() {
    	Validate.isTrue(isNew(), "Cannot create a corresponding entity document for an edit on an existing entity.");
    	if (id instanceof ItemIdValue) {
	    	return Datamodel.makeItemDocument((ItemIdValue) id,
	                getLabels().stream().collect(Collectors.toList()),
	                getDescriptions().stream().collect(Collectors.toList()),
	                getAliases().stream().collect(Collectors.toList()),
	                getStatementGroupsForNewEntity(),
	                Collections.emptyMap());
    	} else {
    		throw new NotImplementedException("Creating new entities of type "+id.getEntityType()+" is not supported yet.");
    	}
    }
    
    /**
     * In case the subject id is not new, returns the corresponding update given
     * the current state of the entity.
     */
    public EntityUpdate toEntityUpdate(EntityDocument entityDocument) {
    	Validate.isFalse(isNew(), "Cannot create a corresponding entity update for a creation of a new entity.");
        if (id instanceof ItemIdValue) {
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
            
			return Datamodel.makeItemUpdate((ItemIdValue) getEntityId(),
                    entityDocument.getRevisionId(),
                    labelUpdate,
                    descriptionUpdate,
                    aliasMap,
                    statementUpdate,
                    Collections.emptyList(),
                    Collections.emptyList());
			
        } else if (id instanceof MediaInfoIdValue) {
        	MediaInfoDocument mediaInfoDocument = (MediaInfoDocument) entityDocument;
        	
        	// Labels (captions)
            List<MonolingualTextValue> labels = getLabels().stream().collect(Collectors.toList());
            labels.addAll(getLabelsIfNew().stream()
                  .filter(label -> !mediaInfoDocument.getLabels().containsKey(label.getLanguageCode())).collect(Collectors.toList()));
            TermUpdate labelUpdate = Datamodel.makeTermUpdate(labels, Collections.emptyList());
            
            // Statements
            StatementUpdate statementUpdate = toStatementUpdate(mediaInfoDocument);
            
            return Datamodel.makeMediaInfoUpdate(
            		(MediaInfoIdValue) id,
                    entityDocument.getRevisionId(),
                    labelUpdate,
                    statementUpdate);
        } else {
    		throw new NotImplementedException("Editing entities of type "+id.getEntityType()+" is not supported yet.");
    	}
    }
    
    /**
     * Generates the statement update given the current statement groups on the entity.
     * @param currentDocument 
     * @return
     */
    protected StatementUpdate toStatementUpdate(StatementDocument currentDocument) {
    	Map<PropertyIdValue, List<StatementEdit>> groupedEdits = statements.stream()
    			.collect(Collectors.groupingBy(StatementEdit::getPropertyId));
    	StatementUpdateBuilder builder = StatementUpdateBuilder.create(currentDocument.getEntityId());
    	
    	for (Entry<PropertyIdValue, List<StatementEdit>> entry : groupedEdits.entrySet()) {
    		StatementGroupEdit statementGroupEdit = new StatementGroupEdit(entry.getValue());
    		StatementGroup statementGroup = currentDocument.findStatementGroup(entry.getKey().getId());
    		statementGroupEdit.contributeToStatementUpdate(builder, statementGroup);
    	}
    	return builder.build();
    }
    
    /**
     * Generates the statement groups which should appear on this entity if it is created
     * as new.
     * @todo those statements are not currently deduplicated among themselves
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

    @Override
    public boolean equals(Object other) {
        if (other == null || !TermedStatementEntityEdit.class.isInstance(other)) {
            return false;
        }
        TermedStatementEntityEdit otherUpdate = (TermedStatementEntityEdit) other;
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
    
    /**
     * Helper function to merge dictionaries of terms to override or provide.
     * @param currentTerms
     * 		current map of terms to override
     * @param currentTermsIfNew
     *      current map of terms to provide if not already there
     * @param newTerms
     *      new terms to override
     * @param newTermsIfNew
     *      new terms to provide if not already there
     */
    private static void mergeSingleTermMaps(
    		Map<String,MonolingualTextValue> currentTerms,
    		Map<String,MonolingualTextValue> currentTermsIfNew,
    		Set<MonolingualTextValue> newTerms,
    		Set<MonolingualTextValue> newTermsIfNew) {
    	for(MonolingualTextValue otherLabel : newTerms) {
        	String languageCode = otherLabel.getLanguageCode();
        	currentTerms.put(languageCode, otherLabel);
        	if (currentTermsIfNew.containsKey(languageCode)) {
        		currentTermsIfNew.remove(languageCode);
        	}
        }
        for(MonolingualTextValue otherLabel : newTermsIfNew) {
        	String languageCode = otherLabel.getLanguageCode();
        	if (!currentTermsIfNew.containsKey(languageCode) && !currentTerms.containsKey(languageCode)) {
        		currentTermsIfNew.put(languageCode, otherLabel);
        	}
        }
    }

    protected Map<String, List<MonolingualTextValue>> constructTermListMap(Collection<MonolingualTextValue> mltvs) {
    	Map<String,List<MonolingualTextValue>> result = new HashMap<>();
    	for(MonolingualTextValue mltv : mltvs) {
    		List<MonolingualTextValue> values = result.get(mltv.getLanguageCode());
    		if (values == null) {
    			values = new LinkedList<>();
    			result.put(mltv.getLanguageCode(), values);
    		}
    		values.add(mltv);
    	}
    	return result;
    }
}
