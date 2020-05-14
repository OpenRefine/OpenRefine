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
package org.openrefine.wikidata.qa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jsoup.helper.Validate;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A class to represent a QA warning emitted by the Wikidata schema This could
 * probably be reused at a broader scale, for instance for Data Package
 * validation.
 * 
 * @author Antonin Delpeuch
 *
 */
public class QAWarning implements Comparable<QAWarning> {

    public enum Severity {
        INFO, // We just report something to the user but it is probably fine
        WARNING, // Edits that look wrong but in some cases they are actually fine
        IMPORTANT, // There is almost surely something wrong about the edit but in rare cases we
                   // might want to allow it
        CRITICAL, // We should never edit if there is a critical issue
    }

    /// The type of QA warning emitted
    private final String type;
    // The key for aggregation of other QA warnings together - this specializes the
    // id
    private final String bucketId;
    // The severity of the issue
    private final Severity severity;
    // The number of times this issue was found
    private final int count;
    // Other details about the warning, that can be displayed to the user
    private final Map<String, Object> properties;

    public QAWarning(String type, String bucketId, Severity severity, int count) {
        Validate.notNull(type);
        this.type = type;
        this.bucketId = bucketId;
        Validate.notNull(severity);
        this.severity = severity;
        this.count = count;
        this.properties = new HashMap<>();
    }

    /**
     * @return the full key for aggregation of QA warnings
     */
    @JsonIgnore
    public String getAggregationId() {
        if (this.bucketId != null) {
            return this.type + "_" + this.bucketId;
        } else {
            return this.type;
        }
    }

    /**
     * Aggregates another QA warning of the same aggregation id.
     * 
     * @param other
     */
    public QAWarning aggregate(QAWarning other) {
        assert other.getAggregationId().equals(getAggregationId());
        int newCount = count + other.getCount();
        Severity newSeverity = severity;
        if (other.getSeverity().compareTo(severity) > 0) {
            newSeverity = other.getSeverity();
        }
        QAWarning merged = new QAWarning(getType(), getBucketId(), newSeverity, newCount);
        for (Entry<String, Object> entry : properties.entrySet()) {
            merged.setProperty(entry.getKey(), entry.getValue());
        }
        for (Entry<String, Object> entry : other.getProperties().entrySet()) {
            merged.setProperty(entry.getKey(), entry.getValue());
        }
        return merged;
    }

    /**
     * Sets a property of the QA warning, to be used by the front-end for display.
     * 
     * @param key:
     *            the name of the property
     * @param value
     *            should be Jackson-serializable
     */
    public void setProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("bucketId")
    public String getBucketId() {
        return bucketId;
    }

    @JsonProperty("severity")
    public Severity getSeverity() {
        return severity;
    }

    @JsonProperty("count")
    public int getCount() {
        return count;
    }

    @JsonProperty("properties")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Warnings are sorted by decreasing severity.
     */
    @Override
    public int compareTo(QAWarning other) {
        return -severity.compareTo(other.getSeverity());
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof QAWarning)) {
            return false;
        }
        QAWarning otherWarning = (QAWarning) other;
        return type.equals(otherWarning.getType()) && bucketId.equals(otherWarning.getBucketId())
                && severity.equals(otherWarning.getSeverity()) && count == otherWarning.getCount()
                && properties.equals(otherWarning.getProperties());
    }
}
