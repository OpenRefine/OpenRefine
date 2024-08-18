/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.commands;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.RefineServlet;
import com.google.refine.util.ParsingUtilities;

/**
 * @deprecated deprecated for v3.8. All methods have been deprecated and this class will be removed. Most users should
 *             be extending {@link Command} to get access to this functionality.
 */
@Deprecated
abstract public class HttpUtilities {

    final static protected Logger logger = LoggerFactory.getLogger("command");

    @Deprecated
    static public void respond(HttpServletResponse response, String content)
            throws IOException, ServletException {

        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        Writer w = response.getWriter();
        if (w != null) {
            w.write(content);
            w.flush();
            w.close();
        } else {
            throw new ServletException("response returned a null writer");
        }
    }

    @Deprecated
    static public void respond(HttpServletResponse response, String status, String message)
            throws IOException {
        // FIXME: This is the only place that uses status instead of code
        if (message == null) {
            respondJSON(response, Map.of("status", status));
        } else {
            respondJSON(response, Map.of(
                    "status", status,
                    "message", message));
        }
    }

    /**
     * @deprecated deprecated for v3.8. No internal uses. Move to {@link Command} when deprecation period expires.
     */
    @Deprecated
    static public void respondJSON(HttpServletResponse response, Object o)
            throws IOException {

        respondJsonInternal(response, o);
    }

    /**
     * TODO: options parameter is ignored here, but it's always empty in the only place it's used. DRY up
     *
     * @deprecated deprecated for v3.8. No internal uses. Move to {@link Command} when deprecation period expires.
     */
    @Deprecated
    static public void respondJSON(
            HttpServletResponse response, Object o, Properties options)
            throws IOException {

        respondJsonInternal(response, o);
    }

    // TODO: This can be inlined with 2-parameter method when 3-param is removed
    static private void respondJsonInternal(
            HttpServletResponse response, Object o)
            throws IOException {

        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");

        Writer w = response.getWriter();
        ParsingUtilities.defaultWriter.writeValue(w, o);
        w.flush();
        w.close();
    }

    /**
     * @deprecated deprecated for v3.8. No internal uses.
     */
    @Deprecated
    static public void respondException(HttpServletResponse response, Exception e)
            throws IOException, ServletException {

        logger.warn("Exception caught", e);

        if (response == null) {
            throw new ServletException("Response object can't be null");
        }
        // TODO: We would like to return an HTTP error here, but front end needs scrubbing first to
        // to make sure everywhere can handle it. There are known issues with things like the IllegalArgumentException
        // used for bad regexps
//        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        respondJSON(response, Map.of(
                "code", "error", // respondStatus() above uses status instead of code
                "message", e.getMessage(),
                "stack", ExceptionUtils.getStackTrace(e)));
    }

    /**
     * @deprecated deprecated for v3.8. No internal uses.
     */
    @Deprecated
    static public void redirect(HttpServletResponse response, String url) throws IOException {
        response.sendRedirect(url);
    }

    /**
     * @deprecated deprecated for v3.8. No internal uses. There is an implementation in the {@link Command} class for
     *             commands which need it.
     */
    @Deprecated
    static public int getIntegerParameter(HttpServletRequest request, String name, int def) {
        if (request == null) {
            throw new IllegalArgumentException("parameter 'request' should not be null");
        }
        try {
            return Integer.parseInt(request.getParameter(name));
        } catch (Exception e) {
            logger.warn("Error getting integer parameter", e);
        }
        return def;
    }

    /**
     * @deprecated deprecated for v3.8. No internal uses other than call from {@link Command} where this implementation
     *             can be moved when deprecation period expires.
     */
    @Deprecated
    static public void respondWithErrorPage(
            RefineServlet servlet,
            HttpServletRequest request,
            HttpServletResponse response,
            String message,
            Throwable e) {
        // TODO: Move implementation to {@link Command} when deprecation period expires
        respondWithErrorPage(servlet, request, response, message,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
    }

    /**
     * @deprecated deprecated for v3.8. Only internal use is invocation directly above
     */
    @Deprecated
    static public void respondWithErrorPage(
            RefineServlet servlet,
            HttpServletRequest request,
            HttpServletResponse response,
            String message,
            int status,
            Throwable e) {
        // TODO: Move implementation to {@link Command} when deprecation period expires
        VelocityContext context = new VelocityContext();

        context.put("message", message);
        context.put("stack", e == null ? "" : ExceptionUtils.getStackTrace(e));

        try {
            response.setStatus(status);

            servlet.getModule("core").sendTextFromTemplate(
                    request, response, context, "error.vt", "UTF-8", "text/html", true);

        } catch (Exception e1) {
            logger.error("Error processing Velocity template", e1);
        }
    }
}
