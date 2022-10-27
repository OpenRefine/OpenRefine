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

package org.openrefine.wikibase.editing;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.openrefine.wikibase.schema.entityvalues.ReconEntityIdValue;
import org.openrefine.wikibase.schema.entityvalues.ReconItemIdValue;
import org.openrefine.wikibase.schema.entityvalues.ReconMediaInfoIdValue;
import org.openrefine.wikibase.schema.entityvalues.ReconPropertyIdValue;
import org.openrefine.wikibase.schema.exceptions.NewEntityNotCreatedYetException;
import org.openrefine.wikibase.updates.EntityEdit;
import org.openrefine.wikibase.updates.ItemEdit;
import org.openrefine.wikibase.updates.MediaInfoEdit;
import org.openrefine.wikibase.updates.StatementEdit;
import org.openrefine.wikibase.updates.TermedStatementEntityEdit;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.helpers.DatamodelConverter;
import org.wikidata.wdtk.datamodel.implementation.DataObjectFactoryImpl;
import org.wikidata.wdtk.datamodel.implementation.EntityIdValueImpl;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MediaInfoIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

/**
 * A class that rewrites an {@link TermedStatementEntityEdit}, replacing reconciled entity id values by their concrete
 * values after creation of all the new entities involved.
 *
 * If an entity has not been created yet, an {@link IllegalArgumentException} will be raised.
 *
 * The subject is treated as a special case: it is returned unchanged. This is because it is guaranteed not to appear in
 * the update (but it does appear in the datamodel representation as the subject is passed around to the Claim objects
 * its document contains).
 *
 * @author Antonin Delpeuch
 *
 */
public class ReconEntityRewriter extends DatamodelConverter {

    private final NewEntityLibrary library;
    private final EntityIdValue subject;

    protected static final String notCreatedYetMessage = "Trying to rewrite an update where a new entity was not created yet.";

    /**
     * Constructor. Sets up a rewriter which uses the provided library to look up ids of new entities.
     *
     * @param library
     *            the collection of entities already created
     * @param subject
     *            the subject id of the entity to rewrite
     */
    public ReconEntityRewriter(NewEntityLibrary library, EntityIdValue subject) {
        super(new DataObjectFactoryImpl());
        this.library = library;
        this.subject = subject;
    }

    @Override
    public ItemIdValue copy(ItemIdValue value) {
        if (value instanceof ReconItemIdValue) {
            ReconItemIdValue recon = (ReconItemIdValue) value;
            if (recon.isNew()) {
                String newId = library.getId(recon.getReconInternalId());
                if (newId == null) {
                    if (subject.equals(recon)) {
                        return (ItemIdValue) subject;
                    } else {
                        throw new MissingEntityIdFound(recon);
                    }
                }
                return Datamodel.makeItemIdValue(newId, recon.getRecon().identifierSpace);
            }
        }
        return super.copy(value);
    }

    @Override
    public MediaInfoIdValue copy(MediaInfoIdValue value) {
        if (value instanceof ReconMediaInfoIdValue) {
            ReconMediaInfoIdValue recon = (ReconMediaInfoIdValue) value;
            if (recon.isNew()) {
                String newId = library.getId(recon.getReconInternalId());
                if (newId == null) {
                    if (subject.equals(recon)) {
                        return (MediaInfoIdValue) subject;
                    } else {
                        throw new MissingEntityIdFound(recon);
                    }
                }
                return Datamodel.makeMediaInfoIdValue(newId, recon.getRecon().identifierSpace);
            }
        }
        return super.copy((MediaInfoIdValue) value);
    }

    @Override
    public PropertyIdValue copy(PropertyIdValue value) {
        if (value instanceof ReconPropertyIdValue) {
            ReconPropertyIdValue recon = (ReconPropertyIdValue) value;
            if (recon.isNew()) {
                String newId = library.getId(recon.getReconInternalId());
                if (newId == null) {
                    if (subject.equals(recon)) {
                        return (PropertyIdValue) subject;
                    } else {
                        throw new MissingEntityIdFound(recon);
                    }
                }
                return Datamodel.makePropertyIdValue(newId, recon.getRecon().identifierSpace);
            }
        }
        return super.copy((PropertyIdValue) value);
    }

