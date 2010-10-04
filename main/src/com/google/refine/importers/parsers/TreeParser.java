package com.google.refine.importers.parsers;

import javax.servlet.ServletException;

public interface TreeParser {
    public TreeParserToken next() throws ServletException;
    public TreeParserToken getEventType() throws ServletException; //aka getCurrentToken
    public boolean hasNext() throws ServletException;
    public String getLocalName() throws ServletException; //aka getFieldName
    public String getPrefix();
    public String getText() throws ServletException;
    public int getAttributeCount();
    public String getAttributeValue(int index);
    public String getAttributePrefix(int index);
    public String getAttributeLocalName(int index);
}
