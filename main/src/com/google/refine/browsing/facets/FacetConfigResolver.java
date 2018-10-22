package com.google.refine.browsing.facets;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.fasterxml.jackson.databind.type.TypeFactory;

import com.google.refine.model.recon.ReconConfig;

public class FacetConfigResolver extends TypeIdResolverBase {
    
    protected TypeFactory factory = TypeFactory.defaultInstance();

    @Override
    public Id getMechanism() {
        return Id.NAME;
    }

    @Override
    public String idFromValue(Object instance) {
        return ((ReconConfig)instance).getMode();
    }

    @Override
    public String idFromValueAndType(Object instance, Class<?> type) {
        return ReconConfig.s_opClassToName.get(type);
    }
    
    @Override
    public JavaType typeFromId(DatabindContext context, String id) throws IOException {
        return factory.constructSimpleType(ReconConfig.getClassFromMode(id), new JavaType[0]);
    }
}
