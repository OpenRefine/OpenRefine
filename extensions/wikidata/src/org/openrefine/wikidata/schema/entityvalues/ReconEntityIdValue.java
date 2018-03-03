package org.openrefine.wikidata.schema.entityvalues;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.wikidata.wdtk.datamodel.helpers.Equality;
import org.wikidata.wdtk.datamodel.helpers.Hash;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ValueVisitor;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
 * 
 * @author antonin
 *
 */
public abstract class ReconEntityIdValue implements PrefetchedEntityIdValue {
    
    private Recon _recon;
    private String _cellValue;
    
    public ReconEntityIdValue(Recon match, String cellValue) {
        _recon = match;
        _cellValue = cellValue;
        assert (Recon.Judgment.Matched.equals(_recon.judgment) ||
                Recon.Judgment.New.equals(_recon.judgment));
    }
    
    @JsonIgnore
    public boolean isMatched() {
        return Recon.Judgment.Matched.equals(_recon.judgment) && _recon.match != null;
    }
    
    @JsonIgnore
    public boolean isNew() {
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
     * Returns the integer used internally in OpenRefine to identify the new
     * item.
     * 
     * @return
     *     the judgment history entry id of the reconciled cell
     */
    public long getReconInternalId() {
        return getRecon().judgmentHistoryEntry;
    }
    
    
    /**
     * Returns the reconciliation object corresponding to this entity.
     * 
     * @return
     *     the full reconciliation metadata of the corresponding cell
     */
    public Recon getRecon() {
        return _recon;
    }
    
    /**
     * Returns the id of the reconciled item
     */
    @Override
    public String getId() {
        if (isMatched()) {
            return _recon.match.id;
        } else if (ET_ITEM.equals(getEntityType())) {
            return "Q"+getReconInternalId();
        } else if (ET_PROPERTY.equals(getEntityType())) {
            return "P"+getReconInternalId();
        }
        return null;
    }

    @Override
    public String getSiteIri() {
        if (isMatched()) {
            return _recon.identifierSpace;
        } else {
            return EntityIdValue.SITE_LOCAL;
        }
    }

    @Override
    public String getIri() {
        return getSiteIri() + getId();
    }

    @Override
    public <T> T accept(ValueVisitor<T> valueVisitor) {
        return valueVisitor.visit(this);
    }
    
    @Override
    public boolean equals(Object other) {
        return Equality.equalsEntityIdValue(this, other);

    }
    
    @Override
    public int hashCode() {
        return Hash.hashCode(this);
    }

    @Override
    public String toString() {
        if(isNew()) {
            return "new item (reconciled from " + getReconInternalId() +")";
        } else {
            return getIri() + " (reconciled from " + getReconInternalId()+")";
        }
    }
}
