package com.google.refine.importers.parsers;

import java.io.InputStream;

import javax.servlet.ServletException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
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
    
    public int next() throws ServletException{
        try {
            return parser.next();
        } catch (XMLStreamException e) {
            //TODO log and return 
            throw new ServletException(e.getMessage());
        } 
    }
    
    public int getEventType(){
        return parser.getEventType();
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
}
