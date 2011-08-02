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

package com.google.refine.importers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;

import javax.servlet.ServletException;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.ProjectMetadata;
import com.google.refine.importers.tree.ImportColumnGroup;
import com.google.refine.importers.tree.TreeImportingParserBase;
import com.google.refine.importers.tree.TreeReader;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingUtilities;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;

public class JsonImporter extends TreeImportingParserBase {
    public JsonImporter() {
        super(false);
    }
    
    static private class PreviewParsingState {
        int tokenCount;
    }
    
    final static private int PREVIEW_PARSING_LIMIT = 1000;
    
    @Override
    public JSONObject createParserUIInitializationData(
            ImportingJob job, List<JSONObject> fileRecords, String format) {
        JSONObject options = super.createParserUIInitializationData(job, fileRecords, format);
        try {
            JSONObject firstFileRecord = fileRecords.get(0);
            File file = ImportingUtilities.getFile(job, firstFileRecord);
            InputStream is = new FileInputStream(file);
            try {
                JsonFactory factory = new JsonFactory();
                JsonParser parser = factory.createJsonParser(is);
                
                PreviewParsingState state = new PreviewParsingState();
                Object rootValue = parseForPreview(parser, state);
                if (rootValue != null) {
                    JSONUtilities.safePut(options, "dom", rootValue);
                }
            } finally {
                is.close();
            }
        } catch (IOException e) {
            // Ignore
        }

        return options;
    }
    
    final static private Object parseForPreview(JsonParser parser, PreviewParsingState state, JsonToken token)
            throws JsonParseException, IOException {
        if (token != null) {
            switch (token) {
            case START_ARRAY:
                return parseArrayForPreview(parser, state);
            case START_OBJECT:
                return parseObjectForPreview(parser, state);
            case VALUE_STRING:
                return parser.getText();
            case VALUE_NUMBER_INT:
                return Integer.valueOf(parser.getIntValue());
            case VALUE_NUMBER_FLOAT:
                return Float.valueOf(parser.getFloatValue());
            case VALUE_TRUE:
                return Boolean.TRUE;
            case VALUE_FALSE:
                return Boolean.FALSE;
            case VALUE_NULL:
                return null;
            }
        }
        return null;
    }
    
    final static private Object parseForPreview(JsonParser parser, PreviewParsingState state) {
        try {
            JsonToken token = parser.nextToken();
            state.tokenCount++;
            return parseForPreview(parser, state, token);
        } catch (Exception e) {
            return null;
        }
    }
    
    final static private JSONObject parseObjectForPreview(JsonParser parser, PreviewParsingState state) {
        JSONObject result = new JSONObject();
        loop:while (state.tokenCount < PREVIEW_PARSING_LIMIT) {
            try {
                JsonToken token = parser.nextToken();
                if (token == null) {
                    break;
                }
                state.tokenCount++;
                
                switch (token) {
                case FIELD_NAME:
                    String fieldName = parser.getText();
                    Object fieldValue = parseForPreview(parser, state);
                    JSONUtilities.safePut(result, fieldName, fieldValue);
                    break;
                case END_OBJECT:
                    break loop;
                default:
                    break loop;
                }
            } catch (Exception e) {
                break;
            }
        }
        return result;
    }
    
    final static private JSONArray parseArrayForPreview(JsonParser parser, PreviewParsingState state) {
        JSONArray result = new JSONArray();
        loop:while (state.tokenCount < PREVIEW_PARSING_LIMIT) {
            try {
                JsonToken token = parser.nextToken();
                if (token == null) {
                    break;
                }
                state.tokenCount++;
                
                switch (token) {
                case END_ARRAY:
                    break loop;
                default:
                    Object element = parseForPreview(parser, state, token);
                    JSONUtilities.append(result, element);
                }
            } catch (Exception e) {
                break;
            }
        }
        return result;
    }
    
    @Override
    public void parseOneFile(Project project, ProjectMetadata metadata,
            ImportingJob job, String fileSource, Reader reader,
            ImportColumnGroup rootColumnGroup, int limit, JSONObject options, List<Exception> exceptions) {
        
        parseOneFile(project, metadata, job, fileSource,
            new JSONTreeReader(reader), rootColumnGroup, limit, options, exceptions);
    }
    
    static public class JSONTreeReader implements TreeReader {
        final static Logger logger = LoggerFactory.getLogger("JsonParser");
        
