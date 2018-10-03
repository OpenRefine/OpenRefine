package com.google.refine.sorting;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


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
    
    public static SortingConfig reconstruct(JSONObject obj) {
        Criterion[] criteria;
        if (obj != null && obj.has("criteria") && !obj.isNull("criteria")) {
            JSONArray a = obj.getJSONArray("criteria");
            int count = a.length();

            criteria = new Criterion[count];

            for (int i = 0; i < count; i++) {
                JSONObject obj2 = a.getJSONObject(i);

                criteria[i] = Criterion.reconstruct(obj2);
            }
        } else {
            criteria = new Criterion[0];
        }
        return new SortingConfig(criteria);
    }
}