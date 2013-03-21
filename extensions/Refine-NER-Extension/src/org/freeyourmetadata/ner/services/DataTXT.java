package org.freeyourmetadata.ner.services;

import static org.freeyourmetadata.util.UriUtil.createUri;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.http.HttpEntity;
import org.freeyourmetadata.util.ParameterList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * dataTXT service connector
 * @author Stefano Parmesan
 */
public class DataTXT extends NERServiceBase {
    private final static URI SERVICEBASEURL = createUri("http://spaziodati.eu/datatxt/v3/");
    private final static URI DOCUMENTATIONURI = createUri("https://spaziodati.3scale.net/getting-started");
    private final static String[] PROPERTYNAMES = { "App ID", "App key", "Language", "Confidence", "Epsilon", "Text Chunks" };

    /**
     * Creates a new dataTXT service connector
     */
    public DataTXT() {
        super(SERVICEBASEURL, PROPERTYNAMES, DOCUMENTATIONURI);
        setProperty("Language", "en");
        setProperty("Confidence", "0.1");
        setProperty("Epsilon", "0.3");
        setProperty("Text Chunks", "");
    }

    /** {@inheritDoc} */
    public boolean isConfigured() {
        return getProperty("App ID").length() > 0
                && getProperty("App key").length() > 0
                && getProperty("Language").length() > 0
                && getProperty("Confidence").length() > 0
                && getProperty("Epsilon").length() > 0;
    }

    /** {@inheritDoc} */
    protected HttpEntity createExtractionRequestBody(final String text) throws UnsupportedEncodingException {
        final ParameterList parameters = new ParameterList();
        parameters.add("service", "tag");
        parameters.add("lang", getProperty("Language"));
        parameters.add("text", text);
        parameters.add("rho", getProperty("Confidence"));
        parameters.add("epsilon", getProperty("Epsilon"));
        parameters.add("long_text", getProperty("Text Chunks"));
        parameters.add("dbpedia", "true");
        parameters.add("include_abstract", "false");
        parameters.add("app_id", getProperty("App ID"));
        parameters.add("app_key", getProperty("App key"));
        return parameters.toEntity();
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    protected NamedEntity[] parseExtractionResponseEntity(final JSONTokener tokener) throws JSONException {
        // Check response status
        final JSONObject response = (JSONObject)tokener.nextValue();
        if (!response.isNull("error"))
            throw new IllegalArgumentException("dataTXT request failed.");
        
        // Find all annotations
        final JSONArray annotations = response.getJSONArray("annotations");
        final NamedEntity[] results = new NamedEntity[annotations.length()];
        for (int i = 0; i < results.length; i++) {
            final JSONObject annotation = annotations.getJSONObject(i);
            final String label = annotation.getString("title");
            final double score = annotation.getDouble("rho");

            final JSONArray references = annotation.getJSONArray("ref");
            final ArrayList<Disambiguation> disambiguations = new ArrayList<Disambiguation>();
            for (int j = 0; j < references.length(); j++) {
                final JSONObject reference = references.getJSONObject(j);
                final Iterator<String> keyIterator = reference.keys();
                while (keyIterator.hasNext()) {
                    final String key = keyIterator.next();
                    // DataTXT returns one match with multiple URIs (from DBpedia and Wikipedia).
                    // We only keep the Wikipedia link for now.
                    if (key.equals("dbpedia")) {
                        disambiguations.add(new Disambiguation(label, createUri(reference.getString(key)), score));
                    }
                }
            }
            results[i] = new NamedEntity(annotation.getString("spot"), disambiguations);
        }
        
        return results;
    }
}
