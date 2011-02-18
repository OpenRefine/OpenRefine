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

package com.google.refine.importers.parsers;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSONParser implements TreeParser{
    final static Logger logger = LoggerFactory.getLogger("JsonParser");
    
    JsonFactory factory = new JsonFactory();
    JsonParser parser = null;
    
    //The following is a workaround for inconsistent Jackson JsonParser
    Boolean lastTokenWasAFieldNameAndCurrentTokenIsANewEntity = false;
    Boolean thisTokenIsAFieldName = false;
    String lastFieldName = null;
    //end of workaround

    public JSONParser(InputStream inputStream){
        try {
            parser = factory.createJsonParser(inputStream);
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
    public TreeParserToken getEventType() throws ServletException {
        return this.mapToTreeParserToken(parser.getCurrentToken());
    }

    @Override
    public String getLocalName() throws ServletException{
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
    public String getText() throws ServletException {
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
    public TreeParserToken next() throws ServletException {
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
            
        return mapToTreeParserToken(next);
    }
    
    protected TreeParserToken mapToTreeParserToken(JsonToken token){
        switch(token){
            case START_ARRAY: return TreeParserToken.StartEntity;
            case END_ARRAY: return TreeParserToken.EndEntity;
            case START_OBJECT: return TreeParserToken.StartEntity;
            case END_OBJECT: return TreeParserToken.EndEntity;
            case VALUE_STRING: return TreeParserToken.Value;
            case FIELD_NAME: return TreeParserToken.Ignorable; //returned by the getLocalName function()
            case VALUE_NUMBER_INT: return TreeParserToken.Value;
            //Json does not have START_DOCUMENT token type (so ignored as default)
            //Json does not have END_DOCUMENT token type (so ignored as default)
            case VALUE_TRUE : return TreeParserToken.Value;
            case VALUE_NUMBER_FLOAT : return TreeParserToken.Value;
            case VALUE_NULL : return TreeParserToken.Value;
            case VALUE_FALSE : return TreeParserToken.Value;
            case VALUE_EMBEDDED_OBJECT : return TreeParserToken.Ignorable;
            case NOT_AVAILABLE : return TreeParserToken.Ignorable;
            default: return TreeParserToken.Ignorable;
        }
    }

}
