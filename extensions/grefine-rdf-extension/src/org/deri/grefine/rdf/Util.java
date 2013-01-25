package org.deri.grefine.rdf;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Properties;

import org.deri.grefine.rdf.app.ApplicationContext;
import org.deri.grefine.rdf.expr.util.RdfExpressionUtil;
import org.deri.grefine.rdf.vocab.VocabularyIndexException;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class Util {

	private static final String XSD_INT_URI = "http://www.w3.org/2001/XMLSchema#int";
	private static final String XSD_DOUBLE_URI = "http://www.w3.org/2001/XMLSchema#double";
	private static final String XSD_DATE_URI = "http://www.w3.org/2001/XMLSchema#date";

	public static String resolveUri(URI base, String rel) {
		// FIXME
		try {
			URI relUri = new URI(rel);
			if (relUri.isAbsolute()) {
				return rel;
			}
		} catch (URISyntaxException e) {
			// silent
		}
		String res;
		try{
			res = resolveRelativeURI(base,rel);
			new URI(res);
			return res;
		}catch(Exception ex){
			//try encoding
			String encodedRel;
			try {
				encodedRel = URLEncoder.encode(rel, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				//silent
				return "";
			}
			return resolveRelativeURI(base, encodedRel);
		}
	}

	private static String resolveRelativeURI(URI base, String rel){
		if (base.getFragment() != null) {
			return base + rel;
		}
		return base.resolve(rel).toString();
	}
	
	public static String getDataType(URI base,String s) {
		if (s == null) {
			return null;
		}
		if (s.equals(XSD_INT_URI)) {
			return XSD_INT_URI;
		}
		if (s.equals(XSD_DOUBLE_URI)) {
			return XSD_DOUBLE_URI;
		}
		if (s.equals(XSD_DATE_URI)) {
			return XSD_DATE_URI;
		}
		return resolveUri(base,s);
	}

	public static RdfSchema getProjectSchema(ApplicationContext ctxt, Project project) throws VocabularyIndexException, IOException {
		synchronized (project) {
			RdfSchema rdfSchema = (RdfSchema) project.overlayModels
					.get("rdfSchema");
			if (rdfSchema == null) {
				rdfSchema = new RdfSchema(ctxt,project);

				project.overlayModels.put("rdfSchema", rdfSchema);
				project.getMetadata().updateModified();
			}

			return rdfSchema;
		}
	}

	public static URI buildURI(String uri) {
		try {
			URI baseUri = new URI(uri);
			return baseUri;
		} catch (URISyntaxException e) {
			throw new RuntimeException("malformed Base URI " + uri, e);
		}
	}
	
	public static Object evaluateExpression(Project project, String expression, String columnName, Row row, int rowIndex) throws ParsingException{
		
		Properties bindings = ExpressionUtils.createBindings(project);
        Evaluable eval = MetaParser.parse(expression);
        
        int cellIndex = (columnName==null||columnName.equals(""))?-1:project.columnModel.getColumnByName(columnName).getCellIndex();
        
        return RdfExpressionUtil.evaluate(eval,bindings, row, rowIndex,columnName , cellIndex);
	}
}
