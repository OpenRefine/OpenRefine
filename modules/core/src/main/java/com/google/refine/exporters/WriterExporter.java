/*

Copyright 2010, Google Inc.
Copyright 2024, OpenRefine contributors
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

package com.google.refine.exporters;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Properties;

import com.google.refine.browsing.Engine;
import com.google.refine.model.Project;

// TODO: Do we want to simplify this and only support StreamExporter (stream is more general)
// or would it be general to provide the exporter with the HttpServletResponse from which we're currently getting the stream/writer?
public interface WriterExporter extends Exporter {

    /**
     *
     * @param project
     * @param options
     * @param engine
     * @param writer
     * @throws IOException
     * @deprecated by tfmorris for 3.9 Implement/use {@link #export(Project, Map, Engine, Writer)}
     */
    @Deprecated(since = "3.9")
    default void export(Project project, Properties options, Engine engine, Writer writer) throws IOException {
        // We provide a default implementation for new exporters so that they don't have to implement the legacy
        // interface
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param project
     * @param options
     * @param engine
     * @param writer
     * @throws IOException
     * @since 3.9
     */
    default void export(Project project, Map<String, String> options, Engine engine, Writer writer) throws IOException {
        // Default implementation for modern callers invoking legacy 3rd party extensions
        export(project, Exporter.remapOptions(options), engine, writer);
    }

}
