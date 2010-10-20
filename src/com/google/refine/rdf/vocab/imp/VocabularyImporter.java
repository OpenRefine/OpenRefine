package com.google.refine.rdf.vocab.imp;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.deri.any23.Any23;
import org.deri.any23.http.HTTPClient;
import org.deri.any23.source.DocumentSource;
import org.deri.any23.source.HTTPDocumentSource;
import org.deri.any23.writer.ReportingTripleHandler;
import org.deri.any23.writer.RepositoryWriter;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.openrdf.sail.memory.MemoryStore;

import com.google.refine.rdf.vocab.RDFSClass;
import com.google.refine.rdf.vocab.RDFSProperty;
import com.google.refine.rdf.vocab.VocabularyImportException;

public class VocabularyImporter {
	
	public Vocabulary importVocabulary(String name, String uri) throws VocabularyImportException{
		VocabularyImportReport report = new VocabularyImportReport();
		Repository repos = getModel(uri, report);
		return getVocabulary(repos, name, uri, report);
	}
	
	
	private static final String PREFIXES = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> "
			+ "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
			+ "PREFIX skos:<http://www.w3.org/2004/02/skos/core#> ";
	private static final String CLASSES_QUERY_P1 = PREFIXES
			+ "SELECT ?resource ?label ?en_label ?description ?en_description ?definition ?en_definition "
			+ "WHERE { "
			+ "?resource rdf:type rdfs:Class. "
			+ "OPTIONAL {?resource rdfs:label ?label.} "
			+ "OPTIONAL {?resource rdfs:label ?en_label. FILTER langMatches( lang(?en_label), \"EN\" )  } "
			+ "OPTIONAL {?resource rdfs:comment ?description.} "
			+ "OPTIONAL {?resource rdfs:comment ?en_description. FILTER langMatches( lang(?en_description), \"EN\" )  } "
			+ "OPTIONAL {?resource skos:definition ?definition.} "
			+ "OPTIONAL {?resource skos:definition ?en_definition. FILTER langMatches( lang(?en_definition), \"EN\" )  } "
			+ "FILTER regex(str(?resource), \"^";
	private static final String CLASSES_QUERY_P2 = "\")}";

	private static final String PROPERTIES_QUERY_P1 = PREFIXES
			+ "SELECT ?resource ?label ?en_label ?description ?en_description ?definition ?en_definition "
			+ "WHERE { "
			+ "?resource rdf:type rdf:Property. "
			+ "OPTIONAL {?resource rdfs:label ?label.} "
			+ "OPTIONAL {?resource rdfs:label ?en_label. FILTER langMatches( lang(?en_label), \"EN\" )  } "
			+ "OPTIONAL {?resource rdfs:comment ?description.} "
			+ "OPTIONAL {?resource rdfs:comment ?en_description. FILTER langMatches( lang(?en_description), \"EN\" )  } "
			+ "OPTIONAL {?resource skos:definition ?definition.} "
			+ "OPTIONAL {?resource skos:definition ?en_definition. FILTER langMatches( lang(?en_definition), \"EN\" )  } "
			+ "FILTER regex(str(?resource), \"^";
	private static final String PROPERTIES_QUERY_P2 = "\")}";

	private Repository getModel(String url, VocabularyImportReport report)
			throws VocabularyImportException {

		try {
			Any23 runner = new Any23();
			runner.setHTTPUserAgent("google-refine-rdf-extension");
			HTTPClient client = runner.getHTTPClient();
			DocumentSource source = new HTTPDocumentSource(client, url);
			Repository repository = new SailRepository(
					new ForwardChainingRDFSInferencer(new MemoryStore()));
			repository.initialize();
			RepositoryConnection con = repository.getConnection();
			RepositoryWriter w = new RepositoryWriter(con);
			ReportingTripleHandler reporter = new ReportingTripleHandler(w);
			runner.extract(source, reporter);

			report.setUrl(url);
			report.setExtractors(reporter.getExtractorNames().toString());
			report.setImportedDate(new Date());

			return repository;
		} catch (Exception e) {
			throw new VocabularyImportException(
					"Unable to import vocabulary from " + url, e);
		}
	}

	private Vocabulary getVocabulary(Repository repos, String name, String uri,
			VocabularyImportReport report) throws VocabularyImportException {
		try {
			Vocabulary v = new Vocabulary(name,uri);
			RepositoryConnection con = repos.getConnection();
			try {

				TupleQuery query = con.prepareTupleQuery(QueryLanguage.SPARQL,
						CLASSES_QUERY_P1 + uri + CLASSES_QUERY_P2);
				TupleQueryResult res = query.evaluate();

				Set<String> seen = new HashSet<String>();
				while (res.hasNext()) {
					BindingSet solution = res.next();
					String clazzURI = solution.getValue("resource")
							.stringValue();
					if (seen.contains(clazzURI)) {
						continue;
					}
					seen.add(clazzURI);
					String label = getFirstNotNull(new Value[] {
							solution.getValue("en_label"),
							solution.getValue("label") });
					String description = getFirstNotNull(new Value[] {
							solution.getValue("en_definition"),
							solution.getValue("definition"),
							solution.getValue("en_description"),
							solution.getValue("description") });
					RDFSClass clazz = new RDFSClass(clazzURI, label,
							description, name, uri);
					v.addClass(clazz);
				}

				query = con.prepareTupleQuery(QueryLanguage.SPARQL,
						PROPERTIES_QUERY_P1 + uri + PROPERTIES_QUERY_P2);
				res = query.evaluate();
				report.setNumOfClasses(seen.size());

				seen = new HashSet<String>();
				while (res.hasNext()) {
					BindingSet solution = res.next();
					String propertyUri = solution.getValue("resource")
							.stringValue();
					if (seen.contains(propertyUri)) {
						continue;
					}
					seen.add(propertyUri);
					String label = getFirstNotNull(new Value[] {
							solution.getValue("en_label"),
							solution.getValue("label") });
					String description = getFirstNotNull(new Value[] {
							solution.getValue("en_definition"),
							solution.getValue("definition"),
							solution.getValue("en_description"),
							solution.getValue("description") });
					RDFSProperty prop = new RDFSProperty(propertyUri, label,
							description, name, uri);
					v.addProperty(prop);
				}

				report.setNumOfProperties(seen.size());
				
				v.setReport(report);
				return v;

			} catch (Exception ex) {
				throw new VocabularyImportException(
						"Error while processing vocabulary retrieved from "
								+ uri, ex);
			} finally {
				con.close();
			}
		} catch (RepositoryException ex) {
			throw new VocabularyImportException(
					"Error while processing vocabulary retrieved from " + uri,
					ex);
		}
	}
	
	private String getFirstNotNull(Value[] values) {
		String s = null;
		for (int i = 0; i < values.length; i++) {
			s = getString(values[i]);
			if (s != null) {
				break;
			}
		}
		return s;
	}

	private String getString(Value v) {
		if (v != null) {
			return v.stringValue();
		}
		return null;
	}

}
