package org.openrefine.wikidata.qa;

import java.util.Properties;

import org.openrefine.wikidata.utils.JacksonJsonizable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A class to represent a QA warning emited by the Wikidata schema
 * This could probably be reused at a broader scale, for instance for
 * Data Package validation.
 * 
 * @author antonin
 *
 */
public class QAWarning extends JacksonJsonizable implements Comparable<QAWarning> {
    
    public enum Severity {
        INFO, // We just report something to the user but it is probably fine
        WARNING, // Edits that look wrong but in some cases they are actually fine
        IMPORTANT, // There is almost surely something wrong about the edit but in rare cases we might want to allow it
        CRITICAL, // We should never edit if there is a critical issue
    }
    
    /// The type of QA warning emitted
    private String type;
    // The key for aggregation of other QA warnings together - this specializes the id
    private String bucketId;
    // The severity of the issue
    private Severity severity;
    // The number of times this issue was found
    private int count;
    // Other details about the warning, that can be displayed to the user
    private Properties properties;

    @JsonCreator
    public QAWarning(
            @JsonProperty("type") String type,
            @JsonProperty("bucket_id") String bucketId,
            @JsonProperty("severity") Severity severity,
            @JsonProperty("count") int count,
            @JsonProperty("properties") Properties properties) {
        this.type = type;
        this.bucketId = bucketId;
        this.severity = severity;
        this.count = count;
        this.properties = properties;
    }
    
    /**
     * Returns the full key for aggregation of QA warnings
     * @return
     */
    public String getAggregationId() {
        if (this.bucketId != null) {
            return this.type + "_" + this.bucketId;
        } else {
            return this.type;
        }
    }
    
    /**
     * Aggregates another QA warning of the same aggregation id.
     * @param other
     */
    public void aggregate(QAWarning other) {
        assert other.getAggregationId() == getAggregationId();
        this.count += other.getCount();
        if(this.severity.compareTo(other.getSeverity()) < 0) {
            this.severity = other.getSeverity();
        }
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
    public Properties getProperties() {
        return properties;
    }

    /**
     * Warnings are sorted by decreasing severity.
     */
    @Override
    public int compareTo(QAWarning other) {
        return - severity.compareTo(other.getSeverity());
    }
}