    @Override
    public EntityIdValue visit(EntityIdValue value) {
        if (value.isPlaceholder() && value instanceof ReconEntityIdValue) {
            ReconEntityIdValue reconIdValue = (ReconEntityIdValue) value;
            String newId = library.getId(reconIdValue.getReconInternalId());
            if (newId == null) {
                if (subject.equals(reconIdValue)) {
                    return subject;
                } else {
                    throw new MissingEntityIdFound(reconIdValue);
                }
            }
            return EntityIdValueImpl.fromId(newId, reconIdValue.getRecon().identifierSpace);
        } else {
            return (EntityIdValue) super.visit(value);
        }
    }

    public StatementEdit copy(StatementEdit value) {
        return new StatementEdit(copy(value.getStatement()), value.getMerger(), value.getMode());
    }

    /**
     * Rewrite an edit, replacing references to all entities already created by their fresh identifiers. The subject id
     * might not have been created already, in which case it will be left untouched. All the other entities need to have
     * been created already.
     *
     * @param edit
     *            the edit to rewrite
     * @return the rewritten update
     * @throws NewEntityNotCreatedYetException
     *             if any non-subject entity had not been created yet
     */
    public EntityEdit rewrite(EntityEdit edit) throws NewEntityNotCreatedYetException {
        try {
            EntityIdValue subject = (EntityIdValue) copyValue(edit.getEntityId());
            if (edit instanceof ItemEdit) {
                ItemEdit update = (ItemEdit) edit;
                Set<MonolingualTextValue> labels = update.getLabels().stream().map(l -> copy(l)).collect(Collectors.toSet());
                Set<MonolingualTextValue> labelsIfNew = update.getLabelsIfNew().stream().map(l -> copy(l)).collect(Collectors.toSet());
                Set<MonolingualTextValue> descriptions = update.getDescriptions().stream().map(l -> copy(l))
                        .collect(Collectors.toSet());
                Set<MonolingualTextValue> descriptionsIfNew = update.getDescriptionsIfNew().stream().map(l -> copy(l))
                        .collect(Collectors.toSet());
                Set<MonolingualTextValue> aliases = update.getAliases().stream().map(l -> copy(l)).collect(Collectors.toSet());
                List<StatementEdit> statements = update.getStatementEdits().stream().map(l -> copy(l))
                        .collect(Collectors.toList());
                return new ItemEdit(subject, statements, labels, labelsIfNew, descriptions, descriptionsIfNew, aliases);
            } else if (edit instanceof MediaInfoEdit) {
                MediaInfoEdit update = (MediaInfoEdit) edit;
                Set<MonolingualTextValue> labels = update.getLabels().stream().map(l -> copy(l)).collect(Collectors.toSet());
                Set<MonolingualTextValue> labelsIfNew = update.getLabelsIfNew().stream().map(l -> copy(l)).collect(Collectors.toSet());
                List<StatementEdit> statements = update.getStatementEdits().stream().map(l -> copy(l))
                        .collect(Collectors.toList());
                return new MediaInfoEdit(subject, statements, labels, labelsIfNew, update.getFilePath(),
                        update.getFileName(), update.getWikitext(), update.isOverridingWikitext());
            } else {
                throw new IllegalStateException(
                        "Rewriting of entities of this type (for subject id " + edit.getEntityId() + ") not supported yet");
            }
        } catch (MissingEntityIdFound e) {
            throw new NewEntityNotCreatedYetException(e.value);
        }
    }

    /**
     * Unchecked version of {@link NewEntityNotCreatedYetException}, for internal use only.
     */
    protected static class MissingEntityIdFound extends Error {

        private static final long serialVersionUID = 1L;
        protected EntityIdValue value;

        public MissingEntityIdFound(EntityIdValue missing) {
            this.value = missing;
        }
    }

}
