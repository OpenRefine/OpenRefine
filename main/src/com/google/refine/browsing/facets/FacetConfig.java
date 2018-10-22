package com.google.refine.browsing.facets;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.google.refine.model.Project;


/**
 * Represents the configuration of a facet, as stored
 * in the engine configuration and in the JSON serialization
 * of operations. It does not contain the actual values displayed by
 * the facet.
 * 
 * @author Antonin Delpeuch
 *
 */
@JsonTypeInfo(
        use=JsonTypeInfo.Id.NAME,
        include=JsonTypeInfo.As.PROPERTY,
        property="type")
@JsonSubTypes({
    @Type(value = ListFacet.ListFacetConfig.class, name = "list"),
    @Type(value = RangeFacet.RangeFacetConfig.class, name = "range"),
    @Type(value = TimeRangeFacet.TimeRangeFacetConfig.class, name = "timerange"),
    @Type(value = TextSearchFacet.TextSearchFacetConfig.class, name = "text"),
    @Type(value = ScatterplotFacet.ScatterplotFacetConfig.class, name = "scatterplot") })
public interface FacetConfig  {   
    /**
     * Instantiates the given facet on a particular project.
     * @param project
     * @return a computed facet on the given project.
     */
    public Facet apply(Project project);
    
    /**
     * The facet type as stored in json.
     */
    @JsonProperty("type")
    public String getJsonType();
}
