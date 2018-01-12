package org.openrefine.wikidata.schema.entityvalues;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.wikidata.wdtk.datamodel.helpers.Hash;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ValueVisitor;

import com.google.refine.model.Recon;

/**
 * An EntityIdValue that holds not just the id but also
 * the label as fetched by either the reconciliation interface
 * or the suggester and its type, both stored as reconciliation
 * candidates.
 * 
 * This label will be localized depending on the language chosen
 * by the user for OpenRefine's interface. Storing it lets us
 * reuse it later on without having to re-fetch it.
 * 
 * Storing the types also lets us perform some constraint checks
 * without re-fetching the types of many items.
 * @author antonin
 *
 */
public abstract class ReconEntityIdValue implements PrefetchedEntityIdValue {
    
    private Recon _recon;
    private String _cellValue;
    
    public ReconEntityIdValue(Recon match, String cellValue) {
        _recon = match;
        _cellValue = cellValue;
    }
    
    protected boolean isMatched() {
        return Recon.Judgment.Matched.equals(_recon.judgment) && _recon.match != null;
    }
    
    protected boolean isNew() {
        return !isMatched();
    }
    
    public String getLabel() {
        if (isMatched()) {
            return _recon.match.name;
        } else {
            return _cellValue;
        }
    }

    public List<String> getTypes() {
        if (isMatched()) {
            return Arrays.asList(_recon.match.types);
        } else {
            return new ArrayList<>();
        }
    }
    
    @Override
    public abstract String getEntityType();

    /**
     * Returns the id of the reconciled item
     */
    @Override
    public String getId() {
        if (isMatched()) {
            return _recon.match.id;
        } else if (ET_ITEM.equals(getEntityType())) {
            return "Q0";
        } else if (ET_PROPERTY.equals(getEntityType())) {
            return "P0";
        }
        return null;
    }

    @Override
    public String getSiteIri() {
        return _recon.identifierSpace;
    }

    @Override
    public String getIri() {
        return getSiteIri() + getId();
    }

    @Override
    public <T> T accept(ValueVisitor<T> valueVisitor) {
        return valueVisitor.visit(this);
    }

    /**
     * Equality check is important when we gather
     * all ItemUpdates related to an ItemId.
     * 
     * The label is ignored in the equality check.
     */
    @Override
    public boolean equals(Object other) {
        if (other == null ||
            !EntityIdValue.class.isInstance(other)) {
            return false;
        }
        
        if (ReconEntityIdValue.class.isInstance(other)) {
            final ReconEntityIdValue reconOther = (ReconEntityIdValue)other;
            
            if (isNew() != reconOther.isNew()) {
                return false;
            } else if (isNew()) {
                // This ensures compliance with OR's notion of new items
                // (it is possible that two cells are reconciled to the same
                // new item, in which case they share the same internal recon id).
                return getRecon().id == reconOther.getRecon().id;
            }
        }
        
        final EntityIdValue otherNew = (EntityIdValue)other;
        return getIri().equals(otherNew.getIri());
    }
    
    public long getReconInternalId() {
        return getRecon().id;
    }
    
    @Override
    public int hashCode() {
        if (isMatched()) {
            return Hash.hashCode(this);
        } else {
            return (int) _recon.id;
        }
    }
    
    protected Recon getRecon() {
        return _recon;
    }
}
