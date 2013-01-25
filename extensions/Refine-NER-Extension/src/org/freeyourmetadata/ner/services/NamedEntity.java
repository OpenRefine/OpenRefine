package org.freeyourmetadata.ner.services;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.model.Cell;
import com.google.refine.model.Recon;
import com.google.refine.model.ReconCandidate;
import com.google.refine.model.Recon.Judgment;

/**
 * A named entity with a label and URIs
 * @author Ruben Verborgh
 */
public class NamedEntity {
    private final static URI[] EMPTY_URI_SET = new URI[0];
    private final static String[] EMPTY_TYPE_SET = new String[0];
    
    private final String label;
    private final URI[] uris;
    
    /**
     * Creates a new named entity without URIs
     * @param label The label of the entity
     */
    public NamedEntity(final String label) {
        this(label, EMPTY_URI_SET);
    }
    
    /**
     * Creates a new named entity with a single URI
     * @param label The label of the entity
     * @param uri The URI of the entity
     */
    public NamedEntity(final String label, final URI uri) {
        this(label, new URI[]{ uri });
    }
    
    /**
     * Creates a new named entity
     * @param label The label of the entity
     * @param uris The URIs of the entity
     */
    public NamedEntity(final String label, final URI[] uris) {
        this.label = label;
        this.uris = uris;
    }
    
    /**
     * Creates a new named entity from a JSON representation
     * @param json The JSON representation of the named entity
     * @throws JSONException if the JSON is not correctly structured
     */
    public NamedEntity(final JSONObject json) throws JSONException {
        this.label = json.getString("label");
        final JSONArray urisJson = json.getJSONArray("uris");
        this.uris = new URI[urisJson.length()];
        for (int i = 0; i < uris.length; i++) {
            try { uris[i] = new URI(urisJson.getString(i)); }
            catch (URISyntaxException e) {}
        }
    }

    /**
     * Gets the entity's label
     * @return The label
     */
    public String getLabel() {
        return label;
    }
    
    /**
     * Gets the entity's URIs
     * @return The URIs
     */
    public URI[] getUris() {
        return uris;
    }
    
    /**
     * Writes the named entity in a JSON representation
     * @param json The JSON writer
     * @throws JSONException if an error occurs during writing
     */
    public void writeTo(final JSONWriter json) throws JSONException {
        json.object();
        json.key("label"); json.value(getLabel());
        json.key("uris");
        json.array();
        for (final URI uri : getUris())
            json.value(uri.toString());
        json.endArray();
        json.endObject();
    }
    
    /**
     * Convert the named entity into a Refine worksheet cell
     * @return The cell
     */
    public Cell toCell() {
        // Find all non-empty URIs
        final ArrayList<String> nonEmptyUris = new ArrayList<String>(getUris().length);
        for (final URI uri : getUris()) {
            final String uriString = uri == null ? "" : uri.toString();
            if (uriString.length() > 0)
                nonEmptyUris.add(uriString);
        }
        
        // Don't include a reconciliation element if there are no URIs
        final Recon recon;
        if (nonEmptyUris.size() == 0) {
            recon = null;
        }
        // Include a reconciliation candidate for each URI
        else {
            recon = new Recon(-1L, "", "");
            // Create the candidates
            for (final String uri : nonEmptyUris)
                recon.addCandidate(new ReconCandidate(uri, getLabel(), EMPTY_TYPE_SET, 1.0));
            // Pick the first one as the best match
            recon.match = recon.candidates.get(0);
            recon.matchRank = 0;
            recon.judgment = Judgment.Matched;
            recon.judgmentAction = "auto";
            // Act as a reconciliation service
            recon.service = "NamedEntity";
        }
        return new Cell(getLabel(), recon);
    }
}
