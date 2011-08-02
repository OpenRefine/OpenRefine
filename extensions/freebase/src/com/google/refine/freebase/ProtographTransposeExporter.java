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

package com.google.refine.freebase;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Properties;

import com.google.refine.browsing.Engine;
import com.google.refine.exporters.WriterExporter;
import com.google.refine.freebase.protograph.Protograph;
import com.google.refine.freebase.protograph.transpose.MqlwriteLikeTransposedNodeFactory;
import com.google.refine.freebase.protograph.transpose.TransposedNodeFactory;
import com.google.refine.freebase.protograph.transpose.Transposer;
import com.google.refine.freebase.protograph.transpose.TripleLoaderTransposedNodeFactory;
import com.google.refine.model.Project;

abstract public class ProtographTransposeExporter implements WriterExporter {
    final protected String _contentType;

    public ProtographTransposeExporter(String contentType) {
        _contentType = contentType;
    }

    @Override
    public String getContentType() {
        return "application/x-unknown";
    }
    
    public boolean takeWriter() {
        return true;
    }
    
    public void export(Project project, Properties options, Engine engine,
            OutputStream outputStream) throws IOException {
        throw new RuntimeException("Not implemented");
    }
    
    @Override
    public void export(Project project, Properties options, Engine engine,
            Writer writer) throws IOException {
        
        Protograph protograph = (Protograph) project.overlayModels.get("freebaseProtograph");
        if (protograph != null) {
            TransposedNodeFactory nodeFactory = createNodeFactory(project, writer);
            
            Transposer.transpose(project, engine.getAllFilteredRows(), 
                    protograph, protograph.getRootNode(0), nodeFactory, -1);
            
            nodeFactory.flush();
        }
    }
    
    abstract protected TransposedNodeFactory createNodeFactory(Project project, Writer writer);
    
    static public class TripleLoaderExporter extends ProtographTransposeExporter {
        public TripleLoaderExporter() {
            super("application/x-unknown");
        }

        @Override
        protected TransposedNodeFactory createNodeFactory(Project project, Writer writer) {
            return new TripleLoaderTransposedNodeFactory(project, writer);
        }
    }

    static public class MqlwriteLikeExporter extends ProtographTransposeExporter {
        public MqlwriteLikeExporter() {
            super("application/x-unknown");
        }

        @Override
        protected TransposedNodeFactory createNodeFactory(Project project, Writer writer) {
            return new MqlwriteLikeTransposedNodeFactory(writer);
        }
    }

}
