package com.metaweb.gridworks.importers;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream; 
import java.io.Reader;
import java.util.Properties;

import org.apache.commons.lang.NotImplementedException;

import com.metaweb.gridworks.importers.XmlImportUtilities.ImportColumnGroup;
import com.metaweb.gridworks.model.Project;

public class XmlImporter implements Importer {

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
        BufferedInputStream bis = new BufferedInputStream(inputStream);
        
        String[] recordPath = null;
        {
            byte[] buffer = new byte[64 * 1024];
            
            bis.mark(buffer.length);
            int c = bis.read(buffer);
            bis.reset();
            
            if (options.containsKey("importer-record-tag")) {
                recordPath = XmlImportUtilities.detectPathFromTag(
                        new ByteArrayInputStream(buffer, 0, c), 
                        options.getProperty("importer-record-tag"));
            } else {
                recordPath = XmlImportUtilities.detectRecordElement(
                        new ByteArrayInputStream(buffer, 0, c));
            }
        }
        
        ImportColumnGroup rootColumnGroup = new ImportColumnGroup();
        
        XmlImportUtilities.importXml(bis, project, recordPath, rootColumnGroup);
        XmlImportUtilities.createColumnsFromImport(project, rootColumnGroup);
        
        project.columnModel.update();
    }
    
}
