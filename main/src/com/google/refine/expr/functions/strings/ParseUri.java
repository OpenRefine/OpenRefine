package com.google.refine.expr.functions.strings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.refine.expr.EvalError;
import com.google.refine.grel.Function;
import com.google.refine.util.ParsingUtilities;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

/**
 * Parse a URI string into its components and returns a JSON object with the following keys:
 * scheme, authority, path, query, fragment, host, port.
 */
public class ParseUri implements Function {
    @Override
    public Object call(Properties bindings, Object[] args) {
        if(args.length == 1 && args[0] instanceof String) {
            String s = (String)args[0];
            try {
                URL url = new URL(s);
                URI uri = url.toURI();

                ObjectMapper objectMapper = new ObjectMapper();
                return ParsingUtilities.mapper.readTree(objectMapper.writeValueAsString(Map.of(
                        "scheme", uri.getScheme() == null ? "" : uri.getScheme(),
                        "host", uri.getHost() == null ? "" : uri.getHost(),
                        "port", String.valueOf(uri.getPort() == -1 ? 80 : uri.getPort()),
                        "path", uri.getPath() == null ? "" : uri.getPath(),
                        "query", uri.getQuery() == null ? "" : uri.getQuery(),
                        "fragment", uri.getFragment() == null ? "" : uri.getFragment(),
                        "authority", uri.getAuthority() == null ? "" : uri.getAuthority()
                )));
            } catch (URISyntaxException | MalformedURLException e) {
                return new EvalError("Invalid URI: " + s);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return new EvalError("ParseUri takes a single string argument.");
    }

    @Override
    public String getDescription() {
        return "Parses a URI and extracts its components.";
    }

    @Override
    public String getParams() {
        return "string url";
    }

    @Override
    public String getReturns() {
        return "JSON object";
    }
}
