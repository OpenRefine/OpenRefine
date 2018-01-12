package org.openrefine.wikidata.schema.entityvalues;

import java.util.List;

import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;

/**
 * An entity id value that also comes with
 * a label and possibly types.
 * 
 * The rationale behind this classes is that OpenRefine
 * already stores labels and types for the Wikidata items
 * it knows about (in the reconciliation data), so it is
 * worth keeping this data to avoid re-fetching it when
 * we need it.
 * 
 * @author antonin
 *
 */
public interface PrefetchedEntityIdValue extends EntityIdValue {
    
    /**
     * This should return the label "as we got it", with no guarantee
     * that it is current or that its language matches that of the user.
     * In general though, that should be the case if the user always uses
     * OpenRefine with the same language settings.
     * 
     * @return the preferred label of the entity
     */
    public String getLabel();
    
    /**
     * Returns a list of types for this item. Again these are the types
     * as they were originally fetched from the reconciliation interface:
     * they can diverge from what is currently on the item.
     * 
     * Empty lists should be returned for
     */
    public List<String> getTypes();
}
