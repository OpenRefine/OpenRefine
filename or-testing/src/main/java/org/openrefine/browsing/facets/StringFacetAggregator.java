
package org.openrefine.browsing.facets;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.openrefine.browsing.filters.AnyRowRecordFilter;
import org.openrefine.model.RecordFilter;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;

public class StringFacetAggregator implements FacetAggregator<StringFacetState> {

    private static final long serialVersionUID = 3209825226374715166L;
    private int columnIdx;
    private String selected;

    public StringFacetAggregator(int columnIdx, String selected) {
        this.columnIdx = columnIdx;
        this.selected = selected;
    }

    @Override
    public StringFacetState sum(StringFacetState first, StringFacetState second) {
        Map<String, Long> counts = new HashMap<>(second.occurences);
        for (Entry<String, Long> entry : first.occurences.entrySet()) {
            counts.put(entry.getKey(), counts.getOrDefault(entry.getKey(), 0L) + entry.getValue());
        }
        return new StringFacetState(counts);
    }

    @Override
    public StringFacetState withRow(StringFacetState state, long rowId, Row row) {
        Object cellValue = row.getCellValue(columnIdx);
        String stringValue = cellValue == null ? "null" : cellValue.toString();
        Map<String, Long> counts = new HashMap<>(state.occurences);
        counts.put(stringValue, counts.getOrDefault(stringValue, 0L) + 1);
        return new StringFacetState(counts);
    }

    @Override
    public RowFilter getRowFilter() {
        return new RowFilter() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean filterRow(long rowIndex, Row row) {
                if (selected == null) {
                    return true;
                }
                Object cellValue = row.getCellValue(columnIdx);
                String stringValue = cellValue == null ? "null" : cellValue.toString();
                return stringValue.equals(selected);
            }

        };
    }

    @Override
    public RecordFilter getRecordFilter() {
        return new AnyRowRecordFilter(getRowFilter());
    }

}
