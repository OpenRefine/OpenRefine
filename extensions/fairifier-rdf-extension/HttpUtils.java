package org.deri.grefine.rdf.utils;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.http.params.*;
import java.util.List; 
import org.apache.http.NameValuePair;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.commons.codec.binary.Base64;


/**
 * Some HTTP utilities
 * 
 * @author Sergio Fernández <sergio.fernandez@salzburgresearch.at>
 * @author Shamanou van Leeuwen
 */
public class HttpUtils {
	
	private static Logger log = LoggerFactory.getLogger(HttpUtils.class);
	public static final String USER_AGENT = "DTLS FAIRifier";
	public static final int CONNECTION_TIMEOUT = 10000;
	public static final int SO_TIMEOUT = 60000;
    private static final int MAX_REDIRECTS = 3;
	
	public static HttpClient createClient() {
        HttpParams httpParams = new BasicHttpParams();
        httpParams.setParameter(CoreProtocolPNames.USER_AGENT, USER_AGENT);
        httpParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, SO_TIMEOUT);
        httpParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
        httpParams.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS ,true);
        httpParams.setIntParameter(ClientPNames.MAX_REDIRECTS, MAX_REDIRECTS);
        return new DefaultHttpClient(httpParams);
    }
	
	public static HttpEntity get(String uri) throws IOException {
	    log.debug("GET request over " + uri);
            HttpGet get = new HttpGet(uri);
            return get(get);
	}
	
	public static HttpEntity get(String uri, String accept) throws IOException {
	    log.debug("GET request over " + uri);
        HttpGet get = new HttpGet(uri);
        get.setHeader("Accept", accept);
        return get(get);
	}

	public static HttpEntity put(String uri,  String content, String username, String password) throws IOException {
        log.debug("PUT request over " + uri);
        String auth = username + ":" + password;
		String encodedAuth = Base64.encodeBase64String(auth.getBytes());
        HttpPut put = new HttpPut(uri);
        put.setHeader("Authorization","Basic "+ encodedAuth);
        put.setHeader("Content-Type","text/turtle");
        put.setEntity(new StringEntity(content));
        return put(put);
    }
	
	public static HttpEntity post(String uri,  String content, String content_type) throws IOException {
        log.debug("POST request over " + uri);
        HttpPost post = new HttpPost(uri);
        post.setHeader("Content-Type",content_type);
        post.setEntity(new StringEntity(content));
        return post(post);
    }

	private static HttpEntity put(HttpPut put) throws IOException {
	    HttpClient client = createClient();
	    HttpResponse response = client.execute(put);
	    if (201 == response.getStatusLine().getStatusCode()) {
	        return response.getEntity();
	    } else {
	        String msg = "Error performing PUT request: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();
	        log.error(msg);
	        throw new ClientProtocolException(msg);
	    }
	}
        
	private static HttpEntity post(HttpPost post) throws IOException {
	    HttpClient client = createClient();
	    HttpResponse response = client.execute(post);
	    if (201 == response.getStatusLine().getStatusCode()) {
	        return response.getEntity();
	    } else {
	        String msg = "Error performing POST request: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();
	        log.error(msg);
	        throw new ClientProtocolException(msg);
	    }
	}
	
	private static HttpEntity get(HttpGet get) throws IOException {
		HttpClient client = createClient();
		HttpResponse response = client.execute(get);
		if (200 == response.getStatusLine().getStatusCode()) {
			return response.getEntity();
		} else {
			String msg = "Error performing GET request: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();
			log.error(msg);
			throw new ClientProtocolException(msg);
		}
	}

}
