package org.openrefine.wikidata.schema.entityvalues;

import org.wikidata.wdtk.datamodel.helpers.Hash;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ValueVisitor;

/**
 * An EntityIdValue that holds not just the id but also
 * the label as fetched by either the reconciliation interface
 * or the suggester.
 * 
 * This label will be localized depending on the language chosen
 * by the user for OpenRefine's interface. Storing it lets us
 * reuse it later on without having to re-fetch it.
 * @author antonin
 *
 */
public abstract class TermedEntityIdValue implements EntityIdValue {
    
    private String _id;
    private String _siteIRI;
    private String _label;
    
    public TermedEntityIdValue(String id, String siteIRI, String label) {
        _id = id;
        _siteIRI = siteIRI;
        _label = label;
    }

    @Override
    public abstract String getEntityType();

    @Override
    public String getId() {
        return _id;
    }

    @Override
    public String getSiteIri() {
        return _siteIRI;
    }
    
    public String getLabel() {
        return _label;
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
        final EntityIdValue otherNew = (EntityIdValue)other;
        return getIri().equals(otherNew.getIri());
    }
    
    @Override
    public int hashCode() {
        return Hash.hashCode(this);
    }
}
