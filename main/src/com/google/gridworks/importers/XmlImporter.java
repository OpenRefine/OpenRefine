package com.google.gridworks.importers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gridworks.importers.XmlImportUtilities.ImportColumnGroup;
import com.google.gridworks.model.Project;

public class XmlImporter implements StreamImporter {

    final static Logger logger = LoggerFactory.getLogger("XmlImporter");

    public static final int BUFFER_SIZE = 64 * 1024;

    @Override
    public void read(
        InputStream inputStream,
        Project project,
        Properties options
    ) throws ImportException {
        logger.trace("XmlImporter.read");
        PushbackInputStream pis = new PushbackInputStream(inputStream,BUFFER_SIZE);

        String[] recordPath = null;
        {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytes_read = 0;
            try {
                while (bytes_read < BUFFER_SIZE) {
                    int c = pis.read(buffer, bytes_read, BUFFER_SIZE - bytes_read);
                    if (c == -1) break;
                    bytes_read +=c ;
                }
                pis.unread(buffer, 0, bytes_read);
            } catch (IOException e) {
                throw new ImportException("Read error",e);
            }
            
            if (options.containsKey("importer-record-tag")) {
                recordPath = XmlImportUtilities.detectPathFromTag(
                        new ByteArrayInputStream(buffer, 0, bytes_read),
                        options.getProperty("importer-record-tag"));
            } else {
                recordPath = XmlImportUtilities.detectRecordElement(
                        new ByteArrayInputStream(buffer, 0, bytes_read));
            }
        }

        if (recordPath == null)
            return;
        
        ImportColumnGroup rootColumnGroup = new ImportColumnGroup();

        XmlImportUtilities.importXml(pis, project, recordPath, rootColumnGroup);
        XmlImportUtilities.createColumnsFromImport(project, rootColumnGroup);
        
        project.columnModel.update();
    }

    @Override
    public boolean canImportData(String contentType, String fileName) {
        if (contentType != null) {
            contentType = contentType.toLowerCase().trim();

            if("application/xml".equals(contentType) ||
                      "text/xml".equals(contentType) ||
                      "application/rss+xml".equals(contentType) ||
                      "application/atom+xml".equals(contentType)) {
                return true;
            }
        } else if (fileName != null) {
            fileName = fileName.toLowerCase();
            if (
                    fileName.endsWith(".xml") ||
                    fileName.endsWith(".atom") ||
                    fileName.endsWith(".rss")
                ) {
                return true;
            }
        }
        return false;
    }

}
