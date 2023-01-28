
package org.openrefine.overlay;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class OverlayModelResolver extends TypeIdResolverBase {

    protected TypeFactory factory = TypeFactory.defaultInstance();

    final static protected Map<String, Class<? extends OverlayModel>> s_overlayModelClasses = new HashMap<>();
    final static protected Map<Class<? extends OverlayModel>, String> s_classToId = new HashMap<>();

    static public void registerOverlayModel(String modelName, Class<? extends OverlayModel> klass) {
        s_overlayModelClasses.put(modelName, klass);
        s_classToId.put(klass, modelName);
    }

    @Override
    public String idFromValue(Object value) {
        return s_classToId.get(value.getClass());
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        return s_classToId.get(suggestedType);
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) {
        Class<? extends OverlayModel> opClass = s_overlayModelClasses.get(id);
        if (opClass == null) {
            opClass = UnknownOverlayModel.class;
        }
        return factory.constructSimpleType(opClass, new JavaType[0]);
    }

    @Override
    public Id getMechanism() {
        return Id.NAME;
    }

    /**
     * Exposed for deserialization of legacy projects.
     */
    public static Class<? extends OverlayModel> getClass(String id) {
        return s_overlayModelClasses.get(id);
    }

}
