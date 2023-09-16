/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package org.openrefine.browsing.facets;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

import org.openrefine.model.ColumnModel;
import org.openrefine.overlay.OverlayModel;

/**
 * Represents the configuration of a facet, as stored in the engine configuration and in the JSON serialization of
 * operations. It does not contain the actual values displayed by the facet.
 * 
 * @author Antonin Delpeuch
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonTypeIdResolver(FacetConfigResolver.class)
public interface FacetConfig {

    /**
     * Instantiates the given facet on a particular column model. This allows to check the validity of the configuration
     * against a particular table schema (checking that the dependent columns exist, for instance).
     * 
     * @param columnModel
     *            the header of the table the facet is applied to.
     * @param overlayModels
     *            the overlay models of the table the facet is applied to (can be accessed by expressions evaluated by
     *            the facet)
     * @return a computed facet on the given project.
     */
    public Facet apply(ColumnModel columnModel, Map<String, OverlayModel> overlayModels);

    /**
     * Computes the set of columns the facet depends on. If the facet relies on an unknown set of columns, or if it is
     * not row-wise, this returns null.
     * 
     * @return the set of column names the facet depends on.
     */
    @JsonIgnore
    public Set<String> getColumnDependencies();

    /**
     * Updates the facet config after a renaming of columns.
     * 
     * @return null if the update could not be performed, or the new facet config if the update could be performed.
     */
    public FacetConfig renameColumnDependencies(Map<String, String> substitutions);

    /**
     * A neutral facet is a facet that does not filter out any row, i.e. it is in its initial, reset state. Neutral
     * facets are still useful to visualize the distribution of values in a column.
     */
    @JsonIgnore
    public boolean isNeutral();

    /**
     * The facet type as stored in json.
     */
    @JsonIgnore // already included by @JsonTypeInfo
    public String getJsonType();
}
