
package org.openrefine.wikibase.commands;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonGenerator;

import org.openrefine.commands.Command;
import org.openrefine.util.ParsingUtilities;

public class CommandUtilities {

    /**
     * Helper introduced to ease returning error messages from a response. Curiously this is not part of
     * {@link Command}: the respond method uses the "status" JSON key instead of the "code" one required by the JS code.
     * 
     * @param response
     * @param errorMessage
     * @throws IOException
     */
    public static void respondError(HttpServletResponse response, String errorMessage)
            throws IOException {
        Writer w = response.getWriter();
        JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);
        writer.writeStartObject();
        writer.writeStringField("code", "error");
        writer.writeStringField("message", errorMessage);
        writer.writeEndObject();
        writer.flush();
        writer.close();
        w.flush();
        w.close();
    }
}
