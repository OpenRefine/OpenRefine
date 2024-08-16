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
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;

import com.google.refine.browsing.Engine;
import com.google.refine.model.Project;

// TODO: Do we want to simplify this and only support one of WriterExporter / StreamExporter
// This interface is used by OdsExporter and XlsExporter (only)
public interface StreamExporter extends Exporter {

    /**
     *
     * @deprecated for 3.9 by tfmorris - use {@link #export(Project, Map, Engine, OutputStream)}
     */
    @Deprecated(since = "3.9")
    default void export(Project project, Properties options, Engine engine, OutputStream outputStream) throws IOException {
        // We provide a default implementation for new exporters so that they don't have to implement the legacy
        // interface
        throw new UnsupportedOperationException();
    }

    /**
     * Export a project to an OutputStream using the given faceted browsing engines and options.
     *
     * @param project
     *            the project to be exported
     * @param options
     *            option settings to be used for the export
     * @param engine
     *            faceted browsing configuration
     * @param outputStream
     *            the OutputStream to be written to
     * @throws IOException
     * @since 3.9
     */
    default void export(Project project, Map<String, String> options, Engine engine, OutputStream outputStream) throws IOException {
        // Default implementation for modern callers invoking legacy 3rd party extensions
        export(project, Exporter.remapOptions(options), engine, outputStream);
    };

}
