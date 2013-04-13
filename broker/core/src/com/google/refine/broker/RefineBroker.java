/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/


package com.google.refine.broker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.simile.butterfly.ButterflyModuleImpl;

/**
 * This class contains all the code shared by various implementations of a OpenRefine Broker.
 * 
 * A broker is a server used by multiple OpenRefine installations to enable collaborative
 * development over the same project.
 * 
 * Broker implementations differ in how they store their state but all of them are required
 * to extend this abstract class and implement the services that are called via HTTP.
 * 
 */
public abstract class RefineBroker extends ButterflyModuleImpl {
    
    static final public String GET_STATE = "get_state";
    static final public String EXPIRE = "expire";
    static final public String OBTAIN_LOCK = "obtain_lock";
    static final public String RELEASE_LOCK = "release_lock";
    static final public String TRANSFORM = "transform";
    static final public String START = "start";
    static final public String OPEN = "open";

    static final public int ALL = 0;
    static final public int COL = 1;
    static final public int CELL = 2;
    
    static final protected Logger logger = LoggerFactory.getLogger("refine.broker");
    
    // TODO: This API is deprecated.
    static final protected String USER_INFO_URL = "http://api.freebase.com/api/service/user_info";
    static final protected String DELEGATED_OAUTH_HEADER = "X-Freebase-Credentials";
    static final protected String OAUTH_HEADER = "Authorization";

    static protected String OK; 
    
    static {
        try {
            JSONObject o = new JSONObject();
            o.put("status","ok");
            OK = o.toString();
        } catch (JSONException e) {
            // not going to happen;
        }
    }
    
    static public final long LOCK_DURATION = 60 * 1000; // 1 minute
    static public final long USER_DURATION = 5 * 60 * 1000; // 1 minute
    static public final long LOCK_EXPIRATION_CHECK_DELAY = 5 * 1000; // 5 seconds
    
    protected HttpClient httpclient;

    protected boolean developmentMode;
    
    @Override
    public void init(ServletConfig config) throws Exception {
        super.init(config);
        httpclient = getHttpClient();
        developmentMode = Boolean.parseBoolean(config.getInitParameter("refine.development"));
        if (developmentMode) logger.warn("Running in development mode");
    }

    @Override
    public void destroy() throws Exception {
        httpclient.getConnectionManager().shutdown();       
    }
    
    @Override
    public boolean process(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("> process '{}'", path);
        } else {
            logger.info("process '{}'", path);
        }

        try {

            if (GET_STATE.equals(path)) {
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Content-Type", "application/json");
                getState(response, getParameter(request, "pid"), getUserId(request), getInteger(request, "rev"));
            } else if (EXPIRE.equals(path)) {
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Content-Type", "application/json");
                expire(response);
            } else if (OBTAIN_LOCK.equals(path)) {
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Content-Type", "application/json");
                obtainLock(response, getParameter(request, "pid"), getUserId(request), getInteger(request, "locktype"), getParameter(request, "lockvalue"));
            } else if (RELEASE_LOCK.equals(path)) {
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Content-Type", "application/json");
                releaseLock(response, getParameter(request, "pid"), getUserId(request), getParameter(request, "lock"));
            } else if (TRANSFORM.equals(path)) {
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Content-Type", "application/json");
                addTransformations(response, getParameter(request, "pid"), getUserId(request), getParameter(request, "lock"), getList(request, "transformations"));
            } else if (START.equals(path)) {
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Content-Type", "application/json");
                startProject(response, getParameter(request, "pid"), getUserId(request), getParameter(request, "lock"), getData(request), getParameter(request, "metadata"), getList(request, "transformations"));
            } else if (OPEN.equals(path)) {
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Content-Type", "application/json");
                openProject(response, getParameter(request, "pid"));
            } else {
                boolean value = super.process(path, request, response);
                if (logger.isDebugEnabled()) logger.debug("< process '{}'", path);
                return value;
            }
                
        } catch (RuntimeException e) {
            logger.error("runtime error", e.getMessage());
            respondError(response, e.getMessage());
        } catch (Exception e) {
            logger.error("internal error", e);
            respondException(response, e);
        }
        
        if (logger.isDebugEnabled()) logger.debug("< process '{}'", path);
        
