
package com.google.refine.expr.functions.strings;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.Function;
import com.google.refine.grel.FunctionDescription;
import com.google.refine.util.ParsingUtilities;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Properties;

/**
 * Parse a URI string into its components and returns a JSON object with the following keys: scheme, authority, path,
 * query, fragment, host, port.
 */
public class ParseUri implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1 && args[0] instanceof String) {
            String s = (String) args[0];
            try {
                URL url = new URL(s);
                URI uri = url.toURI();

                // qp represents the query parameters as a single string in the fragment
                String fragment = "", qp = "";
                if (uri.getFragment() != null) { // if there is a fragment
                    fragment = uri.getFragment(); // get the fragment
                    if (fragment.contains("?")) { // if there is a query string
                        // split the fragment into the query string and the fragment
                        String[] parts = fragment.split("\\?");

                        // get the fragment only and query string
                        fragment = parts[0];
                        qp = parts[1];
                    }
                }

                // split the query string into key-value pairs
                ObjectNode queryParamsNode = ParsingUtilities.mapper.createObjectNode();
                if (qp.length() != 0) {
                    // get the query parameters as a list of name-value pairs
                    Arrays.stream(qp.split("&"))
                            .forEach(pair -> queryParamsNode.put(pair.split("=")[0], pair.split("=")[1]));
                }

                ObjectNode uriNode = ParsingUtilities.mapper.createObjectNode();
                uriNode.put("scheme", uri.getScheme());
                uriNode.put("host", uri.getHost());
                uriNode.put("port", uri.getPort());
                uriNode.put("path", uri.getPath());
                uriNode.put("query", uri.getQuery());
                uriNode.put("authority", uri.getAuthority());
                uriNode.put("fragment", fragment);
                uriNode.put("query_params", queryParamsNode);
                return uriNode;

            } catch (URISyntaxException | MalformedURLException e) {
                return new EvalError(EvalErrorMessage.invalid_uri(s));
            }
        }
        return new EvalError(EvalErrorMessage.expects_one_string(ControlFunctionRegistry.getFunctionName(this)));
    }

    @Override
    public String getDescription() {
        return FunctionDescription.str_parse_uri();
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
