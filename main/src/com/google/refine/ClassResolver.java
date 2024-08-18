
package com.google.refine;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;

/**
 * Utility to expose our class name translation logic to the JSON deserializer.
 */
public class ClassResolver extends TypeIdResolverBase {

    private JavaType baseType;

    @Override
    public void init(JavaType baseType) {
        this.baseType = baseType;
    }

    @Override
    public String idFromValue(Object value) {
        return value.getClass().getCanonicalName();
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        return suggestedType.getCanonicalName();
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) throws IOException {
        try {
            return context.constructSpecializedType(baseType, RefineServlet.getClass(id));
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Id getMechanism() {
        return Id.CLASS;
    }

}
