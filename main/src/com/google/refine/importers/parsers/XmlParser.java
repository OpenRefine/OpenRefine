package com.google.refine.importers.parsers;

import java.io.InputStream;

import javax.servlet.ServletException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class XmlParser implements TreeParser{
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
    
    public TreeParserToken next() throws ServletException{
        try {
            if(!parser.hasNext())
                throw new ServletException("End of XML stream");
        } catch (XMLStreamException e) {
            throw new ServletException(e.getMessage());
        }
        
        int currentToken = -1;
        try {
            currentToken = parser.next();
        } catch (XMLStreamException e) {
            throw new ServletException(e.getMessage());
        }
        
        return convertToTreeParserToken(currentToken);
    }
    
    protected TreeParserToken convertToTreeParserToken(int token) throws ServletException {
        switch(token){
            case XMLStreamConstants.START_ELEMENT: return TreeParserToken.StartEntity;
            case XMLStreamConstants.END_ELEMENT: return TreeParserToken.EndEntity;
            case XMLStreamConstants.CHARACTERS: return TreeParserToken.Value;
            case XMLStreamConstants.START_DOCUMENT: return TreeParserToken.StartDocument;
            case XMLStreamConstants.END_DOCUMENT: return TreeParserToken.EndDocument;
            
            //TODO
            default:
                return TreeParserToken.Ignorable;
                //throw new ServletException("Not yet implemented");
        }
    }
    
    public TreeParserToken getEventType() throws ServletException{
        return this.convertToTreeParserToken(parser.getEventType());
    }
    
    public boolean hasNext() throws ServletException{
        try {
            return parser.hasNext();
        } catch (XMLStreamException e) {
            throw new ServletException(e.getMessage());
        }
    }
    
    public String getLocalName(){
        return parser.getLocalName();
    }
    
    public String getPrefix(){
        return parser.getPrefix();
    }
    
    public String getText(){
        return parser.getText();
    }
    
    public int getAttributeCount(){
        return parser.getAttributeCount();
    }
    
    public String getAttributeValue(int index){
        return parser.getAttributeValue(index);
    }
    public String getAttributePrefix(int index){
        return parser.getAttributePrefix(index);
    }
    public String getAttributeLocalName(int index){
        return parser.getAttributeLocalName(index);
    }
}
