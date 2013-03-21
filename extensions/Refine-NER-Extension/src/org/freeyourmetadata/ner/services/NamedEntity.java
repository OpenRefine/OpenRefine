package org.freeyourmetadata.ner.services;

import java.lang.String;
import java.net.URI;
import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.model.Cell;
import com.google.refine.model.Recon;
import com.google.refine.model.ReconCandidate;
import com.google.refine.model.Recon.Judgment;

/**
 * A named entity with a label and disambiguations
 * @author Ruben Verborgh
 * @author Stefano Parmesan
 */
public class NamedEntity {
    private final static String[] EMPTY_TYPE_SET = new String[0];

    private final String extractedText;
    private final Disambiguation[] disambiguations;

    /**
     * Creates a new named entity without URIs
     * @param extractedText The label of the entity
     */
    public NamedEntity(final String extractedText) {
        this(extractedText, new Disambiguation[] {new Disambiguation(extractedText)});
    }

    /**
     * Creates a new named entity with a single URI
     * @param extractedText The label of the entity
     * @param uri The URI of the entity
     */
    public NamedEntity(final String extractedText, final URI uri) {
        this(extractedText, new Disambiguation[] { new Disambiguation(extractedText, uri) });
    }

    /**
     * Creates a new named entity
     * @param extractedText The label of the entity
     * @param uris The URIs of the entity
     */
    public NamedEntity(final String extractedText, final URI[] uris) {
        this.extractedText = extractedText;
        this.disambiguations = new Disambiguation[uris.length];
        for (int i = 0; i < uris.length; i++)
            disambiguations[i] = new Disambiguation(extractedText, uris[i]);
    }

    /**
     * Creates a new named entity
     * @param extractedText The label matched in the original text
     * @param disambiguations An array of disambiguations
     */
    public NamedEntity(final String extractedText, final Disambiguation[] disambiguations) {
        this.extractedText = extractedText;
        this.disambiguations = disambiguations;
    }
    
    /**
     * Creates a new named entity
     * @param extractedText The label matched in the original text
     * @param disambiguations A list of disambiguations
     */
    public NamedEntity(final String extractedText, final Collection<Disambiguation> disambiguations) {
        this.extractedText = extractedText;
        this.disambiguations = disambiguations.toArray(new Disambiguation[disambiguations.size()]);
    }

    /**
     * Creates a new named entity from a JSON representation
     * @param json The JSON representation of the named entity
     * @throws JSONException if the JSON is not correctly structured
     */
    public NamedEntity(final JSONObject json) throws JSONException {
        extractedText = json.getString("extractedText");
        
        final JSONArray jsonDisambiguations = json.getJSONArray("disambiguations");
        disambiguations = new Disambiguation[jsonDisambiguations.length()];
        for (int i = 0; i < disambiguations.length; i++) {
            disambiguations[i] = new Disambiguation(jsonDisambiguations.getJSONObject(i));
        }
    }

    /**
     * Gets the entity's extractedText
     * @return The extracted text
     */
    public String getExtractedText() {
        return extractedText;
    }

    /**
     * Gets the entity's disambiguations
     * @return The disambiguations
     */
    public Disambiguation[] getDisambiguations() {
        return disambiguations;
    }

    /**
     * Writes the named entity in a JSON representation
     * @param json The JSON writer
     * @throws JSONException if an error occurs during writing
     */
    public void writeTo(final JSONWriter json) throws JSONException {
        json.object();
        json.key("extractedText"); json.value(getExtractedText());
        json.key("disambiguations");
        json.array();
        for (final Disambiguation disambiguation : getDisambiguations())
            disambiguation.writeTo(json);
        json.endArray();
        json.endObject();
    }

    /**
     * Convert the named entity into a Refine worksheet cell
     * @return The cell
     */
    public Cell toCell() {
        // Try to determine a reconciliation value for the cell
        final Recon recon = new Recon(-1L, "", "");
        recon.judgment = Judgment.Matched;
        recon.judgmentAction = "auto";
        recon.service = "NamedEntity";

        // Add all reconciliation candidates
        for (int i = 0; i < disambiguations.length; i++) {
            final Disambiguation match = disambiguations[i];
            final String uri = match.getUri().toString();
            if (uri.length() > 0) {
                final ReconCandidate candidate = new ReconCandidate(uri, match.getLabel(), EMPTY_TYPE_SET, match.getScore());
                recon.addCandidate(candidate);
                // If this candidate is better than the previous best candidate, make it the match
                if (recon.match == null || match.getScore() > recon.match.score) {
                    recon.match = candidate;
                    recon.matchRank = i;
                }
            }
        }

        // Return the cell, adding a reconciliation value if a match was found
        return new Cell(getExtractedText(), recon.match == null ? null : recon);
    }
}
