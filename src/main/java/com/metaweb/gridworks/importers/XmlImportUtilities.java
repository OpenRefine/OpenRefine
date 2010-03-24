package com.metaweb.gridworks.importers;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.Column;

public class XmlImportUtilities {
    static protected class RecordElementCandidate {
        String[] path;
        int count;
    }
    
    static public class ImportColumnGroup {
        public String name = "";
        
        public Map<String, ImportColumnGroup> subgroups = new HashMap<String, ImportColumnGroup>();
        public Map<String, ImportColumn> columns = new HashMap<String, ImportColumn>();
    }
    
    static public class ImportColumn {
        public int cellIndex;
        public String name;
    }
    
    static public class ImportRecord {
        List<List<Cell>> rows = new LinkedList<List<Cell>>();
        List<Integer> columnEmptyRowIndices = new ArrayList<Integer>();
    }
    
    static public String[] detectRecordElement(InputStream inputStream) {
        List<RecordElementCandidate> candidates = new ArrayList<RecordElementCandidate>();
        
        try {
            XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
            
            while (parser.hasNext()) {
                int eventType = parser.next();
                if (eventType == XMLStreamConstants.START_ELEMENT) {
                    RecordElementCandidate candidate = 
                        detectRecordElement(
                            parser, 
                            new String[] { parser.getLocalName() });
                    
                    if (candidate != null) {
                        candidates.add(candidate);
                    }
                }
            }
        } catch (Exception e) {
            // silent
            // e.printStackTrace();
        }
        
        if (candidates.size() > 0) {
            sortRecordElementCandidates(candidates);
            
            return candidates.get(0).path;
        }
        return null;
    }
    
    static protected RecordElementCandidate detectRecordElement(XMLStreamReader parser, String[] path) {
        List<RecordElementCandidate> candidateList = new ArrayList<RecordElementCandidate>();
        
        Map<String, Integer> candidates = new HashMap<String, Integer>();
        
        try {
            while (parser.hasNext()) {
                int eventType = parser.next();
                if (eventType == XMLStreamConstants.END_ELEMENT) {
                    break;
                } else if (eventType == XMLStreamConstants.START_ELEMENT) {
                    String tagName = parser.getLocalName();
                    
                    candidates.put(tagName, candidates.containsKey(tagName) ? candidates.get(tagName) + 1 : 1);
                    
                    String[] path2 = new String[path.length + 1];
                    System.arraycopy(path, 0, path2, 0, path.length);
                    path2[path.length] = tagName;
                    
                    RecordElementCandidate c = detectRecordElement(parser, path2);
                    if (c != null) {
                        candidateList.add(c);
                    }
                }
            }
        } catch (Exception e) {
            // silent
            // e.printStackTrace();
        }
        
        if (candidates.size() > 0) {
            List<RecordElementCandidate> ourCandidateList = new ArrayList<RecordElementCandidate>(candidates.size());
            for (Entry<String, Integer> entry : candidates.entrySet()) {
                int count = entry.getValue();
                if (count > 1) {
                    String[] path2 = new String[path.length + 1];
                    System.arraycopy(path, 0, path2, 0, path.length);
                    path2[path.length] = entry.getKey();
                    
                    RecordElementCandidate candidate = new RecordElementCandidate();
                    candidate.path = path2;
                    candidate.count = count;
                    ourCandidateList.add(candidate);
                }
            }
            
            if (ourCandidateList.size() > 0) {
                sortRecordElementCandidates(ourCandidateList);
                
                RecordElementCandidate ourCandidate = ourCandidateList.get(0);
                if (ourCandidate.count > 10) {
                    return ourCandidate;
                }
                
                candidateList.add(ourCandidate);
            }
        }
        
        if (candidateList.size() > 0) {
            sortRecordElementCandidates(candidateList);
            
            return candidateList.get(0);
        }
        
        return null;
    }

    static public void sortRecordElementCandidates(List<RecordElementCandidate> list) {
        Collections.sort(list, new Comparator<RecordElementCandidate>() {
            public int compare(RecordElementCandidate o1, RecordElementCandidate o2) {
                return o2.count - o1.count;
            }
        });
    }
    
    static public void importXml(
        InputStream inputStream, 
        Project project, 
        String[] recordPath,
        ImportColumnGroup rootColumnGroup
    ) {
        try {
            XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
            
            while (parser.hasNext()) {
                int eventType = parser.next();
                if (eventType == XMLStreamConstants.START_ELEMENT) {
                    findRecord(project, parser, recordPath, 0, rootColumnGroup);
                }
            }
        } catch (Exception e) {
            // silent
        }
    }
    
    static public void createColumnsFromImport(
        Project project, 
        ImportColumnGroup columnGroup
    ) {
        for (ImportColumn c : columnGroup.columns.values()) {
            Column column = new com.metaweb.gridworks.model.Column(c.cellIndex, c.name);
            project.columnModel.columns.add(column);
        }
        
        for (ImportColumnGroup g : columnGroup.subgroups.values()) {
            createColumnsFromImport(project, g);
        }
    }
    
    static protected void findRecord(
        Project project, 
        XMLStreamReader parser,
        String[] recordPath,
        int pathIndex,
        ImportColumnGroup rootColumnGroup
    ) throws XMLStreamException {
        String tagName = parser.getLocalName();
        if (tagName.equals(recordPath[pathIndex])) {
            if (pathIndex < recordPath.length - 1) {
                while (parser.hasNext()) {
                    int eventType = parser.next();
                    if (eventType == XMLStreamConstants.START_ELEMENT) {
                        findRecord(project, parser, recordPath, pathIndex + 1, rootColumnGroup);
                    } else if (eventType == XMLStreamConstants.END_ELEMENT) {
                        break;
                    }
                }
            } else {
                processRecord(project, parser, rootColumnGroup);
            }
        } else {
            skip(parser);
        }
    }
    
