package com.metaweb.gridworks.rdf.expr.functions.strings;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;
import com.metaweb.gridworks.gel.Function;

public class Urlify implements Function {

	public Object call(Properties bindings, Object[] args) {
		if(args.length==1 || args.length==2){
			String s = args[0].toString();
			s = s.replaceAll("\\s+", "_");
			if(args.length==2){
				String base = args[1].toString();
				URI base_uri;
				try {
					base_uri = new URI(base);
					return base_uri.resolve(s).toString();
				} catch (URISyntaxException e) {
					return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " unable to encode");
				}
			}
			try {
				return URLEncoder.encode(s, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " unable to encode");
			}
		}
		return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects 1 string");
	}

	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		writer.object();
        writer.key("description"); writer.value("replaces spaces with underscore");
        writer.key("params"); writer.value("string s");
        writer.key("returns"); writer.value("string");
        writer.endObject();
		
	}

}
