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
package org.openrefine.sorting;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.openrefine.util.ParsingUtilities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Stores the configuration of a row/record sorting setup.
 * @author Antonin Delpeuch
 *
 */
public final class SortingConfig  {
	
	public static final SortingConfig NO_SORTING = new SortingConfig(Collections.emptyList());
    
    protected final List<Criterion> _criteria;
    
    @JsonCreator
    public SortingConfig(
            @JsonProperty("criteria")
            List<Criterion> criteria) {
        _criteria = criteria;
    }
    
    @JsonProperty("criteria")
    public List<Criterion> getCriteria() {
        return _criteria;
    }
    
    public static SortingConfig reconstruct(String obj) throws IOException {
        return ParsingUtilities.mapper.readValue(obj, SortingConfig.class);
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SortingConfig)) {
            return false;
        }
        SortingConfig otherConfig = (SortingConfig)other;
        return _criteria.equals(otherConfig.getCriteria());
    }
    
    @Override
    public int hashCode() {
        return _criteria.hashCode();
    }
}
