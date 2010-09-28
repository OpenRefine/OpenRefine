package com.google.refine.importers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.ProjectMetadata;
import com.google.refine.importers.TreeImportUtilities.ImportColumnGroup;
import com.google.refine.importers.parsers.JSONParser;
import com.google.refine.importers.parsers.TreeParser;
import com.google.refine.model.Project;

public class JsonImporter implements StreamImporter{
	final static Logger logger = LoggerFactory.getLogger("XmlImporter");

    public static final int BUFFER_SIZE = 64 * 1024;

	@Override
	public void read(InputStream inputStream, Project project,
			ProjectMetadata metadata, Properties options)
			throws ImportException {
		//FIXME the below is a close duplicate of the XmlImporter code.
		//Should wrap a lot of the below into methods and put them in a common superclass
		logger.trace("JsonImporter.read");
        PushbackInputStream pis = new PushbackInputStream(inputStream,BUFFER_SIZE);

        String[] recordPath = null;
        {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytes_read = 0;
            try {//fill the buffer with data
                while (bytes_read < BUFFER_SIZE) {
                    int c = pis.read(buffer, bytes_read, BUFFER_SIZE - bytes_read);
                    if (c == -1) break;
                    bytes_read +=c ;
                }
                pis.unread(buffer, 0, bytes_read);
            } catch (IOException e) {
                throw new ImportException("Read error",e);
            }

            InputStream iStream = new ByteArrayInputStream(buffer, 0, bytes_read);
            TreeParser parser = new JSONParser(iStream);
            if (options.containsKey("importer-record-tag")) {
                try{
                    recordPath = XmlImportUtilities.detectPathFromTag(
                        parser,
                        options.getProperty("importer-record-tag"));
                }catch(Exception e){
                    // silent
                    // e.printStackTrace();
                }
            } else {
                recordPath = XmlImportUtilities.detectRecordElement(parser);
            }
        }

        if (recordPath == null)
            return;

        ImportColumnGroup rootColumnGroup = new ImportColumnGroup();
        XmlImportUtilities.importTreeData(new JSONParser(pis), project, recordPath, rootColumnGroup);
        XmlImportUtilities.createColumnsFromImport(project, rootColumnGroup);

        project.columnModel.update();

	}

	@Override
	public boolean canImportData(String contentType, String fileName) {
		if (contentType != null) {
            contentType = contentType.toLowerCase().trim();

            if("application/json".equals(contentType) ||
                      "text/json".equals(contentType)) {
                return true;
            }
        } else if (fileName != null) {
            fileName = fileName.toLowerCase();
            if (
                    fileName.endsWith(".json") ||
                    fileName.endsWith(".js")
                ) {
                return true;
            }
        }
        return false;
	}

}
