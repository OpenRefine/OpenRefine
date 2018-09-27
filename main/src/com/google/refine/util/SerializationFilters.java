package com.google.refine.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;

import com.google.refine.model.Recon;
import com.google.refine.model.Recon.Judgment;

public class SerializationFilters {
    static class BaseFilter extends SimpleBeanPropertyFilter {
        @Override
        public void serializeAsField(Object obj, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer)
          throws Exception {
           if (include(writer)) {
              writer.serializeAsField(obj, jgen, provider);
           } else if (!jgen.canOmitFields()) {
              writer.serializeAsOmittedField(obj, jgen, provider);
           }
        }
        
        @Override
        protected boolean include(BeanPropertyWriter writer) {
           return true;
        }
        
        @Override
        protected boolean include(PropertyWriter writer) {
           return true;
        }
    }
    
    public static PropertyFilter noFilter = new BaseFilter();
    public static PropertyFilter reconCandidateFilter = new BaseFilter() {
        @Override
        public void serializeAsField(Object obj, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer)
          throws Exception {
           if (include(writer)) {
              if (!writer.getName().equals("c") || ! (obj instanceof Recon)) {
                 writer.serializeAsField(obj, jgen, provider);
                 return;
              }
              Recon recon = (Recon)obj;
              if (recon.judgment == Judgment.None) {
                 writer.serializeAsField(obj, jgen, provider);
              }
           } else if (!jgen.canOmitFields()) {
              writer.serializeAsOmittedField(obj, jgen, provider);
           }
        }
     };
}
