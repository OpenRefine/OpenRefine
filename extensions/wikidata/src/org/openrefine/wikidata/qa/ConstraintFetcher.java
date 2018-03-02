
package org.openrefine.wikidata.qa;

import java.util.Set;

import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

/**
 * An object that fetches constraints about properties.
 * 
 * @author Antonin Delpeuch
 *
 */
public interface ConstraintFetcher {

    /**
     * Retrieves the regular expression for formatting a property, or null if
     * there is no such constraint
     * @param pid
     * @return the expression of a regular expression which should be compatible with java.util.regex
     */
    String getFormatRegex(PropertyIdValue pid);

    /**
     * Retrieves the property that is the inverse of a given property
     * @param pid: the property to retrieve the inverse for
     * @return the pid of the inverse property
     */
    PropertyIdValue getInversePid(PropertyIdValue pid);

    /**
     * Is this property for values only?
     */
    boolean isForValuesOnly(PropertyIdValue pid);

    /**
     * Is this property for qualifiers only?
     */
    boolean isForQualifiersOnly(PropertyIdValue pid);

    /**
     * Is this property for references only?
     */
    boolean isForReferencesOnly(PropertyIdValue pid);

    /**
     * Get the list of allowed qualifiers (as property ids) for this property (null if any)
     */
    Set<PropertyIdValue> allowedQualifiers(PropertyIdValue pid);

    /**
     * Get the list of mandatory qualifiers (as property ids) for this property (null if any)
     */
    Set<PropertyIdValue> mandatoryQualifiers(PropertyIdValue pid);

    /**
     * Is this property expected to have at most one value per item?
     */
    boolean hasSingleValue(PropertyIdValue pid);

    /**
     * Is this property expected to have distinct values?
     */
    boolean hasDistinctValues(PropertyIdValue pid);

}
