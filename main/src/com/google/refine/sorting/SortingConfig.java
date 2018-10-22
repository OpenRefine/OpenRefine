package com.google.refine.sorting;

import java.io.IOException;

import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.refine.util.ParsingUtilities;


/**
 * Stores the configuration of a row/record sorting setup.
 * @author Antonin Delpeuch
 *
 */
public final class SortingConfig  {
    
    protected Criterion[] _criteria;
    
    @JsonCreator
    public SortingConfig(
            @JsonProperty("criteria")
            Criterion[] criteria) {
        _criteria = criteria;
    }
    
    @JsonProperty("criteria")
    public Criterion[] getCriteria() {
        return _criteria;
    }
    
    public static SortingConfig reconstruct(JSONObject obj) throws IOException {
        return ParsingUtilities.mapper.readValue(obj.toString(), SortingConfig.class);
    }
}