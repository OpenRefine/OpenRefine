package org.openrefine.wikidata.editing;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.openrefine.wikidata.schema.entityvalues.ReconEntityIdValue;
import org.openrefine.wikidata.schema.entityvalues.ReconItemIdValue;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.helpers.DatamodelConverter;
import org.wikidata.wdtk.datamodel.implementation.DataObjectFactoryImpl;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;

/**
 * A class that rewrites an {@link ItemUpdate},
 * replacing reconciled entity id values by their concrete
 * values after creation of all the new items involved.
 * 
 * If an item has not been created yet, an {@link IllegalArgumentException}
 * will be raised.
 * 
 * The subject is treated as a special case: it is returned unchanged.
 * This is because it is guaranteed not to appear in the update (but
 * it does appear in the datamodel representation as the subject is passed around
 * to the Claim objects its document contains).
 * 
 * @author Antonin Delpeuch
 *
 */
public class ReconEntityRewriter extends DatamodelConverter {
    
    private NewItemLibrary library;
    private ItemIdValue subject;
    
    /**
     * Constructor. Sets up a rewriter which uses the provided library
     * to look up qids of new items, and the subject (which should not be
     * rewritten).
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
        if(subject.equals(value)) {
            return value;
        }
        if(value instanceof ReconItemIdValue) {
            ReconItemIdValue recon = (ReconItemIdValue)value;
            if(recon.isNew()) {
                String newId = library.getQid(recon.getReconInternalId());
                if(newId == null) {
                    throw new IllegalArgumentException(
                            "Trying to rewrite an update where a new item was not created yet.");
                }
                return Datamodel.makeItemIdValue(newId,
                        recon.getSiteIri());
            }
        }
        return super.copy(value);
    }
    
    public ItemUpdate rewrite(ItemUpdate update) {
        Set<MonolingualTextValue> labels = update.getLabels().stream()
                .map(l -> copy(l)).collect(Collectors.toSet());
        Set<MonolingualTextValue> descriptions = update.getDescriptions().stream()
                .map(l -> copy(l)).collect(Collectors.toSet());
        Set<MonolingualTextValue> aliases = update.getAliases().stream()
                .map(l -> copy(l)).collect(Collectors.toSet());
        List<Statement> addedStatements = update.getAddedStatements().stream()
                .map(l -> copy(l)).collect(Collectors.toList());
        Set<Statement> deletedStatements = update.getDeletedStatements().stream()
                .map(l -> copy(l)).collect(Collectors.toSet());
        return new ItemUpdate(update.getItemId(), addedStatements,
                deletedStatements, labels, descriptions, aliases);
    }
}
