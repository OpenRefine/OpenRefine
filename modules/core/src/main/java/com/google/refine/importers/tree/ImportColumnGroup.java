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

package com.google.refine.importers.tree;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * A column group describes a branch in tree structured data
 */
public class ImportColumnGroup extends ImportVertical {

    public Map<String, ImportColumnGroup> subgroups = new LinkedHashMap<String, ImportColumnGroup>();
    public Map<String, ImportColumn> columns = new LinkedHashMap<String, ImportColumn>();
    public int nextRowIndex; // TODO: this can be hoisted into superclass

    @Override
    void tabulate() {
        for (ImportColumn c : columns.values()) {
            c.tabulate();
            nonBlankCount = Math.max(nonBlankCount, c.nonBlankCount);
        }
        for (ImportColumnGroup g : subgroups.values()) {
            g.tabulate();
            nonBlankCount = Math.max(nonBlankCount, g.nonBlankCount);
        }
    }

    @Override
    public String toString() {
        return String.format("name=%s, nextRowIndex=%d, columns={%s}, subgroups={{%s}}",
                name, nextRowIndex, StringUtils.join(columns.keySet(), ','),
                StringUtils.join(subgroups.keySet(), ','));
    }
}
