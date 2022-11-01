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

package org.openrefine.wikibase.schema.entityvalues;

import java.util.List;

import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An entity id value that also comes with a label and possibly types.
 * 
 * The rationale behind this classes is that OpenRefine already stores labels and types for the Wikidata entities it
 * knows about (in the reconciliation data), so it is worth keeping this data to avoid re-fetching it when we need it.
 * 
 * @author Antonin Delpeuch
 *
 */
public interface PrefetchedEntityIdValue extends EntityIdValue {

    /**
     * This should return the label "as we got it", with no guarantee that it is current or that its language matches
     * that of the user. In general though, that should be the case if the user always uses OpenRefine with the same
     * language settings.
     * 
     * @return the preferred label of the entity
     */
    @JsonProperty("label")
    public String getLabel();

    /**
     * Returns a list of types for this entity. Again these are the types as they were originally fetched from the
     * reconciliation interface: they can diverge from what is currently on the entity.
     * 
     * Empty lists should be returned for
     */
    @JsonProperty("types")
    public List<String> getTypes();
}
