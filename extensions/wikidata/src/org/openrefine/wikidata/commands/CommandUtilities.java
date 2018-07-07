package org.openrefine.wikidata.commands;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONWriter;

public class CommandUtilities {
    
    /**
     * Helper introduced to ease returning error messages from a response. Curiously
     * this is not part of {@link Command}: the respond method uses the "status" JSON
     * key instead of the "code" one required by the JS code.
     * 
     * @param response
     * @param errorMessage
     * @throws IOException 
     */
    public static void respondError(HttpServletResponse response, String errorMessage)
            throws IOException {
        Writer w = response.getWriter();
        JSONWriter writer = new JSONWriter(w);
        writer.object();
        writer.key("code"); writer.value("error");
        writer.key("message"); writer.value(errorMessage);
        writer.endObject();
        w.flush();
        w.close();
    }
}
