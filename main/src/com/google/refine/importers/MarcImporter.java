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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.marc4j.MarcPermissiveStreamReader;
import org.marc4j.MarcWriter;
import org.marc4j.MarcXmlWriter;
import org.marc4j.marc.Record;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingUtilities;
import com.google.refine.util.JSONUtilities;

public class MarcImporter extends XmlImporter {

    public MarcImporter() {
        super();
    }

    @Override
    public ObjectNode createParserUIInitializationData(ImportingJob job, List<ObjectNode> fileRecords, String format) {
        if (fileRecords.size() > 0) {
            ObjectNode firstFileRecord = fileRecords.get(0);
            File file = ImportingUtilities.getFile(job, firstFileRecord);
            File tempFile = new File(file.getAbsolutePath() + ".xml");

            try {
                InputStream inputStream = new FileInputStream(file);
                OutputStream outputStream = new FileOutputStream(tempFile);
                try {
                    MarcWriter writer = new MarcXmlWriter(outputStream, true);

                    MarcPermissiveStreamReader reader = new MarcPermissiveStreamReader(
                            inputStream, true, true);
                    while (reader.hasNext()) {
                        Record record = reader.next();
                        writer.write(record);
                    }
                    writer.close();
                } finally {
                    try {
                        outputStream.close();
                        inputStream.close();

                        if (tempFile.length() == 0) // write failed. Most of time because of wrong Marc format
                            tempFile.delete();
                        else // only set json if write the temp file successfully:
                            JSONUtilities.safePut(firstFileRecord, "location",
                                    JSONUtilities.getString(firstFileRecord, "location", "") + ".xml");

//                        file.delete(); // get rid of our original file
                    } catch (IOException e) {
                        // Just ignore - not much we can do anyway
                    }
                }
            } catch (IOException e) {
                logger.error("Failed to create temporary XML file from MARC file", e);
            }
        }
        ObjectNode options = super.createParserUIInitializationData(job, fileRecords, format);
        return options;
    };

}
