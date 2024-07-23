/*
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

package com.google.refine.util;

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

    private Writer m_writer;
    private int m_count = 0;
    private boolean m_indent = true;

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

    public void println(Object o) throws IOException {
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
