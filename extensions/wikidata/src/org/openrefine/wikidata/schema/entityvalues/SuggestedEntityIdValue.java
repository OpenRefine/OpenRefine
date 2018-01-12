package org.openrefine.wikidata.schema.entityvalues;

import java.util.ArrayList;
import java.util.List;

import org.wikidata.wdtk.datamodel.helpers.Hash;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ValueVisitor;

/**
 * An EntityIdValue that we have obtained from a suggest widget
 * in the schema alignment dialog.
 * 
 * @author antonin
 *
 */
public abstract class SuggestedEntityIdValue implements PrefetchedEntityIdValue {
    
    private String _id;
    private String _siteIRI;
    private String _label;

    public SuggestedEntityIdValue(String id, String siteIRI, String label) {
        _id = id;
        _siteIRI = siteIRI;
        _label = label;
    }

    @Override
    public String getId() {
        return _id;
    }
    
    @Override
    public String getSiteIri() {
        return _siteIRI;
    }
    
    @Override
    public String getLabel() {
        return _label;
    }
    
    @Override
    public List<String> getTypes() {
        return new ArrayList<>();
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
