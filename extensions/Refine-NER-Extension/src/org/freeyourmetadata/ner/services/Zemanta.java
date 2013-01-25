package org.freeyourmetadata.ner.services;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Zemanta service connector
 * @author Ruben Verborgh
 */
public class Zemanta extends NERServiceBase {
    private final static URI SERVICEBASEURL = createUri("http://papi.zemanta.com/services/rest/0.0/");
    private final static URI DOCUMENTATIONURI = createUri("http://freeyourmetadata.org/named-entity-extraction/zemanta/");
    private final static String[] PROPERTYNAMES = { "API key" };
    
    /**
     * Creates a new Zemanta service connector
     */
    public Zemanta() {
        super(SERVICEBASEURL, PROPERTYNAMES, DOCUMENTATIONURI);
    }
    
    /** {@inheritDoc} */
    public boolean isConfigured() {
        return getProperty("API key").length() > 0;
    }
    
    /** {@inheritDoc} */
    protected HttpEntity createExtractionRequestBody(final String text) throws UnsupportedEncodingException {
        final ArrayList<NameValuePair> parameters = new ArrayList<NameValuePair>(5);
        parameters.add(new BasicNameValuePair("method", "zemanta.suggest_markup"));
        parameters.add(new BasicNameValuePair("format", "json"));
        parameters.add(new BasicNameValuePair("return_rdf_links", "1"));
        parameters.add(new BasicNameValuePair("api_key", getProperty("API key")));
        parameters.add(new BasicNameValuePair("text", text));
        return new UrlEncodedFormEntity(parameters);
    }
    
    /** {@inheritDoc} */
    @Override
    protected NamedEntity[] parseExtractionResponseEntity(final JSONTokener tokener) throws JSONException {
        // Check response status
        final JSONObject response = (JSONObject)tokener.nextValue();
        if (!"ok".equals(response.getString("status")))
            throw new IllegalArgumentException("The Zemanta request did not succeed.");
        
        // Get mark-up results
        final JSONObject markup = response.getJSONObject("markup");
        final ArrayList<NamedEntity> results = new ArrayList<NamedEntity>();
        // In the mark-up results, find the links
        final JSONArray links = markup.getJSONArray("links");
        for (int i = 0; i < links.length(); i++) {
            // In each link, find the targets
            final JSONObject link = links.getJSONObject(i);
            final JSONArray targets = link.getJSONArray("target");
            final String label = targets.getJSONObject(0).getString("title");
            final URI[] urls = new URI[targets.length()];
            // Find all target URLs
            for (int j = 0; j < targets.length(); j++)
                urls[j] = createUri(targets.getJSONObject(j).getString("url"));
            results.add(new NamedEntity(label, urls));
        }
        return results.toArray(new NamedEntity[results.size()]);
    }
}
