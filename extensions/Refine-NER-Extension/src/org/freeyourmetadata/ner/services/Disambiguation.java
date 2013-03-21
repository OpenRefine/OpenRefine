package org.freeyourmetadata.ner.services;

import java.net.URI;

import static org.freeyourmetadata.util.UriUtil.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;


/**
 * A disambiguation of a named entity
 * @author Stefano Parmesan
 * @author Ruben Verborgh
 */
public class Disambiguation {
    private final String label;
    private final URI uri;
    private final double score;

    /**
     * Creates a new disambiguation with an empty URI
     * @param label The label of the entity
     */
    public Disambiguation(final String label) {
        this(label, EMPTYURI, 1.0);
    }

    /**
     * Creates a new disambiguation
     * @param label The label of the entity
     * @param uri The URI of the entity
     */
    public Disambiguation(final String label, final URI uri) {
        this(label, uri, 1.0);
    }

    /**
     * Creates a new disambiguation
     * @param label The label of the entity
     * @param uri The URI of the entity
     * @param score The disambiguation's score
     */
    public Disambiguation(final String label, final URI uri, final double score) {
        this.label = label;
        this.uri = uri;
        this.score = score;
    }

    /**
     * Creates a new disambiguation from a JSON representation
     * @param json The JSON representation of the disambiguation
     * @throws JSONException if the JSON is not correctly structured
     */
    public Disambiguation(final JSONObject json) throws JSONException {
        this.label = json.getString("label");
        this.uri = createUri(json.getString("uri"));
        this.score = json.getDouble("score");
    }

    /**
     * Gets the disambiguation's label
     * @return The label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Gets the disambiguation's URI
     * @return The URI
     */
    public URI getUri() {
        return uri;
    }

    /**
     * Gets the disambiguation's score
     * @return The score
     */
    public double getScore() {
        return score;
    }

    /**
     * Writes the disambiguation in a JSON representation
     * @param json The JSON writer
     * @throws JSONException if an error occurs during writing
     */
    public void writeTo(final JSONWriter json) throws JSONException {
        json.object();
        json.key("label"); json.value(getLabel());
        json.key("uri"); json.value(getUri().toString());
        json.key("score"); json.value(getScore());
        json.endObject();
    }
}
