package org.freeyourmetadata.ner.commands;

import javax.servlet.http.HttpServletRequest;

import org.freeyourmetadata.ner.services.NERServiceManager;
import org.json.JSONArray;
import org.json.JSONWriter;

/**
 * Servlet that provides read/write access to <tt>NERServiceManager</tt>
 * @author Ruben Verborgh
 */
public class ServicesCommand extends NERCommand {
    private final NERServiceManager serviceManager;
    
    /**
     * Creates a new <tt>ServicesCommand</tt>
     * @param serviceManager The data source
     */
    public ServicesCommand(final NERServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }
    
    /** {@inheritDoc} */
    @Override
    public void get(final HttpServletRequest request, final JSONWriter response) throws Exception {
        serviceManager.writeTo(response);
    }
    
    /** {@inheritDoc} */
    @Override
    public void put(final HttpServletRequest request, final Object body, final JSONWriter response) throws Exception {
        if(!(body instanceof JSONArray))
            throw new IllegalArgumentException("Body should be a JSON array.");
        serviceManager.updateFrom((JSONArray)body);
        serviceManager.save();
    }
}
