
package org.openrefine.wikibase.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.commands.Command;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;
import org.apache.commons.lang.Validate;
import org.openrefine.wikibase.operations.SaveWikibaseSchemaOperation;
import org.openrefine.wikibase.schema.WikibaseSchema;
import org.openrefine.wikibase.schema.validation.ValidationState;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.openrefine.wikibase.commands.CommandUtilities.respondError;

/**
 * Command to check whether a given schema template is syntactically valid. This uses POST because schema templates can
 * be large, but it is not making any changes to the application state, so we are not CSRF-protecting this one.
 */
public class ParseWikibaseSchemaCommand extends Command {

    protected static class WikibaseSchemaTemplate {

        // the name of the template, displayed in the UI
        @JsonProperty("name")
        String name;
        // a potentially incomplete schema
        @JsonProperty("schema")
        WikibaseSchema schema;

        @JsonCreator
        WikibaseSchemaTemplate(
                @JsonProperty("name") String name,
                @JsonProperty("schema") WikibaseSchema schema) {
            Validate.notNull(name);
            Validate.notEmpty(name);
            Validate.notNull(schema);
            this.name = name;
            this.schema = schema;
        }

    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String jsonString = request.getParameter("template");
            if (jsonString == null || "null".equals(jsonString)) {
                respondError(response, "No Wikibase schema template provided.");
                return;
            }

            try {
                WikibaseSchemaTemplate schema = ParsingUtilities.mapper.readValue(jsonString, WikibaseSchemaTemplate.class);
                // not running validation on the schema, because we are only checking for
                // its syntactic validity as a template, and this is already checked by the parsing above
                ObjectNode jsonResponse = ParsingUtilities.mapper.createObjectNode();
                jsonResponse.put("code", "ok");
                jsonResponse.put("object_type", "template");
                jsonResponse.put("message", "Valid schema template");
                respondJSON(response, jsonResponse);
            } catch (IOException e) {
                // if it is not a schema template, it might just be a schema (without a name).
                // This is also accepted, just for the sake of being able to import schemas exported
                // prior to OpenRefine 3.7 (which did not come with the wrapping object storing the name).
                WikibaseSchema schema = ParsingUtilities.mapper.readValue(jsonString, WikibaseSchema.class);

                ObjectNode jsonResponse = ParsingUtilities.mapper.createObjectNode();
                jsonResponse.put("code", "ok");
                jsonResponse.put("object_type", "schema");
                jsonResponse.put("message", "Valid schema");
                respondJSON(response, jsonResponse);
            }

        } catch (IOException e) {
            // we did not recognize the JSON as that of a schema template or schema
            response.setStatus(401);
            respondError(response, e.getMessage());
        } catch (Exception e) {
            // This is an unexpected exception, so we log it.
            response.setStatus(500);
            respondException(response, e);
        }
    }
}
