package com.google.refine.importers.parsers;

import javax.servlet.ServletException;

public interface TreeParser {
    public int next() throws ServletException;
    public int getEventType();
    public boolean hasNext() throws ServletException;
    public String getLocalName();
    public String getPrefix();
}
