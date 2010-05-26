package com.metaweb.gridworks.importers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Properties;

import org.marc4j.MarcPermissiveStreamReader;
import org.marc4j.MarcWriter;
import org.marc4j.MarcXmlWriter;
import org.marc4j.marc.Record;

import com.metaweb.gridworks.model.Project;

public class MarcImporter implements Importer {

    public boolean takesReader() {
        return false;
    }

    public void read(Reader reader, Project project, Properties options)
        throws Exception {

        throw new UnsupportedOperationException();
    }

    public void read(
        InputStream inputStream,
        Project project,
        Properties options
    ) throws Exception {
        int limit = ImporterUtilities.getIntegerOption("limit",options,-1);
        int skip = ImporterUtilities.getIntegerOption("skip",options,0);

        File tempFile = File.createTempFile("gridworks-import-", ".marc.xml");
        try {
            OutputStream os = new FileOutputStream(tempFile);
            try {
                MarcPermissiveStreamReader reader = new MarcPermissiveStreamReader(
                    inputStream,
                    true,
                    true
                );
                MarcWriter writer = new MarcXmlWriter(os, true);

                int count = 0;
                while (reader.hasNext()) {
                    Record record = reader.next();
                    if (skip <= 0) {
                        if (limit == -1 || count < limit) {
                            writer.write(record);
                            count++;
                        } else {
                            break;
                        }
                    } else {
                        skip--;
                    }
                }
                writer.close();
            } finally {
                os.close();
            }

            InputStream is = new FileInputStream(tempFile);
            try {
                new XmlImporter().read(is, project, options);
            } finally {
                is.close();
            }
        } finally {
            tempFile.delete();
        }
    }

    public boolean canImportData(String contentType, String fileName) {
        if (contentType != null) {
            contentType = contentType.toLowerCase().trim();

            if ("application/marc".equals(contentType)) {
                return true;
            }
        } else if (fileName != null) {
            fileName = fileName.toLowerCase();
            if (
                    fileName.endsWith(".mrc") ||
                    fileName.endsWith(".marc") ||
                    fileName.contains(".mrc.") ||
                    fileName.contains(".marc.")
                ) {
                return true;
            }
        }
        return false;
    }
}
