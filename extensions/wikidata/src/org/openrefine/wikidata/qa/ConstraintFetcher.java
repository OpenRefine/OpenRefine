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

import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

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
     * Is this property expected to have at most one value per item?
     */
    boolean hasSingleValue(PropertyIdValue pid);

    /**
     * Is this property expected to have distinct values?
     */
    boolean hasDistinctValues(PropertyIdValue pid);

}
