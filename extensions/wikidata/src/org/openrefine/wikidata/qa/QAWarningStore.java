package org.openrefine.wikidata.qa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A store for QA warnings which aggregates them by type.
 * @author antonin
 */
public class QAWarningStore {
    
    private Map<String, QAWarning> map;
    private QAWarning.Severity maxSeverity;
    private int totalWarnings;
    
    public QAWarningStore() {
        this.map = new HashMap<>();
        this.maxSeverity = QAWarning.Severity.INFO;
    }
    
    /**
     * Stores a warning, aggregating it with any existing 
     * @param warning
     */
    public void addWarning(QAWarning warning) {
        String aggregationKey = warning.getAggregationId();
        QAWarning.Severity severity = warning.getSeverity();
        if (severity.compareTo(maxSeverity) > 0) {
            maxSeverity = severity;
        }
        totalWarnings += warning.getCount();
        if (map.containsKey(aggregationKey)) {
            QAWarning existing = map.get(aggregationKey);
            existing.aggregate(warning);
        } else {
            map.put(aggregationKey, warning);
        }
    }
    
    /**
     * Returns the list of aggregated warnings, ordered by decreasing severity
     */
    @JsonProperty("warnings")
    public List<QAWarning> getWarnings() {
        List<QAWarning> result = new ArrayList<>(map.values());
        Collections.sort(result);
        return result;
    }
    
    /**
     * Returns the maximum severity of the stored warnings (INFO if empty)
     */
    @JsonProperty("max_severity")
    public QAWarning.Severity getMaxSeverity() {
        return maxSeverity;
    }
    
    /**
     * Returns the total number of warnings
     */
    @JsonProperty("nb_warnings")
    public int getNbWarnings() {
        return totalWarnings;
    }
}
