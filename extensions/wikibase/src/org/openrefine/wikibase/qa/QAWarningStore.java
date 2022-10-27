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

package org.openrefine.wikibase.qa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A store for QA warnings which aggregates them by type.
 * 
 * @author Antonin Delpeuch
 */
public class QAWarningStore {

    @JsonIgnore
    private Map<String, QAWarning> map;
    @JsonIgnore
    private QAWarning.Severity maxSeverity;
    @JsonIgnore
    private int totalWarnings;

    public QAWarningStore() {
        this.map = new HashMap<>();
        this.maxSeverity = QAWarning.Severity.INFO;
    }

    /**
     * Stores a warning, aggregating it with any existing
     * 
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
            map.put(aggregationKey, existing.aggregate(warning));
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