    static protected void skip(XMLStreamReader parser) throws XMLStreamException {
        while (parser.hasNext()) {
            int eventType = parser.next();
            if (eventType == XMLStreamConstants.START_ELEMENT) {
                skip(parser);
            } else if (eventType == XMLStreamConstants.END_ELEMENT) {
                return;
            }
        }
    }
    
    static protected void processRecord(
        Project project, 
        XMLStreamReader parser,
        ImportColumnGroup rootColumnGroup
    ) throws XMLStreamException {
        ImportRecord record = new ImportRecord();
        
        processSubRecord(project, parser, rootColumnGroup, record);
        
        for (List<Cell> row : record.rows) {
            Row realRow = new Row(row.size());
            
            for (int c = 0; c < row.size(); c++) {
                Cell cell = row.get(c);
                if (cell != null) {
                    realRow.setCell(c, cell);
                }
            }
            
            project.rows.add(realRow);
        }
    }
    
    static protected void processSubRecord(
        Project project, 
        XMLStreamReader parser,
        ImportColumnGroup columnGroup,
        ImportRecord record
    ) throws XMLStreamException {
        int commonStartingRowIndex = 0;
        for (ImportColumn column : columnGroup.columns.values()) {
            if (column.cellIndex < record.columnEmptyRowIndices.size()) {
                commonStartingRowIndex = Math.max(
                        commonStartingRowIndex, 
                        record.columnEmptyRowIndices.get(column.cellIndex));
            }
        }
        
        while (parser.hasNext()) {
            int eventType = parser.next();
            if (eventType == XMLStreamConstants.START_ELEMENT) {
                ImportColumnGroup thisColumnGroup = getColumnGroup(project, columnGroup, parser.getLocalName());
                
                int attributeCount = parser.getAttributeCount();
                for (int i = 0; i < attributeCount; i++) {
                    addCell(
                        project, 
                        thisColumnGroup, 
                        record, 
                        parser.getAttributeLocalName(i),
                        parser.getAttributeValue(i),
                        commonStartingRowIndex
                    );
                }
                
                processSubRecord(
                    project,
                    parser,
                    thisColumnGroup,
                    record
                );
            } else if (eventType == XMLStreamConstants.CDATA || 
                        eventType == XMLStreamConstants.CHARACTERS) {
                addCell(
                    project, 
                    columnGroup, 
                    record, 
                    null,
                    parser.getText(),
                    commonStartingRowIndex
                );
            } else if (eventType == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
    }
    
    static protected void addCell(
        Project project,
        ImportColumnGroup columnGroup,
        ImportRecord record,
        String columnLocalName,
        Serializable value,
        int commonStaringRowIndex
    ) {
        if (value == null || (value instanceof String && ((String) value).isEmpty())) {
            return;
        }
        
        ImportColumn column = getColumn(project, columnGroup, columnLocalName);
        int cellIndex = column.cellIndex;
        
        while (cellIndex >= record.columnEmptyRowIndices.size()) {
            record.columnEmptyRowIndices.add(commonStaringRowIndex);
        }
        int rowIndex = record.columnEmptyRowIndices.get(cellIndex);
        
        while (rowIndex >= record.rows.size()) {
            record.rows.add(new ArrayList<Cell>());
        }
        List<Cell> row = record.rows.get(rowIndex);
        
        while (cellIndex >= row.size()) {
            row.add(null);
        }
        
        row.set(cellIndex, new Cell(value, null));
        
        record.columnEmptyRowIndices.set(cellIndex, rowIndex + 1);
    }
    
    static protected ImportColumn getColumn(
        Project project,
        ImportColumnGroup columnGroup,
        String localName
    ) {
        if (columnGroup.columns.containsKey(localName)) {
            return columnGroup.columns.get(localName);
        }
        
        ImportColumn column = createColumn(project, columnGroup, localName);
        columnGroup.columns.put(localName, column);
        
        return column;
    }
    
    static protected ImportColumn createColumn(
        Project project,
        ImportColumnGroup columnGroup,
        String localName
    ) {
        ImportColumn newColumn = new ImportColumn();
        
        newColumn.name = 
            columnGroup.name.length() == 0 ? 
            (localName == null ? "Text" : localName) : 
            (localName == null ? columnGroup.name : (columnGroup.name + " - " + localName));
            
        newColumn.cellIndex = project.columnModel.allocateNewCellIndex();
        
        return newColumn;
    }

    static protected ImportColumnGroup getColumnGroup(
        Project project,
        ImportColumnGroup columnGroup,
        String localName
    ) {
        if (columnGroup.subgroups.containsKey(localName)) {
            return columnGroup.subgroups.get(localName);
        }
        
        ImportColumnGroup subgroup = createColumnGroup(project, columnGroup, localName);
        columnGroup.subgroups.put(localName, subgroup);
        
        return subgroup;
    }
    
    static protected ImportColumnGroup createColumnGroup(
        Project project,
        ImportColumnGroup columnGroup,
        String localName
    ) {
        ImportColumnGroup newGroup = new ImportColumnGroup();
        
        newGroup.name = 
            columnGroup.name.length() == 0 ? 
            (localName == null ? "Text" : localName) : 
            (localName == null ? columnGroup.name : (columnGroup.name + " - " + localName));
        
        return newGroup;
    }
}
