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
import java.io.PushbackInputStream;
import java.io.Serializable;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.google.common.base.CharMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

public class XmlImporter extends TreeImportingParserBase {

    static final Logger logger = LoggerFactory.getLogger(XmlImporter.class);

    public XmlImporter() {
        super(true);
    }

    static private class PreviewParsingState {

        int tokenCount;
    }

    final static private int PREVIEW_PARSING_LIMIT = 1000;

    @Override
    public ObjectNode createParserUIInitializationData(
            ImportingJob job, List<ObjectNode> fileRecords, String format) {
        ObjectNode options = super.createParserUIInitializationData(job, fileRecords, format);
        try {
            if (fileRecords.size() > 0) {
                ObjectNode firstFileRecord = fileRecords.get(0);
                File file = ImportingUtilities.getFile(job, firstFileRecord);
                InputStream is = new FileInputStream(file);

                try {
                    XMLStreamReader parser = createXMLStreamReader(is);
                    PreviewParsingState state = new PreviewParsingState();

                    while (parser.hasNext() && state.tokenCount < PREVIEW_PARSING_LIMIT) {
                        int tokenType = parser.next();
                        state.tokenCount++;
                        if (tokenType == XMLStreamConstants.START_ELEMENT) {
                            ObjectNode rootElement = descendElement(parser, state);
                            if (rootElement != null) {
                                JSONUtilities.safePut(options, "dom", rootElement);
                                break;
                            }
                        } else {
                            // ignore everything else
                        }
                    }
                } catch (XMLStreamException e) {
                    logger.warn("Error generating parser UI initialization data for XML file", e);
                } finally {
                    is.close();
                }
            }
        } catch (IOException e) {
            logger.error("Error generating parser UI initialization data for XML file", e);
        }

        return options;
    }

    final static private ObjectNode descendElement(XMLStreamReader parser, PreviewParsingState state) {
        ObjectNode result = ParsingUtilities.mapper.createObjectNode();
        {
            String name = parser.getLocalName();
            JSONUtilities.safePut(result, "n", name);

            String prefix = parser.getPrefix();
            if (prefix != null) {
                JSONUtilities.safePut(result, "p", prefix);
            }
            String nsUri = parser.getNamespaceURI();
            if (nsUri != null) {
                JSONUtilities.safePut(result, "uri", nsUri);
            }
        }

        int namespaceCount = parser.getNamespaceCount();
        if (namespaceCount > 0) {
            ArrayNode namespaces = result.putArray("ns");

            for (int i = 0; i < namespaceCount; i++) {
                ObjectNode namespace = ParsingUtilities.mapper.createObjectNode();
                namespaces.add(namespace);
                JSONUtilities.safePut(namespace, "p", parser.getNamespacePrefix(i));
                JSONUtilities.safePut(namespace, "uri", parser.getNamespaceURI(i));
            }
        }

        int attributeCount = parser.getAttributeCount();
        if (attributeCount > 0) {
            ArrayNode attributes = result.putArray("a");

            for (int i = 0; i < attributeCount; i++) {
                ObjectNode attribute = ParsingUtilities.mapper.createObjectNode();
                attributes.add(attribute);
                JSONUtilities.safePut(attribute, "n", parser.getAttributeLocalName(i));
                JSONUtilities.safePut(attribute, "v", parser.getAttributeValue(i));
                String prefix = parser.getAttributePrefix(i);
                if (prefix != null) {
                    JSONUtilities.safePut(attribute, "p", prefix);
                }
            }
        }

        ArrayNode children = ParsingUtilities.mapper.createArrayNode();
        try {
            while (parser.hasNext() && state.tokenCount < PREVIEW_PARSING_LIMIT) {
                int tokenType = parser.next();
                state.tokenCount++;
                if (tokenType == XMLStreamConstants.END_ELEMENT) {
                    break;
                } else if (tokenType == XMLStreamConstants.START_ELEMENT) {
                    ObjectNode childElement = descendElement(parser, state);
                    if (childElement != null) {
                        children.add(childElement);
                    }
                } else if (tokenType == XMLStreamConstants.CHARACTERS ||
                        tokenType == XMLStreamConstants.CDATA ||
                        tokenType == XMLStreamConstants.SPACE) {
                    ObjectNode childElement = ParsingUtilities.mapper.createObjectNode();
                    JSONUtilities.safePut(childElement, "t", parser.getText());
                    children.add(childElement);
                } else {
                    // ignore everything else
                }
            }
        } catch (XMLStreamException e) {
            logger.error("Error generating parser UI initialization data for XML file", e);
        }

        if (children.size() > 0) {
            result.set("c", children);
        }
        return result;
    }

