package org.deri.grefine.reconcile.sindice;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;


import org.deri.grefine.reconcile.rdf.factories.JenaTextSparqlQueryFactory;
import org.deri.grefine.reconcile.rdf.factories.SparqlQueryFactory;
import org.deri.grefine.reconcile.util.GRefineJsonUtilities;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class SindiceBroker {
	final static Logger logger = LoggerFactory.getLogger("SindiceBroker");
	private final String sindiceSearchUrl = "http://api.sindice.com/v2/search";
	private SparqlQueryFactory queryFactory = new JenaTextSparqlQueryFactory();
	
	public List<String> guessDomain(String query, int limit, GRefineJsonUtilities jsonUtilities) {
		Model model;
		String domain;
		SindiceQueryEndpoint endpoint;
		ImmutableList<String> empty = ImmutableList.of();
		List<String> domains = new LinkedList<String>();
		try {
			LinkedHashSet<String[]> urlPairs = getUrlsForSimpleTermSearch(query, GUESS_DOMAIN_SEARCH_LIMIT, jsonUtilities);
			for(String[] pair: urlPairs){
				domain = getDomainForUrl(pair[0], pair[1], jsonUtilities);
				if(domains.contains(domain)){
					continue;
				}
				model = getModelForUrl(pair[0], pair[1], jsonUtilities);
				endpoint = new SindiceQueryEndpoint(queryFactory);
				if(endpoint.hasResult(model, query, empty, limit - domains.size())){
					domains.add(domain);
				}
				if(domains.size()>=limit){
					break;
				}
			}
			
			return domains;
			
		} catch (Exception e) {
			logger.error("error reconcling using Sindice API", e);
		}
		return domains;
	}
	
	public Model getModelForUrl(String url, String cacheUrl, GRefineJsonUtilities jsonUtilities) throws JSONException, IOException {
		JSONObject sindiceCacheObj = jsonUtilities.getJSONObjectFromUrl(new URL(cacheUrl));
        JSONArray rdfContentArr = sindiceCacheObj.getJSONObject(url).getJSONArray("explicit_content");
        StringBuilder buff = new StringBuilder();
        for(int j=0;j<rdfContentArr.length();j++){
        	String triple = rdfContentArr.getString(j);
        	buff.append(triple);
        }
        Model m = ModelFactory.createDefaultModel();
		m.read(new StringReader(buff.toString()), null, "N-TRIPLE");
		
		return m;
	}

	private String getDomainForUrl(String url, String cacheUrl, GRefineJsonUtilities jsonUtilities) throws JSONException, IOException {
		JSONObject sindiceCacheObj = jsonUtilities.getJSONObjectFromUrl(new URL(cacheUrl));
		String domain = sindiceCacheObj.getJSONObject(url).getString("domain");
		return domain;
	}

	/**
	 * @param q
	 * @return LinkedHashSet of string-pairs. each pair is (in order) URL of the document
	 *         matching the search for q and the Sindice cache URL for the
	 *         document. the LinkedHashSet assures no duplication while keeping the order of addition
	 * @throws IOException
	 * @throws JSONException
	 */
	public LinkedHashSet<String[]> getUrlsForSimpleTermSearch(String q, int searchLimit, GRefineJsonUtilities jsonUtilities) throws JSONException, IOException {
		return getUrlsForSimpleTermSearch(q, null, null, searchLimit, jsonUtilities);
	}
	
	public LinkedHashSet<String[]> getUrlsForSimpleTermSearch(String q, String domain, String type, int searchLimit, GRefineJsonUtilities jsonUtilities) throws JSONException, IOException {
		LinkedHashSet<String[]> lst = new LinkedHashSet<String[]>();
		URL url = buildUrl(q,domain, type, searchLimit);
		JSONObject documentsObj = jsonUtilities.getJSONObjectFromUrl(url);
		JSONArray entries = documentsObj.getJSONArray("entries");
		int length = Math.min(entries.length(), searchLimit);
		for (int i = 0; i < length; i++) {
			String link = entries.getJSONObject(i).getString("link");
			String cache = entries.getJSONObject(i).getString("cache");
			lst.add(new String[] {link, cache + "&field=domain"});
		}
		
		return lst;
	}

	private URL buildUrl(String q, String domain, String type, int searchLimit) throws MalformedURLException {
		try {
			//TODO
			if(type!=null){type = type.replace("#", "%23");}
			String typeFilter = type==null?"":"class:\""+type+"\"";
			String domainFilter = domain==null?"":"domain:"+URLEncoder.encode(domain,"UTF-8")+ "%20";
			 
			String fq =domainFilter  + typeFilter; 
			//FIXME couldn't figure out how to tell Sindice how many items I want (itemsPerPage)
			String query = String.format("q=%s&qt=%s&fq=%s",
					URLEncoder.encode(q , "UTF-8"),
					URLEncoder.encode("term", "UTF-8"),
					fq);
			return new URL(sindiceSearchUrl + "?" + query);
		} catch (UnsupportedEncodingException e) {
			// should never get here
			throw new RuntimeException(e);
		}

	}
	
	static class ModelAndDomain{
		final String domain;
		final Model model;
		public ModelAndDomain(String domain, Model m) {
			this.domain = domain;
			this.model = m;
		}
		public String getDomain() {
			return domain;
		}
		public Model getModel() {
			return model;
		}
	}
	
	static final int DEFAULT_SEARCH_LIMIT = 3;
	static final int GUESS_DOMAIN_SEARCH_LIMIT = 8;
}
