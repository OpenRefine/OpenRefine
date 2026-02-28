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

package com.google.refine.grel;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import com.google.refine.util.ParsingUtilities;

/**
 * Interface for functions. When a function is called, its arguments have already been evaluated down into non-error
 * values.
 */
public interface Function {

    public Object call(Properties bindings, Object[] args);

    @JsonProperty("description")
    default public String getDescription() {
        // TODO: This should be localized (I18N)
        return getStringFromJsonizable(this, "description");
    }

    // TODO: Apparently this is optional in the new design, which seems error prone
    // TODO: We'd probably like this to include type info in a more usable form than a row string to help automate
    // parameter checking
    @JsonProperty("params")
    default public String getParams() {
        return getStringFromJsonizable(this, "params");
    }

    @JsonProperty("returns")
    default public String getReturns() {
        // TODO: in the future, add return type info here to help automate handling
        return getStringFromJsonizable(this, "returns");
    }

    static String getStringFromJsonizable(Object jsonizable, String key) {
        try (StringWriter jsonStringWriter = new StringWriter()) {
            // The legacy interface that Functions and Controls inherited from looked like this:
//            public interface Jsonizable {
//                public void write(org.json.JSONWriter writer, Properties options) throws org.json.JSONException;
//            }
            // If this method doesn't exist, it's not a legacy Function, so there's a different problem
            // We can't include org.json JAR, meaning we don't have direct access to any of the types, so we need to
            // rely on string matching through the Java reflection API

            boolean found = false;
            for (Method method : jsonizable.getClass().getMethods()) {
                // First look for the write(org.json.JSONWriter, Properties) method used by the Jsonizable interface
                if ("write".equals(method.getName()) && "org.json.JSONWriter".equals(method.getParameterTypes()[0].getCanonicalName())) {
                    try {
                        // Now look for the JSONWriter constructor which takes an Appendable, so we can call it with our
                        // StringWriter
                        for (Constructor c : method.getParameterTypes()[0].getConstructors()) {
                            if (c.getParameterCount() == 1 && c.getParameterTypes()[0] == Appendable.class) {
                                // Create an instance of a JSONWriter using our StringWriter
                                Object jsonWriter = c.newInstance(jsonStringWriter);
                                // Invoke the write() method with our new instance
                                method.invoke(jsonizable, jsonWriter, new Properties());
                                found = true;
                                break;
                            }
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        return "Error finding/invoking write() for legacy Function/Control " + jsonizable.getClass().getCanonicalName()
                                + " " + e.getLocalizedMessage();
                    } catch (InstantiationException e) {
                        return "Error instantiating legacy Function/Control " + jsonizable.getClass().getCanonicalName() + " "
                                + e.getLocalizedMessage();
                    }
                    break;
                }
            }
            // If we couldn't find a matching method, it must not have been a legacy Function/Control and something else
            // is going on
            // TODO: Should we complain/error here or just leave silently?
            if (!found) {
                return "";
            }
            JsonNode json = null;
            try {
                // Parse the resulting JSON string using Jackson
                json = ParsingUtilities.mapper.readTree(jsonStringWriter.toString());
            } catch (JsonProcessingException e) {
                return "Error parsing JSON returned from legacy Function/Control: " + e.getLocalizedMessage();
            }
            return json.get(key).asText("Missing " + key + " for legacy Function/Control " + jsonizable.getClass().getCanonicalName());
        } catch (SecurityException e) {
            // FIXME: move this catch to somewhere with a generic error
        } catch (IOException e) {
            return "";
        }
        return "params".equals(key) ? "" : "Missing " + key + " for Function/Control " + jsonizable.getClass().getCanonicalName();
    }

}
