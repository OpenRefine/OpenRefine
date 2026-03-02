
package com.google.refine.commands;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Returns the list of available character encodings.
 */
public class GetEncodingsCommand extends Command {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        SortedMap<String, Charset> charsets = Charset.availableCharsets();
        List<Map<String, Object>> encodings = new ArrayList<>();

        for (Map.Entry<String, Charset> entry : charsets.entrySet()) {
            String code = entry.getKey();
            Charset charset = entry.getValue();

            Map<String, Object> encoding = Map.of(
                "code", code,
                "name", charset.displayName(),
                "aliases", new ArrayList<>(charset.aliases())
            );

            encodings.add(encoding);
        }

        respondJSON(response, encodings);
    }
}
