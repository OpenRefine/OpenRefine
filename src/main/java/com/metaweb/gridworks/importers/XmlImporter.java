package com.metaweb.gridworks.importers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.util.Properties;

import org.apache.commons.lang.NotImplementedException;

import com.metaweb.gridworks.importers.XmlImportUtilities.ImportColumnGroup;
import com.metaweb.gridworks.model.Project;

public class XmlImporter implements Importer {

    public static final int BUFFER_SIZE = 64 * 1024;
    
    public boolean takesReader() {
        return false;
    }

    public void read(Reader reader, Project project, Properties options, int skip, int limit)
            throws Exception {
        
        throw new NotImplementedException();
    }

    public void read(
        InputStream inputStream, 
        Project project,
        Properties options, 
        int skip, 
        int limit
    ) throws Exception {
        PushbackInputStream pis = new PushbackInputStream(inputStream,BUFFER_SIZE);
        
        String[] recordPath = null;
        {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytes_read = 0;
            while (bytes_read < BUFFER_SIZE) {
                int c = pis.read(buffer, bytes_read, BUFFER_SIZE - bytes_read);
                if (c == -1) break;
                bytes_read +=c ;
            }
            pis.unread(buffer, 0, bytes_read);
            
            if (options.containsKey("importer-record-tag")) {
                recordPath = XmlImportUtilities.detectPathFromTag(
                        new ByteArrayInputStream(buffer, 0, bytes_read), 
                        options.getProperty("importer-record-tag"));
            } else {
                recordPath = XmlImportUtilities.detectRecordElement(
                        new ByteArrayInputStream(buffer, 0, bytes_read));
            }
        }

        ImportColumnGroup rootColumnGroup = new ImportColumnGroup();
        
        XmlImportUtilities.importXml(pis, project, recordPath, rootColumnGroup);
        XmlImportUtilities.createColumnsFromImport(project, rootColumnGroup);
        
        project.columnModel.update();
    }
    
}
