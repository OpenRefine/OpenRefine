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

package com.google.refine.importers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.json.JSONObject;
import org.marc4j.MarcPermissiveStreamReader;
import org.marc4j.MarcWriter;
import org.marc4j.MarcXmlWriter;
import org.marc4j.marc.Record;

import com.google.refine.ProjectMetadata;
import com.google.refine.importers.tree.ImportColumnGroup;
import com.google.refine.importing.ImportingJob;
import com.google.refine.model.Project;

public class MarcImporter extends XmlImporter {
    @Override
    public void parseOneFile(Project project, ProjectMetadata metadata,
            ImportingJob job, String fileSource, InputStream inputStream,
            ImportColumnGroup rootColumnGroup, int limit, JSONObject options,
            List<Exception> exceptions) {
        
        File tempFile;
        try {
            tempFile = File.createTempFile("refine-import-", ".marc.xml");
        } catch (IOException e) {
            exceptions.add(new ImportException("Unexpected error creating temp file", e));
            return;
        }
        
        try {
            OutputStream os = new FileOutputStream(tempFile);
            try {
                MarcWriter writer = new MarcXmlWriter(os, true);
                
                MarcPermissiveStreamReader reader = new MarcPermissiveStreamReader(
                    inputStream, true, true);
                while (reader.hasNext()) {
                    Record record = reader.next();
                    writer.write(record);
                }
                writer.close();
            } finally {
                try {
                    os.close();
                } catch (IOException e) {
                    // Just ignore - not much we can do anyway
                }
            }

            InputStream is = new FileInputStream(tempFile);
            try {
                super.parseOneFile(project, metadata, job, fileSource, inputStream,
                        rootColumnGroup, limit, options, exceptions);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    // Just ignore - not much we can do anyway
                }
            }
        } catch (FileNotFoundException e) {
            exceptions.add(new ImportException("Input file not found", e));
            return;
        } finally {
            tempFile.delete();
        }
    }
}
