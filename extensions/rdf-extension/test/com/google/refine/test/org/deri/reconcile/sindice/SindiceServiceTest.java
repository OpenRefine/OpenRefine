package com.google.refine.test.org.deri.reconcile.sindice;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.refine.org.deri.reconcile.model.ReconciliationCandidate;
import com.google.refine.org.deri.reconcile.model.ReconciliationRequest;
import com.google.refine.org.deri.reconcile.rdf.endpoints.QueryEndpoint;
import com.google.refine.org.deri.reconcile.rdf.endpoints.QueryEndpointFactory;
import com.google.refine.org.deri.reconcile.sindice.SindiceBroker;
import com.google.refine.org.deri.reconcile.sindice.SindiceService;
import com.google.refine.org.deri.reconcile.util.GRefineJsonUtilities;
import com.google.refine.org.deri.reconcile.util.GRefineJsonUtilitiesImpl;
import com.google.refine.org.deri.reconcile.util.RdfUtilitiesImpl;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import static org.mockito.Mockito.*;

public class SindiceServiceTest {

	String name = "sindice-test";
	String query = "Deutschland";
	int limit =5;
	int DEFAULT_SEARCH_LIMIT = 8;
	int DOMAIN_SPECIFIED_SEARCH_LIMIT = 3;
	String domain = "dbpedia.org";
	
	SindiceService service;
	GRefineJsonUtilities jsonUtil;
	ReconciliationRequest request;
	LinkedHashSet<String[]> urlPairs; 
	QueryEndpointFactory mockQueryEndpointFactory; 
	SindiceBroker mockBroker;
	QueryEndpoint mockEndpoint; 
	
	@BeforeMethod
	public void setUp() throws Exception{
		jsonUtil = new GRefineJsonUtilitiesImpl();
		request = new ReconciliationRequest(query, limit);
		urlPairs = new LinkedHashSet<String[]>();
		urlPairs.add(new String[] {"http://doc1.result.url","http://doc1.sindice.cache.url"} );
		urlPairs.add(new String[] {"http://doc2.result.url","http://doc2.sindice.cache.url"} );

		/*
		 * Mocks
		 */
		mockBroker = mock(SindiceBroker.class);
		mockEndpoint = mock(QueryEndpoint.class);
		mockQueryEndpointFactory = mock(QueryEndpointFactory.class);
		when(mockQueryEndpointFactory.getLarqQueryEndpoint((Model)anyObject())).thenReturn(mockEndpoint);
		
		/*
		 * Configuring mocks
		 */
		//nulls are for domain and type i.e. no restriction on any
		when(mockBroker.getUrlsForSimpleTermSearch(query,null,null,DEFAULT_SEARCH_LIMIT,jsonUtil)).thenReturn(urlPairs);
		when(mockBroker.getUrlsForSimpleTermSearch(query,domain,null,DOMAIN_SPECIFIED_SEARCH_LIMIT,jsonUtil)).thenReturn(urlPairs);
		
		when(mockBroker.getModelForUrl("http://doc1.result.url", "http://doc1.sindice.cache.url", jsonUtil)).thenReturn(ModelFactory.createDefaultModel());
		when(mockBroker.getModelForUrl("http://doc2.result.url", "http://doc2.sindice.cache.url", jsonUtil)).thenReturn(ModelFactory.createDefaultModel());
	}
	
	@Test
	public void reconcileTest()throws Exception{
		/*
		 * testing
		 */
		service = new SindiceService(name, name, null, jsonUtil, new RdfUtilitiesImpl(), mockBroker, mockQueryEndpointFactory);
		service.reconcile(request);
		
		/*
		 * Verification
		 */
		verify(mockBroker).getUrlsForSimpleTermSearch(query,null,null,DEFAULT_SEARCH_LIMIT,jsonUtil);
		verify(mockBroker).getModelForUrl("http://doc1.result.url", "http://doc1.sindice.cache.url", jsonUtil);
		verify(mockBroker).getModelForUrl("http://doc2.result.url", "http://doc2.sindice.cache.url", jsonUtil);
		
		ImmutableList<String> empty = ImmutableList.of();
		verify(mockEndpoint, times(2)).reconcileEntities(request, empty, 0.9);
	}
	
	@Test
	public void reconcileTestWithEnoughResults()throws Exception{
		ImmutableList<String> empty = ImmutableList.of();
		List<ReconciliationCandidate> results = new ArrayList<ReconciliationCandidate>();
		//prepare enough results
		for(int i=0;i<limit;i++){
			results.add(new ReconciliationCandidate(String.valueOf(i), "", null, 0, false));
		}
		
		when(mockEndpoint.reconcileEntities(request, empty, 0.9)).thenReturn(results);
		/*
		 * testing
		 */
		service = new SindiceService(name, name, null, jsonUtil, new RdfUtilitiesImpl(), mockBroker, mockQueryEndpointFactory);
		service.reconcile(request);
		
		/*
		 * Verification
		 */
		verify(mockBroker).getUrlsForSimpleTermSearch(query,null,null,DEFAULT_SEARCH_LIMIT,jsonUtil);
		verify(mockBroker).getModelForUrl("http://doc1.result.url", "http://doc1.sindice.cache.url", jsonUtil);
		//the other URL will not be called
		
		verify(mockEndpoint, times(1)).reconcileEntities(request, empty, 0.9);
	}
	
	@Test
	public void reconcileTestWithDomain()throws Exception{
		/*
		 * testing
		 */
		service = new SindiceService(name, name, domain, jsonUtil, new RdfUtilitiesImpl(), mockBroker, mockQueryEndpointFactory);
		service.reconcile(request);
		
		/*
		 * Verification
		 */
		verify(mockBroker).getUrlsForSimpleTermSearch(query,domain,null,DOMAIN_SPECIFIED_SEARCH_LIMIT,jsonUtil);
		verify(mockBroker).getModelForUrl("http://doc1.result.url", "http://doc1.sindice.cache.url", jsonUtil);
		verify(mockBroker).getModelForUrl("http://doc2.result.url", "http://doc2.sindice.cache.url", jsonUtil);
		
		ImmutableList<String> empty = ImmutableList.of();
		verify(mockEndpoint, times(2)).reconcileEntities(request, empty, 0.9);
	}
	
	@Test
	public void reconcileTestWithType()throws Exception{
		/*
		 * testing
		 */
		SindiceService service = new SindiceService(name, name, null, jsonUtil, new RdfUtilitiesImpl(), mockBroker, mockQueryEndpointFactory);
		request.setTypes(new String[] {"httphttp://xmlns.com/foaf/0.1/Person"});
		service.reconcile(request);
		
		/*
		 * Verification
		 */
		verify(mockBroker).getUrlsForSimpleTermSearch(query,null,null,DEFAULT_SEARCH_LIMIT,jsonUtil);
		verify(mockBroker).getModelForUrl("http://doc1.result.url", "http://doc1.sindice.cache.url", jsonUtil);
		verify(mockBroker).getModelForUrl("http://doc2.result.url", "http://doc2.sindice.cache.url", jsonUtil);
		
		ImmutableList<String> empty = ImmutableList.of();
		verify(mockEndpoint, times(2)).reconcileEntities(request, empty, 0.9);
	}
	
}
