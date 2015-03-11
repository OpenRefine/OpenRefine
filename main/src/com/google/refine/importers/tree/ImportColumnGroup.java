package com.google.refine.importers.tree;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

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
                name,nextRowIndex,StringUtils.join(columns.keySet(), ','),
                StringUtils.join(subgroups.keySet(),','));
    }
}