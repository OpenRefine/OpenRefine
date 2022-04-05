package com.google.refine.expr.functions.strings;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.Function;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

public class ParseUri implements Function {
    @Override
    public Object call(Properties bindings, Object[] args) {
        if(args.length == 1 && args[0] instanceof String) {
            String s = (String)args[0];
            try {
                URL url = new URL(s);
                URI uri = url.toURI();

                return Map.of(
                        "scheme", uri.getScheme() == null ? "" : uri.getScheme(),
                        "host", uri.getHost() == null ? "" : uri.getHost(),
                        "port", String.valueOf(uri.getPort()),
                        "path", uri.getPath() == null ? "" : uri.getPath(),
                        "query", uri.getQuery() == null ? "" : uri.getQuery(),
                        "fragment", uri.getFragment() == null ? "" : uri.getFragment(),
                        "authority", uri.getAuthority() == null ? "" : uri.getAuthority()
                );
            } catch (URISyntaxException | MalformedURLException e) {
                return new EvalError("Invalid URI: " + s);
            }
        }
        return new EvalError("ParseUri takes a single string argument");
    }

    @Override
    public String getDescription() {
        return "Parses a URI and extracts its components";
    }

    @Override
    public String getReturns() {
        return "Map m";
    }
}
