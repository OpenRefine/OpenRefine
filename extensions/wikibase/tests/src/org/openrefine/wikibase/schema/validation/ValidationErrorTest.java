
package org.openrefine.wikibase.schema.validation;

import java.util.Arrays;

import org.openrefine.wikibase.schema.validation.PathElement.Type;
import org.testng.annotations.Test;

import com.google.refine.util.TestUtils;

public class ValidationErrorTest {

    @Test
    public void testSerialize() {
        String expectedJson = "{\n"
                + "       \"message\" : \"Empty date field\",\n"
                + "       \"path\" : [ {\n"
                + "         \"name\" : null,\n"
                + "         \"position\" : 0,\n"
                + "         \"type\" : \"entity\"\n"
                + "       }, {\n"
                + "         \"name\" : \"inception (P571)\",\n"
                + "         \"position\" : -1,\n"
                + "         \"type\" : \"statement\"\n"
                + "       }, {\n"
                + "         \"name\" : null,\n"
                + "         \"position\" : 0,\n"
                + "         \"type\" : \"reference\"\n"
                + "       }, {\n"
                + "         \"name\" : \"retrieved (P813)\",\n"
                + "         \"position\" : -1,\n"
                + "         \"type\" : \"value\"\n"
                + "       } ]\n"
                + "     }";

        ValidationError validation = new ValidationError(Arrays.asList(
                new PathElement(Type.ENTITY, 0),
                new PathElement(Type.STATEMENT, "inception (P571)"),
                new PathElement(Type.REFERENCE, 0),
                new PathElement(Type.VALUE, "retrieved (P813)")),
                "Empty date field");

        TestUtils.isSerializedTo(validation, expectedJson);
    }
}
