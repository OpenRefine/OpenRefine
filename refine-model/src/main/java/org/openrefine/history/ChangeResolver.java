
package org.openrefine.history;

import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.fasterxml.jackson.databind.type.TypeFactory;

import org.openrefine.RefineModel;

public class ChangeResolver extends TypeIdResolverBase {

    protected TypeFactory factory = TypeFactory.defaultInstance();

    @Override
    public String idFromValue(Object value) {
        return value.getClass().getName();
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        return suggestedType.getName();
    }

    @Override
    public Id getMechanism() {
        return Id.NAME;
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) {
        Class<?> opClass = null;
        try {
            opClass = RefineModel.getClass(id);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(String.format("Impossible to find change class %s", id));
            // TODO add UnknownChange and adapt History accordingly
        }
        return factory.constructSimpleType(opClass, new JavaType[0]);
    }

}
