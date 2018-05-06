/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package org.openrefine.wikidata.utils;

import java.io.IOException;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.refine.Jsonizable;

/**
 * This class is inefficient because it serializes the object to string and then
 * deserializes it back. Unfortunately, this is the only simple way to bridge
 * Jackson to org.json. This conversion should be removed when (if ?) we migrate
 * OpenRefine a better JSON library.
 * 
 * @author Antonin Delpeuch
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class JacksonJsonizable implements Jsonizable {

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            writer.value(new JSONObject(mapper.writeValueAsString(this)));
        } catch (JsonProcessingException e) {
            throw new JSONException(e.toString());
        }
    }

    public static <T> T fromJSONClass(JSONObject obj, Class<T> klass)
            throws JSONException {
        ObjectMapper mapper = new ObjectMapper();
        String json = obj.toString();
        try {
            return mapper.readValue(json, klass);
        } catch (JsonParseException e) {
            throw new JSONException(e.toString());
        } catch (JsonMappingException e) {
            throw new JSONException(e.toString());
        } catch (IOException e) {
            throw new JSONException(e.toString());
        }
    }

}
