package com.google.refine.clustering;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.refine.clustering.binning.BinningClusterer.BinningClustererConfig;
import com.google.refine.clustering.knn.kNNClusterer.kNNClustererConfig;
import com.google.refine.model.Project;

/**
 * Represents the configuration data for a clusterer.
 * @author Antonin Delpeuch
 *
 */
@JsonTypeInfo(
        use=JsonTypeInfo.Id.NAME,
        include=JsonTypeInfo.As.PROPERTY,
        property="type")
@JsonSubTypes({
    @Type(value = kNNClustererConfig.class, name = "knn"),
    @Type(value = BinningClustererConfig.class, name = "binning") })
public abstract class ClustererConfig  {
    
    protected String columnName;
    
    @JsonProperty("column")
    public String getColumnName() {
        return columnName;
    }
    
    @JsonProperty("column")
    public void setColumnName(String name) {
    	columnName = name;
    }
    
    /**
     * Instantiate the configuration on a particular project.
     * @param project
     * @return
     */
    public abstract Clusterer apply(Project project);
    
    /**
     * Type string used in Json serialization
     */
    @JsonProperty("type")
    public abstract String getType();
}
