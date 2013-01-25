package org.freeyourmetadata.ner.commands;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONTokener;
import org.json.JSONWriter;

import com.google.refine.commands.Command;

/**
 * Base class for JSON-based commands
 * @author Ruben Verborgh
 */
public abstract class NERCommand extends Command {
    /** {@inheritDoc} */
    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        final JSONWriter writer = createResponseWriter(response);
        try {
            get(request, writer);
        }
        catch (Exception error){
            error.printStackTrace();
            throw new ServletException(error);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public void doPut(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        final JSONWriter writer = createResponseWriter(response);
        final JSONTokener tokener = new JSONTokener(request.getReader());
        try {
            put(request, tokener.nextValue(), writer);
        }
        catch (Exception error){
            error.printStackTrace();
            throw new ServletException(error);
        }
    }

    /**
     * Creates a JSON response writer and sets the content-type accordingly
     * @param response The response
     * @return The response writer
     * @throws IOException
     */
    protected JSONWriter createResponseWriter(final HttpServletResponse response) throws IOException {
        response.setHeader("Content-Type", "application/json");
        return new JSONWriter(response.getWriter());
    }
    
    /**
     * Handles a <tt>GET</tt> request
     * @param request The request
     * @param response The response writer
     * @throws Exception if something goes wrong
     */
    public void get(final HttpServletRequest request, final JSONWriter response) throws Exception {}
    
    /**
     * Handles a <tt>PUT</tt> request
     * @param request The request
     * @param body The parsed JSON object from the request body
     * @param response The response writer
     * @throws Exception if something goes wrong
     */
    public void put(final HttpServletRequest request, final Object body, final JSONWriter response) throws Exception {}
}
