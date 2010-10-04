package com.google.refine.importers.parsers;

import java.io.InputStream;

import javax.servlet.ServletException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlParser implements TreeParser{
    final static Logger logger = LoggerFactory.getLogger("XmlParser");
    
    XMLStreamReader parser = null;
    
    public XmlParser(InputStream inputStream){
        try {
            parser = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
        } catch (XMLStreamException e) {
            // silent
            // e.printStackTrace();
        } catch (FactoryConfigurationError e) {
            // silent
            // e.printStackTrace();
        }
    }
    
    @Override
    public TreeParserToken next() throws ServletException{
        try {
            if(!parser.hasNext())
                throw new ServletException("End of XML stream");
        } catch (XMLStreamException e) {
            throw new ServletException(e);
        }
        
        int currentToken = -1;
        try {
            currentToken = parser.next();
        } catch (XMLStreamException e) {
            throw new ServletException(e);
        }
        
        return convertToTreeParserToken(currentToken);
    }
    
    protected TreeParserToken convertToTreeParserToken(int token) throws ServletException {
        switch(token){
            //Xml does not have StartArray element type
            //Xml does not have EndArray element type
            case XMLStreamConstants.START_ELEMENT: return TreeParserToken.StartEntity;
            case XMLStreamConstants.END_ELEMENT: return TreeParserToken.EndEntity;
            case XMLStreamConstants.CHARACTERS: return TreeParserToken.Value;
            case XMLStreamConstants.START_DOCUMENT: return TreeParserToken.StartDocument;
            case XMLStreamConstants.END_DOCUMENT: return TreeParserToken.EndDocument;
            
            //TODO
            default:
                return TreeParserToken.Ignorable;
        }
    }
    
    @Override
    public TreeParserToken getEventType() throws ServletException{
        return this.convertToTreeParserToken(parser.getEventType());
    }
    
    @Override
    public boolean hasNext() throws ServletException{
        try {
            return parser.hasNext();
        } catch (XMLStreamException e) {
            throw new ServletException(e);
        }
    }
    
    @Override
    public String getLocalName() throws ServletException{
        try{
            return parser.getLocalName();
        }catch(IllegalStateException e){
            return null;
        }
    }
    
    @Override
    public String getPrefix(){
        return parser.getPrefix();
    }
    
    @Override
    public String getText(){
        return parser.getText();
    }
    
    @Override
    public int getAttributeCount(){
        return parser.getAttributeCount();
    }
    
    @Override
    public String getAttributeValue(int index){
        return parser.getAttributeValue(index);
    }
    
    @Override
    public String getAttributePrefix(int index){
        return parser.getAttributePrefix(index);
    }
    
    @Override
    public String getAttributeLocalName(int index){
        return parser.getAttributeLocalName(index);
    }
}
