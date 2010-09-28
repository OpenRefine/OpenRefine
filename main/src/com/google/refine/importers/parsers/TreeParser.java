package com.google.refine.importers.parsers;

import javax.servlet.ServletException;

public interface TreeParser {
    public TreeParserToken next() throws ServletException;
    public int getEventType(); //aka getCurrentToken
    public boolean hasNext() throws ServletException;
    public String getLocalName();
    public String getPrefix();
    public String getText();
    public int getAttributeCount();
    public String getAttributeValue(int index);
    public String getAttributePrefix(int index);
    public String getAttributeLocalName(int index);
}
