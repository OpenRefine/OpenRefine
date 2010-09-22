package com.google.refine.importers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.marc4j.MarcPermissiveStreamReader;
import org.marc4j.MarcWriter;
import org.marc4j.MarcXmlWriter;
import org.marc4j.marc.Record;

import com.google.refine.ProjectMetadata;
import com.google.refine.model.Project;

public class MarcImporter implements StreamImporter {

    @Override
    public void read(
        InputStream inputStream,
        Project project,
        ProjectMetadata metadata, Properties options
    ) throws ImportException {
        int limit = ImporterUtilities.getIntegerOption("limit",options,-1);
        int skip = ImporterUtilities.getIntegerOption("skip",options,0);

        File tempFile;
        try {
            tempFile = File.createTempFile("gridworks-import-", ".marc.xml");
        } catch (IOException e) {
            throw new ImportException("Unexpected error creating temp file",e);
        }
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
                try {
                    os.close();
                } catch (IOException e) {
                    // Just ignore - not much we can do anyway
                }
            }

            InputStream is = new FileInputStream(tempFile);
            try {
                new XmlImporter().read(is, project, metadata, options);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    // Just ignore - not much we can do anyway
                }
            }
        } catch (FileNotFoundException e) {
            throw new ImportException("Input file not found", e);
        } finally {
            tempFile.delete();
        }
    }

    @Override
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
