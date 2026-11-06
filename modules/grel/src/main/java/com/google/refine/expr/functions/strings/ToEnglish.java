package com.google.refine.expr.functions.strings;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.Function;

public class ToEnglish implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args == null || args.length != 1 || !(args[0] instanceof String)) {
            return new EvalError(EvalErrorMessage.expects_one_string(ControlFunctionRegistry.getFunctionName(this)));
        }

        String s = (String) args[0];
        String url = "http://127.0.0.1:5000/translate";
        String payload = "{"
                + "\"q\":" + toJsonString(s) + ","
                + "\"source\":\"auto\","
                + "\"target\":\"en\""
                + "}";

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }
            return new EvalError("Translation request failed with status: " + response.statusCode()
                    + " - " + response.body());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return new EvalError("ToEnglish translation failed: " + e.getMessage());
        }
    }

    // Converts a Java string to a quoted, escaped JSON string literal.
    private static String toJsonString(String value) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    @Override
    public String getDescription() {
        return "Converts a string to English.";
    }

    @Override
    public String getParams() {
        return "string s";
    }

    @Override
    public String getReturns() {
        return "string";
    }
}