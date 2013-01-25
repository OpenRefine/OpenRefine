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
 * DBpedia lookup service connector
 * @author Ruben Verborgh
 */
public class DBpediaLookup extends NERServiceBase {
    private final static URI SERVICEBASEURL = createUri("http://spotlight.dbpedia.org/rest/annotate");
    private final static String[] PROPERTYNAMES = { "Confidence", "Support" };

    /**
     * Creates a new DBpedia lookup service connector
     */
    public DBpediaLookup() {
        super(SERVICEBASEURL, PROPERTYNAMES);
        setProperty("Confidence", "0.5");
        setProperty("Support", "30");
    }
    
    /** {@inheritDoc} */
    protected HttpEntity createExtractionRequestBody(final String text) throws UnsupportedEncodingException {
        final ArrayList<NameValuePair> parameters = new ArrayList<NameValuePair>(3);
        parameters.add(new BasicNameValuePair("confidence", getProperty("Confidence")));
        parameters.add(new BasicNameValuePair("support", getProperty("Support")));
        parameters.add(new BasicNameValuePair("text", text));
        return new UrlEncodedFormEntity(parameters);
    }
    
    /** {@inheritDoc} */
    @Override
    protected NamedEntity[] parseExtractionResponseEntity(final JSONTokener tokener) throws JSONException {
        final JSONObject response = (JSONObject)tokener.nextValue();
        // Empty result if no resources were found
        if (!response.has("Resources"))
            return EMPTY_EXTRACTION_RESULT;
        // Extract resources
        final JSONArray resources = response.getJSONArray("Resources");
        final NamedEntity[] results = new NamedEntity[resources.length()];
        for (int i = 0; i < resources.length(); i++) {
            final JSONObject resource = resources.getJSONObject(i);
            results[i] = new NamedEntity(resource.getString("@surfaceForm"),
                                         createUri(resource.getString("@URI")));
        }
        return results;
    }
}
