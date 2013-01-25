package org.freeyourmetadata.ner.services;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Alchemy service connector
 * @author Ruben Verborgh
 */
public class AlchemyAPI extends NERServiceBase {
    private final static URI SERVICEBASEURL = createUri("http://access.alchemyapi.com/calls/text/TextGetRankedNamedEntities?outputMode=json");
    private final static URI DOCUMENTATIONURI = createUri("http://freeyourmetadata.org/named-entity-extraction/alchemyapi/");
    private final static String[] PROPERTYNAMES = { "API key" };
    private final static HashSet<String> NONURIFIELDS = new HashSet<String>(
            Arrays.asList(new String[]{ "subType", "name", "website" }));
    
    /**
     * Creates a new Alchemy service connector
     */
    public AlchemyAPI() {
        super(SERVICEBASEURL, PROPERTYNAMES, DOCUMENTATIONURI);
    }
    
    /** {@inheritDoc} */
    public boolean isConfigured() {
        return getProperty("API key").length() > 0;
    }
    
    /** {@inheritDoc} */
    protected HttpEntity createExtractionRequestBody(final String text) throws UnsupportedEncodingException {
        final ArrayList<NameValuePair> parameters = new ArrayList<NameValuePair>(2);
        parameters.add(new BasicNameValuePair("apikey", getProperty("API key")));
        parameters.add(new BasicNameValuePair("text", text));
        return new UrlEncodedFormEntity(parameters);
    }
    
    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    protected NamedEntity[] parseExtractionResponseEntity(final JSONTokener tokener) throws JSONException {
        // Check response status
        final JSONObject response = (JSONObject)tokener.nextValue();
        if (!"OK".equals(response.getString("status")))
            throw new IllegalArgumentException("The AlchemyAPI request did not succeed.");
        // Find all entities
        final JSONArray entities = response.getJSONArray("entities");
        final NamedEntity[] results = new NamedEntity[entities.length()];
        for (int i = 0; i < results.length; i++) {
            final JSONObject entity = entities.getJSONObject(i);
            final String label = entity.getString("text");
            // Find possible URLs in the entities
            final HashSet<URI> uris = new HashSet<URI>();
            if (entity.has("disambiguated")) {
                final JSONObject disambiguated = entity.getJSONObject("disambiguated");
                final Iterator<String> keyIterator = disambiguated.keys();
                while (keyIterator.hasNext()) {
                    final String key = keyIterator.next();
                    if (!NONURIFIELDS.contains(key))
                        uris.add(createUri(disambiguated.getString(key)));
                }
            }
            // Create new named entity for the result
            results[i] = new NamedEntity(label, uris.toArray(new URI[uris.size()]));
        }
        return results;
    }
}
