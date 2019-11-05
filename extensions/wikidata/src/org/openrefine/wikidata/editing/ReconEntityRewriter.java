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
import org.openrefine.wikidata.schema.exceptions.NewItemNotCreatedYetException;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.helpers.DatamodelConverter;
import org.wikidata.wdtk.datamodel.implementation.DataObjectFactoryImpl;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
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

	private final NewItemLibrary library;
	private final ItemIdValue subject;

	protected static final String notCreatedYetMessage = "Trying to rewrite an update where a new item was not created yet.";

	/**
	 * Constructor. Sets up a rewriter which uses the provided library to look up
	 * qids of new items.
	 *
	 * @param library
	 *      the collection of items already created
	 * @param subject
	 *      the subject id of the entity to rewrite
	 */
	public ReconEntityRewriter(NewItemLibrary library, ItemIdValue subject) {
		super(new DataObjectFactoryImpl());
		this.library = library;
		this.subject = subject;
	}

	@Override
	public ItemIdValue copy(ItemIdValue value) {
		if (value instanceof ReconItemIdValue) {
			ReconItemIdValue recon = (ReconItemIdValue) value;
			if (recon.isNew()) {
				String newId = library.getQid(recon.getReconInternalId());
				if (newId == null) {
					if (subject.equals(recon)) {
						return subject;
					} else {
						throw new MissingEntityIdFound(recon);
					}
				}
				return Datamodel.makeItemIdValue(newId, recon.getRecon().identifierSpace);
			}
		}
		return super.copy(value);
	}

	/**
	 * Rewrite an update, replacing references to all entities already
	 * created by their fresh identifiers. The subject id might not have been
	 * created already, in which case it will be left untouched. All the other
	 * entities need to have been created already.
	 *
	 * @param update
	 *      the update to rewrite
	 * @return
	 *      the rewritten update
	 * @throws NewItemNotCreatedYetException
	 *      if any non-subject entity had not been created yet
	 */
	public ItemUpdate rewrite(ItemUpdate update) throws NewItemNotCreatedYetException {
		try {
			ItemIdValue subject = copy(update.getItemId());
			Set<MonolingualTextValue> labels = update.getLabels().stream().map(l -> copy(l)).collect(Collectors.toSet());
			Set<MonolingualTextValue> labelsIfNew = update.getLabelsIfNew().stream().map(l -> copy(l)).collect(Collectors.toSet());
			Set<MonolingualTextValue> descriptions = update.getDescriptions().stream().map(l -> copy(l))
					.collect(Collectors.toSet());
			Set<MonolingualTextValue> descriptionsIfNew = update.getDescriptionsIfNew().stream().map(l -> copy(l))
					.collect(Collectors.toSet());
			Set<MonolingualTextValue> aliases = update.getAliases().stream().map(l -> copy(l)).collect(Collectors.toSet());
			List<Statement> addedStatements = update.getAddedStatements().stream().map(l -> copy(l))
					.collect(Collectors.toList());
			Set<Statement> deletedStatements = update.getDeletedStatements().stream().map(l -> copy(l))
					.collect(Collectors.toSet());
			return new ItemUpdate(subject, addedStatements, deletedStatements, labels, labelsIfNew, descriptions, descriptionsIfNew, aliases);
		} catch(MissingEntityIdFound e) {
			throw new NewItemNotCreatedYetException(e.value);
		}
	}

	/**
	 * Unchecked version of {@class NewItemNotCreatedYetException}, for internal use only.
	 */
	protected static class MissingEntityIdFound extends Error {
		private static final long serialVersionUID = 1L;
		protected EntityIdValue value;
		public MissingEntityIdFound(EntityIdValue missing) {
			this.value = missing;
		}
	}

}
