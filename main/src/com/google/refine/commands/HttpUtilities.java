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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.refine.RefineServlet;
import com.google.refine.util.ParsingUtilities;

abstract public class HttpUtilities {

    final static protected Logger logger = LoggerFactory.getLogger("command");

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

    static public void respond(HttpServletResponse response, String status, String message)
            throws IOException {

        Writer w = response.getWriter();
        JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);
        writer.writeStartObject();
        writer.writeStringField("status", status);
        writer.writeStringField("message", message);
        writer.writeEndObject();
        writer.flush();
        writer.close();
        w.flush();
        w.close();
    }

    static public void respondJSON(HttpServletResponse response, Object o)
            throws IOException {

        respondJSON(response, o, new Properties());
    }

    static public void respondJSON(
            HttpServletResponse response, Object o, Properties options)
            throws IOException {

        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");

        Writer w = response.getWriter();
        ParsingUtilities.defaultWriter.writeValue(w, o);
        w.flush();
        w.close();
    }

    static public void respondException(HttpServletResponse response, Exception e)
            throws IOException, ServletException {

        logger.warn("Exception caught", e);

        if (response == null) {
            throw new ServletException("Response object can't be null");
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();
        sw.flush();

        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");

        Writer w = response.getWriter();
        JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);
        writer.writeStartObject();
        writer.writeStringField("code", "error");
        writer.writeStringField("message", e.getMessage());
        writer.writeStringField("stack", sw.toString());
        writer.writeEndObject();
        writer.flush();
        writer.close();
        w.flush();
        w.close();
    }

    static public void redirect(HttpServletResponse response, String url) throws IOException {
        response.sendRedirect(url);
    }

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

    static public void respondWithErrorPage(
            RefineServlet servlet,
            HttpServletRequest request,
            HttpServletResponse response,
            String message,
            Throwable e) {
        respondWithErrorPage(servlet, request, response, message,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
    }

    static public void respondWithErrorPage(
            RefineServlet servlet,
            HttpServletRequest request,
            HttpServletResponse response,
            String message,
            int status,
            Throwable e) {
        VelocityContext context = new VelocityContext();

        context.put("message", message);

        if (e != null) {
            StringWriter writer = new StringWriter();

            e.printStackTrace(new PrintWriter(writer));

            context.put("stack", writer.toString());
        } else {
            context.put("stack", "");
        }

        try {
            response.setStatus(status);

            servlet.getModule("core").sendTextFromTemplate(
                    request, response, context, "error.vt", "UTF-8", "text/html", true);

        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }
}
