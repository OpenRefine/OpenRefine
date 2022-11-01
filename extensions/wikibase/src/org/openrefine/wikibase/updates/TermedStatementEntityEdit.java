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

package org.openrefine.wikibase.updates;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.helper.Validate;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A class to plan an update of an entity, after evaluating the statements but before fetching the current content of
 * the entity (this is why it does not extend StatementsUpdate).
 * 
 * @author Antonin Delpeuch
 */
public abstract class TermedStatementEntityEdit extends LabeledStatementEntityEdit {

    final Map<String, MonolingualTextValue> descriptions;
    final Map<String, MonolingualTextValue> descriptionsIfNew;
    final Map<String, List<MonolingualTextValue>> aliases;

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
    public TermedStatementEntityEdit(
            EntityIdValue id,
            List<StatementEdit> statements,
            Set<MonolingualTextValue> labels,
            Set<MonolingualTextValue> labelsIfNew,
            Set<MonolingualTextValue> descriptions,
            Set<MonolingualTextValue> descriptionsIfNew,
            Set<MonolingualTextValue> aliases) {
        super(id, statements, new HashMap<>(), new HashMap<>());
        Validate.notNull(id);
        if (statements == null) {
            statements = Collections.emptyList();
        }
        mergeSingleTermMaps(this.labels, this.labelsIfNew, labels, labelsIfNew);
        this.descriptions = new HashMap<>();
        this.descriptionsIfNew = new HashMap<>();
        mergeSingleTermMaps(this.descriptions, this.descriptionsIfNew, descriptions, descriptionsIfNew);
        this.aliases = constructTermListMap(aliases != null ? aliases : Collections.emptyList());
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
    protected TermedStatementEntityEdit(
            EntityIdValue id,
            List<StatementEdit> statements,
            Map<String, MonolingualTextValue> labels,
            Map<String, MonolingualTextValue> labelsIfNew,
            Map<String, MonolingualTextValue> descriptions,
            Map<String, MonolingualTextValue> descriptionsIfNew,
            Map<String, List<MonolingualTextValue>> aliases) {
        super(id, statements, labels, labelsIfNew);
        this.descriptions = descriptions;
        this.descriptionsIfNew = descriptionsIfNew;
        this.aliases = aliases;
    }

    /**
     * @return true when this change leaves the content of the document untouched
     */
    @Override
    public boolean isEmpty() {
        return (statements.isEmpty() &&
                labels.isEmpty() &&
                descriptions.isEmpty() &&
                aliases.isEmpty() &&
                labelsIfNew.isEmpty() &&
                descriptionsIfNew.isEmpty());
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

    protected Map<String, List<MonolingualTextValue>> constructTermListMap(Collection<MonolingualTextValue> mltvs) {
        Map<String, List<MonolingualTextValue>> result = new HashMap<>();
        for (MonolingualTextValue mltv : mltvs) {
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
