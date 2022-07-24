/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import java.util.zip.GZIPInputStream;

public class ParsingUtilities {

    public static JsonFactory jsonFactory = new JsonFactory();
    static {
        jsonFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
    }
    public static final ObjectMapper mapper = new ObjectMapper(jsonFactory);
    static {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Double.class, new SerializationFilters.DoubleSerializer());
        module.addSerializer(double.class, new SerializationFilters.DoubleSerializer());
        module.addSerializer(OffsetDateTime.class, new SerializationFilters.OffsetDateSerializer());
        module.addSerializer(LocalDateTime.class, new SerializationFilters.LocalDateSerializer());
        module.addDeserializer(OffsetDateTime.class, new SerializationFilters.OffsetDateDeserializer());
        module.addDeserializer(LocalDateTime.class, new SerializationFilters.LocalDateDeserializer());

        mapper.registerModule(module);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    }

    public static final FilterProvider defaultFilters = new SimpleFilterProvider()
            .addFilter("reconCandidateFilter", SerializationFilters.reconCandidateFilter);
    public static final FilterProvider saveFilters = new SimpleFilterProvider()
            .addFilter("reconCandidateFilter", SerializationFilters.noFilter);

    public static final ObjectWriter saveWriter = mapper.writerWithView(JsonViews.SaveMode.class).with(saveFilters);
    public static final ObjectWriter defaultWriter = mapper.writerWithView(JsonViews.NonSaveMode.class).with(defaultFilters);

    public static final DateTimeFormatter ISO8601 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final ZoneId defaultZone = ZoneId.systemDefault();

    static public Properties parseUrlParameters(HttpServletRequest request) {
        Properties options = new Properties();

        String query = request.getQueryString();
        if (query != null) {
            if (query.startsWith("?")) {
                query = query.substring(1);
            }
            parseParameters(options, query);
        }
        return options;
    }

    static public Properties parseParameters(Properties p, String str) {
        if (str != null) {
            String[] pairs = str.split("&");
            for (String pairString : pairs) {
                int equal = pairString.indexOf('=');
                String name = (equal >= 0) ? pairString.substring(0, equal) : "";
                String value = (equal >= 0) ? ParsingUtilities.decode(pairString.substring(equal + 1)) : "";
                p.put(name, value);
            }
        }
        return p;
    }

    static public Properties parseParameters(String str) {
        return (str == null) ? null : parseParameters(new Properties(), str);
    }

    static public String inputStreamToString(InputStream is) throws IOException {
        return inputStreamToString(is, "UTF-8");
    }

    static public String inputStreamToString(InputStream is, String encoding) throws IOException {
        InputStream uncompressedStream = is;
        // Historical special case only used by tests. Probably can be removed.
        if ("gzip".equals(encoding)) {
            uncompressedStream = new GZIPInputStream(is);
            encoding = "UTF-8";
        }
        return IOUtils.toString(uncompressedStream, encoding);
    }

    private static final URLCodec codec = new URLCodec();

    /**
     * Encode a string as UTF-8.
     */
    static public String encode(String s) {
        try {
            return codec.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return s; // should not happen
        }
    }

    /**
     * Decode a string from UTF-8 encoding.
     */
    static public String decode(String s) {
        try {
            return codec.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return s; // should not happen
        } catch (DecoderException e) {
            return s; // should not happen
        }
    }

    /**
     * Convert a date/time to an ISO_LOCAL_DATE_TIME string
     * 
     * @param d
     *            the date to be written
     * @return string with ISO_LOCAL_DATE_TIME formatted date &amp; time
     */
    static public String dateToString(OffsetDateTime d) {
        return d.format(ISO8601);
    }

    static public String localDateToString(LocalDateTime d) {
        OffsetDateTime odt = OffsetDateTime.of(d,
                OffsetDateTime.now().getOffset());
        // FIXME: A LocalDate has no timezone, by definition.
        return odt.withOffsetSameInstant(ZoneOffset.of("Z")).format(ISO8601);
    }

    /**
     * Parse an ISO_LOCAL_DATE_TIME formatted string into a Java Date. For backward compatibility, to support the
     * version &lt;= 2.8, cannot use the DateTimeFormatter.ISO_OFFSET_DATE_TIME. Instead, use the ISO8601 below format:
     * yyyy-MM-dd'T'HH:mm:ss'Z'
     * 
     * @param s
     *            the string to be parsed
     * @return LocalDateTime or null if the parse failed
     */
    static public OffsetDateTime stringToDate(String s) {
        // Accept timestamps with an explicit time zone
        try {
            return OffsetDateTime.parse(s);
        } catch (DateTimeParseException e) {

        }

        // Also accept timestamps without an explicit zone and
        // assume them to be in local time.
        try {
            LocalDateTime localTime = LocalDateTime.parse(s);
            return OffsetDateTime.of(localTime, ZoneId.systemDefault().getRules().getOffset(localTime));
        } catch (DateTimeParseException e) {

        }
        return null;
    }

    static public LocalDateTime stringToLocalDate(String s) {
        // parse the string as a date and express it in local time
        OffsetDateTime parsed = stringToDate(s);
        if (parsed == null) {
            return null;
        }
        return parsed.withOffsetSameInstant(OffsetDateTime.now().getOffset())
                .toLocalDateTime();
    }

    static public String instantToString(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneId.of("Z")).format(ISO8601);
    }

    static public String instantToLocalDateTimeString(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(ISO8601);
    }

    static public OffsetDateTime calendarToOffsetDateTime(Calendar calendar) {
        return calendar.toInstant().atOffset(ZoneOffset.of("Z"));
    }

    static public Calendar offsetDateTimeToCalendar(OffsetDateTime offsetDateTime) {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("Z"));
        cal.setTimeInMillis(offsetDateTime.toInstant().toEpochMilli());
        return cal;
    }

    public static boolean isDate(Object o) {
        return o instanceof OffsetDateTime;
    }

    /**
     * Converts an old-style Java Date to an OffsetDateTime, assuming the date is represented in the current default
     * system zone (which is what you get if the date was parsed using `Calendar.getDefault()`).
     * 
     * @param date
     * @return
     */
    public static OffsetDateTime toDate(Date date) {
        return date.toInstant().atZone(defaultZone).toOffsetDateTime();
    }

    /**
     * Converts an old-style Java Calendar to an OffsetDateTime, assuming the date is represented in the current default
     * system zone (which is what you get if the date was parsed using `Calendar.getDefault()`).
     * 
     * @param date
     * @return
     */
    public static OffsetDateTime toDate(Calendar date) {
        return date.toInstant().atZone(defaultZone).toOffsetDateTime();
    }

    public static ObjectNode evaluateJsonStringToObjectNode(String optionsString) {
        try {
            JsonNode tree = mapper.readTree(optionsString);
            if (tree instanceof ObjectNode) {
                return (ObjectNode) tree;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ArrayNode evaluateJsonStringToArrayNode(String parameter) {
        try {
            JsonNode tree = mapper.readTree(parameter);
            if (tree instanceof ArrayNode) {
                return (ArrayNode) tree;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
