package com.google.refine.tests.importers;

import java.util.List;

import javax.servlet.ServletException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.google.refine.importers.XmlImportUtilities;
import com.google.refine.importers.parsers.TreeParser;
import com.google.refine.model.Project;

public class XmlImportUtilitiesStub extends XmlImportUtilities {
    
    public List<String> detectRecordElementWrapper(TreeParser parser, String tag) throws ServletException{
        return super.detectRecordElement(parser, tag);
    }

    public void ProcessSubRecordWrapper(Project project, XMLStreamReader parser, ImportColumnGroup columnGroup, ImportRecord record) throws XMLStreamException{
        super.processSubRecord(project, parser, columnGroup, record);
    }

    public void findRecordWrapper(Project project, XMLStreamReader parser, String[] recordPath, int pathIndex, ImportColumnGroup rootColumnGroup) throws XMLStreamException{
        super.findRecord(project, parser, recordPath, pathIndex, rootColumnGroup);
    }

    public void processRecordWrapper(Project project, XMLStreamReader parser, ImportColumnGroup rootColumnGroup) throws XMLStreamException{
        super.processRecord(project, parser, rootColumnGroup);
    }

    public void addCellWrapper(Project project, ImportColumnGroup columnGroup, ImportRecord record, String columnLocalName, String text, int commonStartingRowIndex) {
        super.addCell(project, columnGroup, record, columnLocalName, text);
    }
}