    @Override
    public void parseOneFile(Project project, ProjectMetadata metadata,
            ImportingJob job, String fileSource, InputStream inputStream,
            ImportColumnGroup rootColumnGroup, int limit, ObjectNode options,
            List<Exception> exceptions) {

        try {
            parseOneFile(project, metadata, job, fileSource,
                    new XmlParser(inputStream), rootColumnGroup, limit, options, exceptions);
        } catch (XMLStreamException e) {
            exceptions.add(e);
        } catch (IOException e) {
            exceptions.add(e);
        }
    }

    static public class XmlParser implements TreeReader {

        final protected XMLStreamReader parser;
        static final int WHITESPACE_CHARACTERS_TOKEN = 15;

        public XmlParser(InputStream inputStream) throws XMLStreamException, IOException {
            parser = createXMLStreamReader(inputStream);
        }

        @Override
        public Token next() throws TreeReaderException {
            try {
                if (!parser.hasNext()) {
                    throw new TreeReaderException("End of XML stream");
                }
            } catch (XMLStreamException e) {
                throw new TreeReaderException(e);
            }

            int currentToken = -1;
            try {
                currentToken = parser.next();
            } catch (XMLStreamException e) {
                throw new TreeReaderException(e);
            }
            // Issue #1095 : Preventing addition of empty cells containing whitespaces in the table
            // Whitespaces between tags will be parsed as Characters by default
            // Updates the token if the text value is a whitespace
            if (currentToken == XMLStreamConstants.CHARACTERS) {
                String text = parser.getText();
                if (!text.isEmpty() && CharMatcher.whitespace().matchesAllOf(text)) {
                    currentToken = WHITESPACE_CHARACTERS_TOKEN;
                }
            }
            return mapToToken(currentToken);
        }

        protected Token mapToToken(int token) {
            switch (token) {
                case XMLStreamConstants.START_ELEMENT:
                    return Token.StartEntity;
                case XMLStreamConstants.END_ELEMENT:
                    return Token.EndEntity;
                case XMLStreamConstants.CHARACTERS:
                    return Token.Value;
                case XMLStreamConstants.START_DOCUMENT:
                    return Token.Ignorable;
                case XMLStreamConstants.END_DOCUMENT:
                    return Token.Ignorable;
                case XMLStreamConstants.SPACE:
                    return Token.Value;
                case XMLStreamConstants.PROCESSING_INSTRUCTION:
                    return Token.Ignorable;
                case XMLStreamConstants.NOTATION_DECLARATION:
                    return Token.Ignorable;
                case XMLStreamConstants.NAMESPACE:
                    return Token.Ignorable;
                case XMLStreamConstants.ENTITY_REFERENCE:
                    return Token.Ignorable;
                case XMLStreamConstants.DTD:
                    return Token.Ignorable;
                case XMLStreamConstants.COMMENT:
                    return Token.Ignorable;
                case XMLStreamConstants.CDATA:
                    return Token.Ignorable;
                case XMLStreamConstants.ATTRIBUTE:
                    return Token.Ignorable;
                case WHITESPACE_CHARACTERS_TOKEN:
                    return Token.Ignorable;
                default:
                    return Token.Ignorable;
            }
        }

        @Override
        public Token current() throws TreeReaderException {
            return this.mapToToken(parser.getEventType());
        }

        @Override
        public boolean hasNext() throws TreeReaderException {
            try {
                return parser.hasNext();
            } catch (XMLStreamException e) {
                throw new TreeReaderException(e);
            }
        }

        @Override
        public String getFieldName() throws TreeReaderException {
            try {
                return parser.getLocalName();
            } catch (IllegalStateException e) {
                return null;
            }
        }

        @Override
        public String getPrefix() {
            return parser.getPrefix();
        }

        @Override
        public String getFieldValue() {
            return parser.getText();
        }

        @Override
        public Serializable getValue() {
            // XML parser only does string types
            return getFieldValue();
        }

        @Override
        public int getAttributeCount() {
            return parser.getAttributeCount();
        }

        @Override
        public String getAttributeValue(int index) {
            return parser.getAttributeValue(index);
        }

        @Override
        public String getAttributePrefix(int index) {
            return parser.getAttributePrefix(index);
        }

        @Override
        public String getAttributeLocalName(int index) {
            return parser.getAttributeLocalName(index);
        }
    }

    final static private XMLStreamReader createXMLStreamReader(InputStream inputStream) throws XMLStreamException, IOException {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_COALESCING, true);
        factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, true);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        return factory.createXMLStreamReader(wrapPrefixRemovingInputStream(inputStream));
    }

    final static private InputStream wrapPrefixRemovingInputStream(InputStream inputStream)
            throws XMLStreamException, IOException {
        PushbackInputStream pis = new PushbackInputStream(inputStream);
        int b;
        int count = 0;
        while (count < 100 && (b = pis.read()) >= 0) {
            if (++count > 100) {
                throw new XMLStreamException(
                        "File starts with too much non-XML content to skip over");
            } else if (b == '<') {
                pis.unread(b);
                break;
            }
        }
        return pis;
    }
}
