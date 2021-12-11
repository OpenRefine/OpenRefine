
package org.openrefine.browsing.facets;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.RowInRecordFilter;

public class StringFacetAggregator extends FacetAggregator<StringFacetState> {

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
    public StringFacetState withRow(StringFacetState state, long rowId, Row row, Record record) {
        Object cellValue = row.getCellValue(columnIdx);
        String stringValue = cellValue == null ? "null" : cellValue.toString();
        Map<String, Long> counts = new HashMap<>(state.occurences);
        counts.put(stringValue, counts.getOrDefault(stringValue, 0L) + 1);
        return new StringFacetState(counts);
    }

    @Override
    public RowInRecordFilter getRowFilter() {
        return new RowInRecordFilter(true) {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean filterRow(long rowIndex, Row row, Record record) {
                if (selected == null) {
                    return true;
                }
                Object cellValue = row.getCellValue(columnIdx);
                String stringValue = cellValue == null ? "null" : cellValue.toString();
                return stringValue.equals(selected);
            }

        };
    }

}