        JsonFactory factory = new JsonFactory();
        JsonParser parser = null;
        
        //The following is a workaround for inconsistent Jackson JsonParser
        Boolean lastTokenWasAFieldNameAndCurrentTokenIsANewEntity = false;
        Boolean thisTokenIsAFieldName = false;
        String lastFieldName = null;
        //end of workaround
        
        public JSONTreeReader(Reader reader) {
            try {
                parser = factory.createJsonParser(reader);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        /**
         * Does nothing. All Json is treated as elements
         */
        @Override
        public int getAttributeCount() {
            // TODO Auto-generated method stub
            return 0;
        }
        
        /**
         * Does nothing. All Json is treated as elements
         */
        @Override
        public String getAttributeLocalName(int index) {
            return null;
        }

        /**
         * Does nothing. All Json is treated as elements
         */
        @Override
        public String getAttributePrefix(int index) {
            // TODO Auto-generated method stub
            return null;
        }

        /**
         * Does nothing. All Json is treated as elements
         */
        @Override
        public String getAttributeValue(int index) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Token current() throws ServletException {
            return this.mapToToken(parser.getCurrentToken());
        }

        @Override
        public String getFieldName() throws ServletException{
            try {
                String text = parser.getCurrentName();
                
                //The following is a workaround for inconsistent Jackson JsonParser
                if(text == null){
                    if(this.lastTokenWasAFieldNameAndCurrentTokenIsANewEntity) 
                        text = this.lastFieldName;
                    else
                        text = "__anonymous__";
                }
                //end of workaround
                
                return text;
            } catch (Exception e) {
                throw new ServletException(e);
            }
        }

        /**
         * Does nothing. Json does not have prefixes
         */
        @Override
        public String getPrefix() {
            return null;
        }

        @Override
        public String getFieldValue() throws ServletException {
            try {
                return parser.getText();
            } catch (Exception e) {
                throw new ServletException(e);
            }
        }

        @Override
        public boolean hasNext() throws ServletException {
            return true; //FIXME fairly obtuse, is there a better way (advancing, then rewinding?)
        }

        @Override
        public Token next() throws ServletException {
            JsonToken next;
            try {
                next = parser.nextToken();
            } catch (JsonParseException e) {
                throw new ServletException(e);
            } catch (IOException e) {
                throw new ServletException(e);
            }
            
            if(next == null)
                throw new ServletException("No more Json Tokens in stream");
            
            //The following is a workaround for inconsistent Jackson JsonParser
            if(next == JsonToken.FIELD_NAME){
                try {
                    this.thisTokenIsAFieldName = true;
                    this.lastFieldName = parser.getCurrentName();
                } catch (Exception e) {
                    //silent
                }
            }else if(next == JsonToken.START_ARRAY || next == JsonToken.START_OBJECT){
                if(this.thisTokenIsAFieldName){
                    this.lastTokenWasAFieldNameAndCurrentTokenIsANewEntity = true;
                    this.thisTokenIsAFieldName = false;
                }else{
                    this.lastTokenWasAFieldNameAndCurrentTokenIsANewEntity = false;
                    this.lastFieldName = null;
                }
            }else{
                this.lastTokenWasAFieldNameAndCurrentTokenIsANewEntity = false;
                this.lastFieldName = null;
                this.thisTokenIsAFieldName = false;
            }
            //end of workaround
                
            return mapToToken(next);
        }
        
        protected Token mapToToken(JsonToken token){
            switch(token){
                case START_ARRAY: return Token.StartEntity;
                case END_ARRAY: return Token.EndEntity;
                case START_OBJECT: return Token.StartEntity;
                case END_OBJECT: return Token.EndEntity;
                case VALUE_STRING: return Token.Value;
                case FIELD_NAME: return Token.Ignorable; //returned by the getLocalName function()
                case VALUE_NUMBER_INT: return Token.Value;
                //Json does not have START_DOCUMENT token type (so ignored as default)
                //Json does not have END_DOCUMENT token type (so ignored as default)
                case VALUE_TRUE : return Token.Value;
                case VALUE_NUMBER_FLOAT : return Token.Value;
                case VALUE_NULL : return Token.Value;
                case VALUE_FALSE : return Token.Value;
                case VALUE_EMBEDDED_OBJECT : return Token.Ignorable;
                case NOT_AVAILABLE : return Token.Ignorable;
                default: return Token.Ignorable;
            }
        }
    }
}
