package org.freeyourmetadata.ner.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONTokener;
import org.json.JSONWriter;

/**
 * Abstract base class for named-entity recognition services
 * with default support for JSON communication (but others are possible)
 * @author Ruben Verborgh
 */
public abstract class NERServiceBase implements NERService {
    /** The empty extraction result, containing no entities. */
    protected final static NamedEntity[] EMPTY_EXTRACTION_RESULT = new NamedEntity[0];
    
    private final static Charset UTF8 = Charset.forName("UTF-8");
    
    
    private final URI serviceUrl;
    private final String[] propertyNames;
    private final HashMap<String, String> properties;
    private final URI documentationUri;
    
    /**
     * Creates a new named-entity recognition service base class
     * @param propertyNames The names of supported properties
     */
    public NERServiceBase(final String[] propertyNames) {
        this(null, propertyNames, null);
    }
    
    /**
     * Creates a new named-entity recognition service base class
     * @param serviceUrl The URL of the service (can be null if not fixed)
     * @param propertyNames The names of supported properties
     */
    public NERServiceBase(final URI serviceUrl, final String[] propertyNames) {
       this(serviceUrl, propertyNames, null);
    }
    
    /**
     * Creates a new named-entity recognition service base class
     * @param serviceUrl The URL of the service (can be null if not fixed)
     * @param propertyNames The names of supported properties
     * @param documentationUri The URI of the service's documentation
     */
    public NERServiceBase(final URI serviceUrl, final String[] propertyNames, final URI documentationUri) {
        this.serviceUrl = serviceUrl;
        this.propertyNames = propertyNames;
        this.documentationUri = documentationUri;
        
        properties = new HashMap<String, String>(propertyNames.length);
        for (String propertyName : propertyNames)
            this.properties.put(propertyName, "");
    }
    
    /** {@inheritDoc} */
    @Override
    public String[] getPropertyNames() {
        return propertyNames;
    }

    /** {@inheritDoc} */
    @Override
    public String getProperty(final String name) {
        return properties.get(name);
    }

    /** {@inheritDoc} */
    @Override
    public void setProperty(final String name, final String value) {
        if (!properties.containsKey(name))
            throw new IllegalArgumentException("The property " + name
                                               + " is invalid for " + getClass().getName() + ".");
        properties.put(name, value == null ? "" : value);
    }
    
    /** {@inheritDoc} */
    @Override
    public NamedEntity[] extractNamedEntities(final String text) throws Exception {
        final HttpUriRequest request = createExtractionRequest(text);
        return performExtractionRequest(request);
    }
    
    /** {@inheritDoc} */
    public boolean isConfigured() {
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    public URI getDocumentationUri() {
        return documentationUri;
    }
    
    /**
     * Performs the named-entity recognition request
     * @param request The request
     * @return The extracted named entities
     * @throws Exception if the request fails
     */
    protected NamedEntity[] performExtractionRequest(final HttpUriRequest request) throws Exception {
        final DefaultHttpClient httpClient = new DefaultHttpClient();
        final HttpResponse response = httpClient.execute(request);
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
            throw new IllegalStateException(
                    String.format("The extraction request returned status code %d instead of %s.",
                                  response.getStatusLine().getStatusCode(), HttpStatus.SC_OK));
        final HttpEntity responseEntity = response.getEntity();
        return parseExtractionResponseEntity(responseEntity);
    }

    /**
     * Creates a named-entity recognition request on the specified text
     * @param text The text to analyze
     * @return The created request
     * @throws Exception if the request cannot be created
     */
    protected HttpUriRequest createExtractionRequest(final String text) throws Exception {
        final URI requestUrl = createExtractionRequestUrl(text);
        final HttpEntity body = createExtractionRequestBody(text);
        final HttpPost request = new HttpPost(requestUrl);
        request.setHeader("Accept", "application/json");
        request.setEntity(body);
        return request;
    }
    
    /**
     * Creates the URL for a named-entity recognition request on the specified text
     * @param text The text to analyze
     * @return The created URL
     */
    protected URI createExtractionRequestUrl(final String text) {
        return serviceUrl;
    }

    /**
     * Creates the body for a named-entity recognition request on the specified text
     * @param text The text to analyze
     * @return The created body entity
     * @throws Exception if the request body cannot be created
     */
    protected HttpEntity createExtractionRequestBody(final String text) throws Exception {
        final ByteArrayOutputStream bodyOutput = new ByteArrayOutputStream();
        final JSONWriter bodyWriter = new JSONWriter(new OutputStreamWriter(bodyOutput, UTF8));
        try {
            writeExtractionRequestBody(text, bodyWriter);
        }
        catch (JSONException error) {
            throw new RuntimeException(error);
        }
        try {
            bodyOutput.close();
        }
        catch (IOException e) { }
        final byte[] bodyBytes = bodyOutput.toByteArray();
        final ByteArrayInputStream bodyInput = new ByteArrayInputStream(bodyBytes);
        final HttpEntity body = new InputStreamEntity(bodyInput, bodyBytes.length);
        return body;
    }
    
    /**
     * Writes the body JSON for a named-entity recognition request on the specified text
     * @param text The text to analyze
     * @param body The body writer
     * @throws JSONException if writing the body goes wrong
     */
    protected void writeExtractionRequestBody(final String text, final JSONWriter body) throws JSONException { }
    
    /**
     * Parses the entity of the named-entity recognition response
     * @param response A response of the named-entity extraction service
     * @return The extracted named entities
     * @throws Exception if the response cannot be parsed
     */
    protected NamedEntity[] parseExtractionResponseEntity(HttpEntity response) throws Exception {
        final InputStreamReader responseReader = new InputStreamReader(response.getContent());
        return parseExtractionResponseEntity(new JSONTokener(responseReader));
    }
    
    /**
     * Parses the JSON entity of the named-entity recognition response
     * @param tokener The tokener containing the response
     * @return The extracted named entities
     * @throws JSONException if the response cannot be parsed
     */
    protected NamedEntity[] parseExtractionResponseEntity(final JSONTokener tokener) throws JSONException {
        return EMPTY_EXTRACTION_RESULT;
    }
    
    /**
     * Encodes the specified text for use in an URL.
     * @param text The text to encode
     * @return The encoded text
     */
    protected static String urlEncode(String text) {
        try {
            return URLEncoder.encode(text, "UTF-8");
        }
        catch (UnsupportedEncodingException error) {
            throw new RuntimeException(error);
        }
    }
}
