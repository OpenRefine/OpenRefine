/*

Copyright 2010,2012 Google Inc.
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
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.refine.ProjectMetadata;
import com.google.refine.importers.tree.ImportColumnGroup;
import com.google.refine.importers.tree.TreeImportingParserBase;
import com.google.refine.importers.tree.TreeReader;
import com.google.refine.importers.tree.TreeReaderException;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingUtilities;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

public class JsonImporter extends TreeImportingParserBase {

    static final Logger logger = LoggerFactory.getLogger(JsonImporter.class);

    public final static String ANONYMOUS = "_";

    public JsonImporter() {
        super(true);
    }

    static private class PreviewParsingState {

        int tokenCount;
    }

    final static private int PREVIEW_PARSING_LIMIT = 1000;

    @Override
    public ObjectNode createParserUIInitializationData(ImportingJob job,
            List<ObjectNode> fileRecords, String format) {
        ObjectNode options = super.createParserUIInitializationData(job, fileRecords, format);
        if (fileRecords.size() > 0) {
            try {
                ObjectNode firstFileRecord = fileRecords.get(0);
                File file = ImportingUtilities.getFile(job, firstFileRecord);
                JsonFactory factory = new JsonFactory();
                JsonParser parser = factory.createParser(file);
                parser.enable(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS);

                PreviewParsingState state = new PreviewParsingState();
                JsonNode rootValue = parseForPreview(parser, state);
                if (rootValue != null) {
                    JSONUtilities.safePut(options, "dom", rootValue);
                }
            } catch (IOException e) {
                logger.error("Error generating parser UI initialization data for JSON file", e);
            }
        }

        return options;
    }

    final static private JsonNode parseForPreview(JsonParser parser, PreviewParsingState state, JsonToken token)
            throws JsonParseException, IOException {
        if (token != null) {
            switch (token) {
                case START_ARRAY:
                    return parseArrayForPreview(parser, state);
                case START_OBJECT:
                    return parseObjectForPreview(parser, state);
                case VALUE_STRING:
                    return new TextNode(parser.getText());
                case VALUE_NUMBER_INT:
                    return new LongNode(parser.getLongValue());
                case VALUE_NUMBER_FLOAT:
                    return new DoubleNode(parser.getDoubleValue());
                case VALUE_TRUE:
                    return BooleanNode.getTrue();
                case VALUE_FALSE:
                    return BooleanNode.getFalse();
                case VALUE_NULL:
                    return null;
                case END_ARRAY:
                case END_OBJECT:
                case FIELD_NAME:
                case NOT_AVAILABLE:
                case VALUE_EMBEDDED_OBJECT:
                default:
                    break;
            }
        }
        return null;
    }

    final static private JsonNode parseForPreview(JsonParser parser, PreviewParsingState state) {
        try {
            JsonToken token = parser.nextToken();
            state.tokenCount++;
            return parseForPreview(parser, state, token);
        } catch (IOException e) {
            return null;
        }
    }

    final static private ObjectNode parseObjectForPreview(JsonParser parser, PreviewParsingState state) {
        ObjectNode result = ParsingUtilities.mapper.createObjectNode();
        loop: while (state.tokenCount < PREVIEW_PARSING_LIMIT) {
            try {
                JsonToken token = parser.nextToken();
                if (token == null) {
                    break;
                }
                state.tokenCount++;

                switch (token) {
                    case FIELD_NAME:
                        String fieldName = parser.getText();
                        JsonNode fieldValue = parseForPreview(parser, state);
                        JSONUtilities.safePut(result, fieldName, fieldValue);
                        break;
                    case END_OBJECT:
                        break loop;
                    default:
                        break loop;
                }
            } catch (IOException e) {
                break;
            }
        }
        return result;
    }

    final static private ArrayNode parseArrayForPreview(JsonParser parser, PreviewParsingState state) {
        ArrayNode result = ParsingUtilities.mapper.createArrayNode();
        loop: while (state.tokenCount < PREVIEW_PARSING_LIMIT) {
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
                        JsonNode element = parseForPreview(parser, state, token);
                        result.add(element);
                }
            } catch (IOException e) {
                break;
            }
        }
        return result;
    }

    @Override
    public void parseOneFile(Project project, ProjectMetadata metadata,
            ImportingJob job, String fileSource, InputStream is,
            ImportColumnGroup rootColumnGroup, int limit, ObjectNode options, List<Exception> exceptions) {

        parseOneFile(project, metadata, job, fileSource,
                new JSONTreeReader(is), rootColumnGroup, limit, options, exceptions);
    }

    static public class JSONTreeReader implements TreeReader {

        final static Logger logger = LoggerFactory.getLogger("JsonParser");

        JsonFactory factory = new JsonFactory();
        JsonParser parser = null;

        private JsonToken current = null;
        private JsonToken next = null;
        private String fieldName = ANONYMOUS;
        private Serializable fieldValue = null;

        public JSONTreeReader(InputStream is) {
            try {
                parser = factory.createParser(is);
                parser.enable(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS);
                current = null;
                next = parser.nextToken();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Does nothing. All Json is treated as elements
         */
        @Override
        public int getAttributeCount() {
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
            return null;
        }

        /**
         * Does nothing. All Json is treated as elements
         */
        @Override
        public String getAttributeValue(int index) {
            return null;
        }

        @Override
        public Token current() {
            if (current != null) {
                return this.mapToToken(current);
            } else {
                return null;
            }
        }

        @Override
        public String getFieldName() throws TreeReaderException {
            return fieldName;
        }

        /**
         * Does nothing. Json does not have prefixes
         */
        @Override
        public String getPrefix() {
            return null;
        }

        @Override
        public String getFieldValue() throws TreeReaderException {
            return fieldValue.toString();
        }

        @Override
        public Serializable getValue()
                throws TreeReaderException {
            return fieldValue;
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        private Serializable getValue(JsonParser parser, JsonToken token) throws IOException {
            if (token != null) {
                switch (token) {
                    case VALUE_STRING:
                        return parser.getText();
                    case VALUE_NUMBER_INT:
                        if (parser.getNumberType() == NumberType.INT || parser.getNumberType() == NumberType.LONG) {
                            return Long.valueOf(parser.getLongValue());
                        } else {
                            return parser.getNumberValue();
                        }
                    case VALUE_NUMBER_FLOAT:
                        if (parser.getNumberType() == NumberType.FLOAT) {
                            return Float.valueOf(parser.getFloatValue());
                        } else if (parser.getNumberType() == NumberType.DOUBLE) {
                            return Double.valueOf(parser.getDoubleValue());
                        } else {
                            return parser.getNumberValue();
                        }
                    case VALUE_TRUE:
                        return Boolean.TRUE;
                    case VALUE_FALSE:
                        return Boolean.FALSE;
                    case VALUE_NULL:
                        return null;
                    case END_ARRAY:
                    default:
                        break;
                }
            }
            return null;
        }

        @Override
        public Token next() throws TreeReaderException {
            JsonToken previous = current;
            current = next;
            next = null; // in case an exception is thrown
            try {
                if (current != null) {
                    if (current.isScalarValue()) {
                        fieldValue = getValue(parser, current);
                    } else {
                        fieldValue = null;
                    }
                    if (current == JsonToken.FIELD_NAME) {
                        fieldName = parser.getText();
                    } else if (current == JsonToken.START_ARRAY
                            || current == JsonToken.START_OBJECT) {
                        // Use current field name for next level object
                        // ie elide one level of anonymous fields
                        if (previous != JsonToken.FIELD_NAME) {
                            fieldName = ANONYMOUS;
                        }
                    }
                }
                next = parser.nextToken();
            } catch (JsonParseException e) {
                throw new TreeReaderException(e.getOriginalMessage());
            } catch (IOException e) {
                throw new TreeReaderException(e);
            }
            return current();
        }

        protected Token mapToToken(JsonToken token) {
            switch (token) {
                case START_ARRAY:
                    return Token.StartEntity;
                case END_ARRAY:
                    return Token.EndEntity;
                case START_OBJECT:
                    return Token.StartEntity;
                case END_OBJECT:
                    return Token.EndEntity;
                case VALUE_STRING:
                    return Token.Value;
                case FIELD_NAME:
                    return Token.Ignorable; // returned by the getLocalName function()
                case VALUE_NUMBER_INT:
                    return Token.Value;
                // Json does not have START_DOCUMENT token type (so ignored as default)
                // Json does not have END_DOCUMENT token type (so ignored as default)
                case VALUE_TRUE:
                    return Token.Value;
                case VALUE_NUMBER_FLOAT:
                    return Token.Value;
                case VALUE_NULL:
                    return Token.Value;
                case VALUE_FALSE:
                    return Token.Value;
                case VALUE_EMBEDDED_OBJECT:
                    return Token.Ignorable;
                case NOT_AVAILABLE:
                    return Token.Ignorable;
                default:
                    return Token.Ignorable;
            }
        }
    }
}
