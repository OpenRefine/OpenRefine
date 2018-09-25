package com.google.refine.sorting;

import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.refine.Jsonizable;

/**
 * Stores the configuration of a row/record sorting setup.
 * @author Antonin Delpeuch
 *
 */
public final class SortingConfig implements Jsonizable {
    
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

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        writer.key("criteria");
        writer.array();
        for (int i = 0; i != _criteria.length; i++) {
            _criteria[i].write(writer, options);
        }
        writer.endArray();
        writer.endObject();
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