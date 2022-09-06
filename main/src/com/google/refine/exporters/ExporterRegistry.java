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

package com.google.refine.exporters;

import java.util.HashMap;
import java.util.Map;

import com.google.refine.exporters.sql.SqlExporter;

abstract public class ExporterRegistry {

    static final private Map<String, Exporter> s_formatToExporter = new HashMap<String, Exporter>();

    static {
        s_formatToExporter.put("csv", new CsvExporter());
        s_formatToExporter.put("tsv", new CsvExporter('\t'));
        s_formatToExporter.put("*sv", new CsvExporter());

        s_formatToExporter.put("xls", new XlsExporter(false));
        s_formatToExporter.put("xlsx", new XlsExporter(true));

        s_formatToExporter.put("ods", new OdsExporter());

        s_formatToExporter.put("html", new HtmlTableExporter());

        s_formatToExporter.put("template", new TemplatingExporter());

        s_formatToExporter.put("sql", new SqlExporter());
    }

    static public void registerExporter(String format, Exporter exporter) {
        s_formatToExporter.put(format.toLowerCase(), exporter);
    }

    static public Exporter getExporter(String format) {
        return s_formatToExporter.get(format.toLowerCase());
    }
}
