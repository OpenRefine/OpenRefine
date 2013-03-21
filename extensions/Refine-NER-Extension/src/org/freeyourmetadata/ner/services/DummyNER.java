package org.freeyourmetadata.ner.services;

import static org.freeyourmetadata.util.UriUtil.createUri;

import java.io.ByteArrayInputStream;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import com.google.common.base.Charsets;

/**
 * DummyNER service connector
 * @author Ruben Verborgh
 */
public class DummyNER extends NERServiceBase {
    private final static URI SERVICEURL = createUri("http://dummyner.freeyourmetadata.org/");
    private final static String[] PROPERTYNAMES = { "API user", "API key" };
    
    /**
     * Create a new DummyNER service connector
     */
    public DummyNER() {
        super(SERVICEURL, PROPERTYNAMES);
        setProperty("API user", "ABCDEFGHIJKL");
        setProperty("API key",  "KLMNOPQRSTUV");
    }
    
    /** {@inheritDoc} */
    public boolean isConfigured() {
        return getProperty("API user").length() > 0 && getProperty("API key").length() > 0;
    }
    
    /** {@inheritDoc} */
    @Override
    protected HttpEntity createExtractionRequestBody(final String text) {
        final byte[] textBytes = text.getBytes(Charsets.UTF_8);
        final ByteArrayInputStream textStream = new ByteArrayInputStream(textBytes);
        return new InputStreamEntity(textStream, textBytes.length);
    }
    
    /** {@inheritDoc} */
    @Override
    protected NamedEntity[] parseExtractionResponseEntity(final JSONTokener tokener) throws JSONException {
        final JSONArray resultsJson = (JSONArray)tokener.nextValue();
        final NamedEntity[] results = new NamedEntity[resultsJson.length()];
        for (int i = 0; i < results.length; i++)
            results[i] = new NamedEntity(resultsJson.getString(i));
        return results;
    }
}
