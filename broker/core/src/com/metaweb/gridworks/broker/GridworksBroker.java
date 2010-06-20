
package com.metaweb.gridworks.broker;

import java.io.IOException;
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
 * This class contains all the code shared by various implementations of a Gridworks Broker.
 * 
 * A broker is a server used by multiple Gridworks installations to enable collaborative
 * development over the same project.
 * 
 * Broker implementations differ in how they store their state but all of them are required
 * to extend this abstract class and implement the services that are called via HTTP.
 * 
 */
public abstract class GridworksBroker extends ButterflyModuleImpl {
            
    protected static final Logger logger = LoggerFactory.getLogger("gridworks.broker");
    
    static final protected String USER_INFO_URL = "http://www.freebase.com/api/service/user_info";
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
    
    protected HttpClient httpclient;

    @Override
    public void init(ServletConfig config) throws Exception {
        super.init(config);
        httpclient = getHttpClient();
    }
    
    @Override
    public void destroy() throws Exception {
        httpclient.getConnectionManager().shutdown();       
    }
    
    @Override
    public boolean process(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("> process {}", path);
        } else {
            logger.info("process {}", path);
        }

        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");
        
        try {
            String uid = getUserId(request);
            logger.debug("uid: {}", path);
            String pid = getParameter(request, "pid");
            logger.debug("pid: {}", path);
            
            // NOTE: conditionals should be ordered by call frequency estimate to (slightly) improve performance
            // we could be using a hashtable and some classes that implement the commands, but the complexity overhead
            // doesn't seem to justify the marginal benefit.
            
            if ("get_lock".equals(path)) {
                getLock(response, pid);
            } else if ("obtain_lock".equals(path)) {
                obtainLock(response, pid, uid);
            } else if ("release_lock".equals(path)) {
                releaseLock(response, pid, uid, getParameter(request, "lock"));
            } else if ("history".equals(path)) {
                getHistory(response, pid, getInteger(request, "tindex"));
            } else if ("transform".equals(path)) {
                addTransformations(response, pid, uid, getParameter(request, "lock"), getList(request, "transformations"));
            } else if ("start".equals(path)) {
                startProject(response, pid, uid, getParameter(request, "lock"), getParameter(request, "data"));
            } else if ("get".equals(path)) {
                getProject(response, pid);
            }
            
        } catch (RuntimeException e) {
            logger.error("runtime error", e);
            respondError(response, e.getMessage());
        } catch (Exception e) {
            logger.error("internal error", e);
            respondException(response, e);
        }
        
        if (logger.isDebugEnabled()) logger.debug("< process {}", path);
        
        return super.process(path, request, response);
    }
    
    // ----------------------------------------------------------------------------------------
    
    protected abstract HttpClient getHttpClient();
    
    protected abstract void getLock(HttpServletResponse response, String pid) throws Exception;

    protected abstract void obtainLock(HttpServletResponse response, String pid, String uid) throws Exception;
    
    protected abstract void releaseLock(HttpServletResponse response, String pid, String uid, String lock) throws Exception;
    
    protected abstract void startProject(HttpServletResponse response, String pid, String uid, String lock, String data) throws Exception;

    protected abstract void addTransformations(HttpServletResponse response, String pid, String uid, String lock, List<String> transformations) throws Exception;
    
    protected abstract void getProject(HttpServletResponse response, String pid) throws Exception;
    
    protected abstract void getHistory(HttpServletResponse response, String pid, int tindex) throws Exception;
    
    // ----------------------------------------------------------------------------------------
    
    @SuppressWarnings("unchecked")
    protected String getUserId(HttpServletRequest request) throws Exception {

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
        httpRequest.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Gridworks Broker");
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
            throw new ServletException("request must come with a '" + name + "' parameter");
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

    static protected void respondError(HttpServletResponse response, String error) throws IOException, ServletException {

        if (response == null) {
            throw new ServletException("Response object can't be null");
        }
        
        try {
            JSONObject o = new JSONObject();
            o.put("code", "error");
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
            o.put("code", "error");
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
        
        JSONObject o = new JSONObject();
        respond(response, o.toString());
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
