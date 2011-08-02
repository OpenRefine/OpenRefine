package com.google.refine.importers.tree;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * A column group describes a branch in tree structured data
 */
public class ImportColumnGroup extends ImportVertical {
    public Map<String, ImportColumnGroup> subgroups = new HashMap<String, ImportColumnGroup>();
    public Map<String, ImportColumn> columns = new HashMap<String, ImportColumn>();
    public int nextRowIndex;

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
    
    public String toString() {
        return String.format("name=%s, columns={%s}, subgroups={{%s}}",
                name,StringUtils.join(columns.keySet(), ','),
                StringUtils.join(subgroups.keySet(),','));
    }
}