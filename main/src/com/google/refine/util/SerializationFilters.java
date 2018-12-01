package com.google.refine.util;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.refine.model.Recon;
import com.google.refine.model.Recon.Judgment;

/**
 * Series of classes which configure JSON serialization at application level.
 * @author Antonin Delpeuch
 */
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
    
    /**
     * Filter out reconciliation candidates when rendering a matched recon
     * in view mode. (In save mode, render them all the time.)
     */
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
    
    /**
     * Serialize double values as integers if they happen to round to an integer.
     */
    public static class DoubleSerializer extends StdSerializer<Double> {
        private static final long serialVersionUID = 132345L;

        public DoubleSerializer() {
            super(Double.class);
        }

        @Override
        public void serialize(Double arg0, JsonGenerator gen, SerializerProvider s)
                throws IOException {
            if (new Double(arg0.longValue()).equals(arg0)) {
                gen.writeNumber(arg0.longValue());
            } else {
                gen.writeNumber(arg0);
            }
        }
    }
    
    /**
     * Serialize dates by ISO format.
     */
    public static class OffsetDateSerializer extends StdSerializer<OffsetDateTime> {
        private static final long serialVersionUID = 93872874L;

        public OffsetDateSerializer() {
            super(OffsetDateTime.class);
        }

        @Override
        public void serialize(OffsetDateTime arg0, JsonGenerator gen, SerializerProvider s)
                throws IOException {
            gen.writeString(ParsingUtilities.dateToString(arg0));
        }       
    }
    
    /**
     * Serialize dates by ISO format.
     */
    public static class LocalDateSerializer extends StdSerializer<LocalDateTime> {
        private static final long serialVersionUID = 93872874L;

        public LocalDateSerializer() {
            super(LocalDateTime.class);
        }

        @Override
        public void serialize(LocalDateTime arg0, JsonGenerator gen, SerializerProvider s)
                throws IOException {
            gen.writeString(ParsingUtilities.localDateToString(arg0));
        }       
    }
    
    public static class OffsetDateDeserializer extends StdDeserializer<OffsetDateTime> {
        private static final long serialVersionUID = 93872874L;

        public OffsetDateDeserializer() {
            super(OffsetDateTime.class);
        }

		@Override
		public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			return ParsingUtilities.stringToDate(p.getValueAsString());
		}       
    }
    
    public static class LocalDateDeserializer extends StdDeserializer<LocalDateTime> {
        private static final long serialVersionUID = 93872874L;

        public LocalDateDeserializer() {
            super(LocalDateTime.class);
        }

		@Override
		public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			return ParsingUtilities.stringToLocalDate(p.getValueAsString());
		}       
    }
}
