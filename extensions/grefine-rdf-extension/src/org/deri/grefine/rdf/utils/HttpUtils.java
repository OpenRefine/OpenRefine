package org.deri.grefine.rdf.utils;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some HTTP utilities
 * 
 * @author Sergio Fernández <sergio.fernandez@salzburgresearch.at>
 *
 */
public class HttpUtils {
	
	private static Logger log = LoggerFactory.getLogger(HttpUtils.class);
	public static final String USER_AGENT = "Google Refine LMF Extension (beta)";
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
