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
package org.openrefine.wikidata.qa;

import java.util.Set;

import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;

/**
 * An object that fetches constraints about properties.
 * 
 * @author Antonin Delpeuch
 *
 */
public interface ConstraintFetcher {

    /**
     * Retrieves the regular expression for formatting a property, or null if there
     * is no such constraint
     * 
     * @param pid
     * @return the expression of a regular expression which should be compatible
     *         with java.util.regex
     */
    String getFormatRegex(PropertyIdValue pid);

    /**
     * Retrieves the property that is the inverse of a given property
     * 
     * @param pid:
     *            the property to retrieve the inverse for
     * @return the pid of the inverse property
     */
    PropertyIdValue getInversePid(PropertyIdValue pid);
    
    /**
     * Is this property supposed to be symmetric (its own inverse)?
     */
    boolean isSymmetric(PropertyIdValue pid);

    /**
     * Can this property be used as values?
     */
    boolean allowedAsValue(PropertyIdValue pid);

    /**
     * Can this property be used as qualifiers?
     */
    boolean allowedAsQualifier(PropertyIdValue pid);

    /**
     * Can this property be used in a reference?
     */
    boolean allowedAsReference(PropertyIdValue pid);

    /**
     * Get the list of allowed qualifiers (as property ids) for this property (null
     * if any)
     */
    Set<PropertyIdValue> allowedQualifiers(PropertyIdValue pid);

    /**
     * Get the list of mandatory qualifiers (as property ids) for this property
     * (null if any)
     */
    Set<PropertyIdValue> mandatoryQualifiers(PropertyIdValue pid);
    
    /**
     * Get the set of allowed values for this property (null if no such constraint).
     * This set may contain null if one of the allowed values in novalue or somevalue.
     */
    Set<Value> allowedValues(PropertyIdValue pid);
    
    /**
     * Get the set of disallowed values for this property (null if no such constraint).
     * This set may contain null if one of the allowed values in novalue or somevalue.
     */
    Set<Value> disallowedValues(PropertyIdValue pid);

    /**
     * Is this property expected to have at most one value per item?
     */
    boolean hasSingleValue(PropertyIdValue pid);
    
    /**
     * Is this property expected to have a single best value only?
     */
    boolean hasSingleBestValue(PropertyIdValue pid);

    /**
     * Is this property expected to have distinct values?
     */
    boolean hasDistinctValues(PropertyIdValue pid);

    /**
     * Can statements using this property have uncertainty bounds?
     */
    boolean boundsAllowed(PropertyIdValue pid);

    /**
     * Is this property expected to have integer values only?
     */
    boolean integerValued(PropertyIdValue pid);
    
    /**
     * Returns the allowed units for this property. If empty, no unit is allowed. If null, any unit is allowed.
     */
    Set<ItemIdValue> allowedUnits(PropertyIdValue pid);
    
    /**
     * Can this property be used on items?
     */
    boolean usableOnItems(PropertyIdValue pid);
}
