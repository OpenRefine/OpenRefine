
package org.openrefine.wikibase.commands;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.commands.Command;
import com.google.refine.util.ParsingUtilities;

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
        ObjectNode jsonObject = ParsingUtilities.mapper.createObjectNode();
        jsonObject.put("code", "error");
        jsonObject.put("message", errorMessage);
        Command.respondJSON(response, jsonObject);
    }
}
