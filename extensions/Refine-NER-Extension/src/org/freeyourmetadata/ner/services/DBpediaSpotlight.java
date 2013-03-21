package org.freeyourmetadata.ner.services;

import static org.freeyourmetadata.util.UriUtil.createUri;

import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.freeyourmetadata.util.ParameterList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * DBpedia spotlight service connector
 * @author Ruben Verborgh
 */
public class DBpediaSpotlight extends NERServiceBase implements NERService {
    private final static URI SERVICEBASEURL = createUri("http://spotlight.dbpedia.org/rest/annotate");
    private final static String[] PROPERTYNAMES = { "Confidence", "Support" };

    /**
     * Creates a new DBpedia spotlight service connector
     */
    public DBpediaSpotlight() {
        super(SERVICEBASEURL, PROPERTYNAMES);
        setProperty("Confidence", "0.5");
        setProperty("Support", "30");
    }
    
    /** {@inheritDoc} */
    protected HttpEntity createExtractionRequestBody(final String text) throws UnsupportedEncodingException {
        final ParameterList parameters = new ParameterList();
        parameters.add("confidence", getProperty("Confidence"));
        parameters.add("support", getProperty("Support"));
        parameters.add("text", text);
        return parameters.toEntity();
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