        return true;
    }
    
    // ----------------------------------------------------------------------------------------
    
    protected abstract HttpClient getHttpClient();

    protected abstract void expire(HttpServletResponse response) throws Exception;

    protected abstract void getState(HttpServletResponse response, String pid, String uid, int rev) throws Exception;

    protected abstract void obtainLock(HttpServletResponse response, String pid, String uid, int locktype, String lockvalue) throws Exception;
    
    protected abstract void releaseLock(HttpServletResponse response, String pid, String uid, String lock) throws Exception;

    protected abstract void startProject(HttpServletResponse response, String pid, String uid, String lock, byte[] data, String metadata, List<String> transformations) throws Exception;

    protected abstract void addTransformations(HttpServletResponse response, String pid, String uid, String lock, List<String> transformations) throws Exception;
    
    protected abstract void openProject(HttpServletResponse response, String pid) throws Exception;
    
    // ----------------------------------------------------------------------------------------
    
    @SuppressWarnings("unchecked")
    protected String getUserId(HttpServletRequest request) throws Exception {

        // This is useful for testing
        if (developmentMode) {
            return getParameter(request, "uid");
        }
        
        String oauth = request.getHeader(DELEGATED_OAUTH_HEADER);
        if (oauth == null) {
            throw new RuntimeException("The request needs to contain the '" + DELEGATED_OAUTH_HEADER + "' header set to obtain user identity via Freebase.");
        }
        
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        Map<String,String> params = (Map<String,String>) request.getParameterMap();
        for (Entry<String,String> e : params.entrySet()) {
            formparams.add(new BasicNameValuePair((String) e.getKey(), (String) e.getValue()));
        }
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");

        HttpPost httpRequest = new HttpPost(USER_INFO_URL);
        httpRequest.setHeader(OAUTH_HEADER, oauth);
        httpRequest.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "OpenRefine Broker");
        httpRequest.setEntity(entity);
                
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = httpclient.execute(httpRequest, responseHandler);
        JSONObject o = new JSONObject(responseBody);
        
        return o.getString("username");
    }

    // ----------------------------------------------------------------------------------------

    static protected String getParameter(HttpServletRequest request, String name) throws ServletException {
        String param = request.getParameter(name);
        if (param == null) {
            throw new RuntimeException("request must come with a '" + name + "' parameter");
        }
        return param;
    }
    
    static protected List<String> getList(HttpServletRequest request, String name) throws ServletException, JSONException {
        String param = getParameter(request, name);
        JSONArray a = new JSONArray(param);
        List<String> result = new ArrayList<String>(a.length());
        for (int i = 0; i < a.length(); i++) {
            result.add(a.getString(i));
        }
        return result;
    }
    
    static protected int getInteger(HttpServletRequest request, String name) throws ServletException, JSONException {
        return Integer.parseInt(getParameter(request, name));
    }
    
    static protected byte[] getData(HttpServletRequest request) throws ServletException, IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        InputStream input = request.getInputStream();
        byte[] buffer = new byte[4096];
        int count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return output.toByteArray();
        
    }
    
    static protected void respondError(HttpServletResponse response, String error) throws IOException, ServletException {

        if (response == null) {
            throw new ServletException("Response object can't be null");
        }
        
        try {
            JSONObject o = new JSONObject();
            o.put("status", "error");
            o.put("message", error);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            respond(response, o.toString());
        } catch (JSONException e) {
            e.printStackTrace(response.getWriter());
        }
    }
    
    static protected void respondException(HttpServletResponse response, Exception e) throws IOException, ServletException {

        if (response == null) {
            throw new ServletException("Response object can't be null");
        }
        
        try {
            JSONObject o = new JSONObject();
            o.put("status", "error");
            o.put("message", e.getMessage());
        
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            sw.flush();
        
            o.put("stack", sw.toString());
        
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            respond(response, o.toString());
        } catch (JSONException e1) {
            e.printStackTrace(response.getWriter());
        }
    }

    static protected void respond(HttpServletResponse response, JSONObject content) throws IOException, ServletException {
        if (content == null) {
            throw new ServletException("Content object can't be null");
        }
        
        respond(response, content.toString());
    }

    static protected void respond(HttpServletResponse response, String content) throws IOException, ServletException {
        if (response == null) {
            throw new ServletException("Response object can't be null");
        }
        
        Writer w = response.getWriter();
        if (w != null) {
            w.write(content);
            w.flush();
            w.close();
        } else {
            throw new ServletException("response returned a null writer");
        }
    }
}
