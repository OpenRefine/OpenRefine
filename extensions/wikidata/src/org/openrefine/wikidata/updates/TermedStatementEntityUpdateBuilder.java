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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.helper.Validate;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

/**
 * Constructs a {@link ItemUpdate} incrementally.
 * 
 * @author Antonin Delpeuch
 *
 */
public class TermedStatementEntityUpdateBuilder {

    private EntityIdValue id;
    private List<Statement> addedStatements;
    private Set<Statement> deletedStatements;
    private Set<MonolingualTextValue> labels;
    private Set<MonolingualTextValue> labelsIfNew;
    private Set<MonolingualTextValue> descriptions;
    private Set<MonolingualTextValue> descriptionsIfNew;
    private Set<MonolingualTextValue> aliases;
    private boolean built;

    /**
     * Constructor.
     * 
     * @param qid
     *            the subject of the document. It can be a reconciled item value for
     *            new items.
     */
    public TermedStatementEntityUpdateBuilder(EntityIdValue id) {
        Validate.notNull(id);
        this.id = id;
        this.addedStatements = new ArrayList<>();
        this.deletedStatements = new HashSet<Statement>();
        this.labels = new HashSet<MonolingualTextValue>();
        this.labelsIfNew = new HashSet<MonolingualTextValue>();
        this.descriptions = new HashSet<MonolingualTextValue>();
        this.descriptionsIfNew = new HashSet<MonolingualTextValue>();
        this.aliases = new HashSet<MonolingualTextValue>();
        this.built = false;
    }

    /**
     * Mark a statement for insertion. If it matches an existing statement, it will
     * update the statement instead.
     * 
     * @param statement
     *            the statement to add or update
     */
    public TermedStatementEntityUpdateBuilder addStatement(Statement statement) {
        Validate.isTrue(!built, "ItemUpdate has already been built");
        addedStatements.add(statement);
        return this;
    }

    /**
     * Mark a statement for deletion. If no such statement exists, nothing will be
     * deleted.
     * 
     * @param statement
     *            the statement to delete
     */
    public TermedStatementEntityUpdateBuilder deleteStatement(Statement statement) {
        Validate.isTrue(!built, "ItemUpdate has already been built");
        deletedStatements.add(statement);
        return this;
    }

    /**
     * Add a list of statement, as in {@link addStatement}.
     * 
     * @param statements
     *            the statements to add
     */
    public TermedStatementEntityUpdateBuilder addStatements(Set<Statement> statements) {
        Validate.isTrue(!built, "ItemUpdate has already been built");
        addedStatements.addAll(statements);
        return this;
    }

    /**
     * Delete a list of statements, as in {@link deleteStatement}.
     * 
     * @param statements
     *            the statements to delete
     */
    public TermedStatementEntityUpdateBuilder deleteStatements(Set<Statement> statements) {
        Validate.isTrue(!built, "ItemUpdate has already been built");
        deletedStatements.addAll(statements);
        return this;
    }

    /**
     * Adds a label to the item.
     * 
     * @param label
     *            the label to add
     * @param override
     *            whether the label should be added even if there is already a label in that language
     */
    public TermedStatementEntityUpdateBuilder addLabel(MonolingualTextValue label, boolean override) {
        Validate.isTrue(!built, "ItemUpdate has already been built");
        if (override) {
        	labels.add(label);
        } else {
        	labelsIfNew.add(label);
        }
        return this;
    }

    /**
     * Adds a list of labels to the item.
     * 
     * @param labels
     *            the labels to add
     * @param override
     *            whether the label should be added even if there is already a label in that language
     */
    public TermedStatementEntityUpdateBuilder addLabels(Set<MonolingualTextValue> labels, boolean override) {
        Validate.isTrue(!built, "ItemUpdate has already been built");
        if (override) {
        	this.labels.addAll(labels);
        } else {
        	labelsIfNew.addAll(labels);
        }
        return this;
    }

    /**
     * Adds a description to the item.
     * 
     * @param description
     *            the description to add
     * @param override
     *            whether the description should be added even if there is already a description in that language
     */
    public TermedStatementEntityUpdateBuilder addDescription(MonolingualTextValue description, boolean override) {
        Validate.isTrue(!built, "ItemUpdate has already been built");
        if (override) {
        	descriptions.add(description);
        } else {
        	descriptionsIfNew.add(description);
        }
        return this;
    }

    /**
     * Adds a list of descriptions to the item.
     * 
     * @param descriptions
     *            the descriptions to add
     * @param override
     *            whether the description should be added even if there is already a description in that language
     */
    public TermedStatementEntityUpdateBuilder addDescriptions(Set<MonolingualTextValue> descriptions, boolean override) {
        Validate.isTrue(!built, "ItemUpdate has already been built");
        if (override) {
        	this.descriptions.addAll(descriptions);
        } else {
        	descriptionsIfNew.addAll(descriptions);
        }
        return this;
    }

    /**
     * Adds an alias to the item. It will be added to any existing aliases in that
     * language.
     * 
     * @param alias
     *            the alias to add
     */
    public TermedStatementEntityUpdateBuilder addAlias(MonolingualTextValue alias) {
        Validate.isTrue(!built, "ItemUpdate has already been built");
        aliases.add(alias);
        return this;
    }

    /**
     * Adds a list of aliases to the item. They will be added to any existing
     * aliases in each language.
     * 
     * @param aliases
     *            the aliases to add
     */
    public TermedStatementEntityUpdateBuilder addAliases(Set<MonolingualTextValue> aliases) {
        Validate.isTrue(!built, "ItemUpdate has already been built");
        this.aliases.addAll(aliases);
        return this;
    }

    /**
     * Constructs the {@link ItemUpdate}.
     * 
     * @return
     */
    public TermedStatementEntityUpdate build() {
        built = true;
        return new TermedStatementEntityUpdate(id, addedStatements, deletedStatements, labels, labelsIfNew, descriptions, descriptionsIfNew, aliases);
    }

}
