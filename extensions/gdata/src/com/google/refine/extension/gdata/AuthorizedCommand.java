
package com.google.refine.extension.gdata;

import java.io.IOException;
import java.util.Base64;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.mit.simile.butterfly.ButterflyModule;
import org.apache.velocity.VelocityContext;

import com.google.refine.commands.Command;
import com.google.refine.util.ParsingUtilities;

public class AuthorizedCommand extends Command {

    Pattern callbackPattern = Pattern.compile("^cb[0-9]+$");
    Pattern winnamePattern = Pattern.compile("^openrefine[0-9]+$");

    ButterflyModule module;

    public AuthorizedCommand(ButterflyModule module) {
        this.module = module;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        VelocityContext velocityContext = new VelocityContext();
        String state = request.getParameter("state");
        if (state == null) {
            respond(response, "error", "No 'state' parameter provided");
            return;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(state);
            JsonNode parsed = ParsingUtilities.mapper.readTree(decoded);
            if (parsed instanceof ObjectNode && parsed.has("winname") && parsed.has("cb")) {
                ObjectNode object = (ObjectNode) parsed;
                String cb = object.get("cb").asText();
                String winname = object.get("winname").asText();
                if (!winnamePattern.matcher(winname).find()) {
                    respond(response, "error", "Invalid winname provided");
                    return;
                }
                if (!callbackPattern.matcher(cb).find()) {
                    respond(response, "error", "Invalid callback provided");
                    return;
                }
                velocityContext.internalPut("winname", winname);
                velocityContext.internalPut("cb", cb);

            } else {
                throw new IllegalArgumentException("expected a JSON object");
            }
        } catch (IllegalArgumentException | IOException e) {
            respond(response, "error", "Invalid 'state' parameter provided");
            return;
        }

        updateToken(request, response);

        try {
            module.sendTextFromTemplate(request, response, velocityContext, "authorized.vt", "UTF-8", "text/html", false);
        } catch (Exception e) {
            respondException(response, e);
        }
    }

    private void updateToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (TokenCookie.getToken(request) != null) {
            return;
        }
        var tokenAndExpiresInSeconds = GoogleAPIExtension.getTokenFromCode(module, request);
        if (tokenAndExpiresInSeconds != null) {
            var tokenInfo = tokenAndExpiresInSeconds.split(",");
            TokenCookie.setToken(request, response, tokenInfo[0], tokenInfo[1]);
            return;
        }
        TokenCookie.deleteToken(request, response);
    }

}
