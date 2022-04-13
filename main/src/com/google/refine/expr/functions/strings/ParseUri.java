package com.google.refine.expr.functions.strings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.refine.expr.EvalError;
import com.google.refine.grel.Function;
import com.google.refine.util.ParsingUtilities;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
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

                // qp represents the query parameters as a single string in the fragment
                String fragment = "", qp = "";
                if(uri.getFragment() != null) { // if there is a fragment
                    fragment = uri.getFragment(); // get the fragment
                    if(fragment.contains("?")) { // if there is a query string
                        // split the fragment into the query string and the fragment
                        String[] parts = fragment.split("\\?");

                        // get the fragment only and query string
                        fragment = parts[0]; qp = parts[1];
                    }
                }

                // initial
                Map<String, String> queryParams = new HashMap<>(Map.of());
                if(qp.length() != 0) {
                    // get the query parameters as a list of name-value pairs
                    Arrays.stream(qp.split("&"))
                            .forEach(pair -> queryParams.put(pair.split("=")[0], pair.split("=")[1]));
                }

                return ParsingUtilities.mapper.readTree(ParsingUtilities.mapper.writeValueAsString(Map.of(
                        "scheme", uri.getScheme(),
                        "host", uri.getHost(),
                        "port", String.valueOf(uri.getPort() == -1 ? 80 : uri.getPort()),
                        "path", uri.getPath(),
                        "query", uri.getQuery(),
                        "authority", uri.getAuthority(),
                        "fragment", fragment,
                        "query_params", ParsingUtilities.mapper.writeValueAsString(queryParams)
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
