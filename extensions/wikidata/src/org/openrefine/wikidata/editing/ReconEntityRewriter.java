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
package org.openrefine.wikidata.editing;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.openrefine.wikidata.schema.entityvalues.ReconItemIdValue;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.helpers.DatamodelConverter;
import org.wikidata.wdtk.datamodel.implementation.DataObjectFactoryImpl;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

/**
 * A class that rewrites an {@link ItemUpdate}, replacing reconciled entity id
 * values by their concrete values after creation of all the new items involved.
 * 
 * If an item has not been created yet, an {@link IllegalArgumentException} will
 * be raised.
 * 
 * The subject is treated as a special case: it is returned unchanged. This is
 * because it is guaranteed not to appear in the update (but it does appear in
 * the datamodel representation as the subject is passed around to the Claim
 * objects its document contains).
 * 
 * @author Antonin Delpeuch
 *
 */
public class ReconEntityRewriter extends DatamodelConverter {

    private NewItemLibrary library;
    private ItemIdValue subject;

    /**
     * Constructor. Sets up a rewriter which uses the provided library to look up
     * qids of new items, and the subject (which should not be rewritten).
     * 
     * @param library
     * @param subject
     */
    public ReconEntityRewriter(NewItemLibrary library, ItemIdValue subject) {
        super(new DataObjectFactoryImpl());
        this.library = library;
        this.subject = subject;
    }

    @Override
    public ItemIdValue copy(ItemIdValue value) {
        if (subject.equals(value)) {
            return value;
        }
        if (value instanceof ReconItemIdValue) {
            ReconItemIdValue recon = (ReconItemIdValue) value;
            if (recon.isNew()) {
                String newId = library.getQid(recon.getReconInternalId());
                if (newId == null) {
                    throw new IllegalArgumentException(
                            "Trying to rewrite an update where a new item was not created yet.");
                }
                return Datamodel.makeItemIdValue(newId, recon.getRecon().identifierSpace);
            }
        }
        return super.copy(value);
    }

    public ItemUpdate rewrite(ItemUpdate update) {
        Set<MonolingualTextValue> labels = update.getLabels().stream().map(l -> copy(l)).collect(Collectors.toSet());
        Set<MonolingualTextValue> descriptions = update.getDescriptions().stream().map(l -> copy(l))
                .collect(Collectors.toSet());
        Set<MonolingualTextValue> aliases = update.getAliases().stream().map(l -> copy(l)).collect(Collectors.toSet());
        List<Statement> addedStatements = update.getAddedStatements().stream().map(l -> copy(l))
                .collect(Collectors.toList());
        Set<Statement> deletedStatements = update.getDeletedStatements().stream().map(l -> copy(l))
                .collect(Collectors.toSet());
        return new ItemUpdate(update.getItemId(), addedStatements, deletedStatements, labels, descriptions, aliases);
    }
}
