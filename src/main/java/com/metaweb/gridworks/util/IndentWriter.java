/*
 * Created on Dec 1, 2005
 * Created by dfhuynh
 */
package com.metaweb.gridworks.util;

import java.io.IOException;
import java.io.Writer;

/**
 * A utility for writing indented code.
 * 
 * @author dfhuynh
 */
public class IndentWriter {
    final static private int s_max = 20;
    
    static private String[] s_indents = new String[s_max];
    static {
        for (int i = 0; i < s_max; i++) {
            StringBuffer sb = new StringBuffer(s_max);
            for (int j = 0; j < i; j++) {
                sb.append('\t');
            }
            s_indents[i] = sb.toString();
        }
    }
    
    private Writer        m_writer;
    private int         m_count = 0;
    private boolean     m_indent = true;
    
    public IndentWriter(Writer writer) {
        m_writer = writer;
    }
    
    public void close() throws IOException {
        m_writer.close();
    }
    
    public void flush() throws IOException {
        m_writer.flush();
    }
    
    public void print(Object o) throws IOException {
        printIndent();
        m_writer.write(o.toString());
        m_indent = false;
    }
    
    public void println() throws IOException {
        printIndent();
        m_writer.write("\n");
        m_indent = true;
    }
    
    public void println(Object o) throws IOException  {
        printIndent();
        m_writer.write(o.toString());
        m_writer.write("\n");
        m_indent = true;
    }
    
    public void indent() {
        m_count++;
    }
    
    public void unindent() {
        m_count--;
    }
    
    private void printIndent() throws IOException {
        if (m_indent) {
            m_writer.write(s_indents[m_count]);
        }
    }
}
