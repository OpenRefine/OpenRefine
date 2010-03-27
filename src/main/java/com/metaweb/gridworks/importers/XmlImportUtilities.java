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
import com.metaweb.gridworks.model.ColumnGroup;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.Column;

public class XmlImportUtilities {
    static protected class RecordElementCandidate {
        String[] path;
        int count;
    }
    
    static protected abstract class ImportVertical {
        public String name = "";
        public int nonBlankCount;
        
        abstract void tabulate();
    }
    
    static public class ImportColumnGroup extends ImportVertical {
        public Map<String, ImportColumnGroup> subgroups = new HashMap<String, ImportColumnGroup>();
        public Map<String, ImportColumn> columns = new HashMap<String, ImportColumn>();
        
        @Override
        void tabulate() {
            for (ImportColumn c : columns.values()) {
                c.tabulate();
                nonBlankCount = Math.max(nonBlankCount, c.nonBlankCount);
            }
            for (ImportColumnGroup g : subgroups.values()) {
                g.tabulate();
                nonBlankCount = Math.max(nonBlankCount, g.nonBlankCount);
            }
        }
    }
    
    static public class ImportColumn extends ImportVertical {
        public int cellIndex;
        public boolean blankOnFirstRow;

        @Override
        void tabulate() {
            // already done the tabulation elsewhere
        }
    }
    
    static public class ImportRecord {
        List<List<Cell>> rows = new LinkedList<List<Cell>>();
        List<Integer> columnEmptyRowIndices = new ArrayList<Integer>();
    }
    
    static public String[] detectPathFromTag(InputStream inputStream, String tag) {
        List<RecordElementCandidate> candidates = new ArrayList<RecordElementCandidate>();
        
        try {
            XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
            
            while (parser.hasNext()) {
                int eventType = parser.next();
                if (eventType == XMLStreamConstants.START_ELEMENT) {
                    List<String> path = detectRecordElement(parser, tag);
                    if (path != null) {
                        String[] path2 = new String[path.size()];
                        
                        path.toArray(path2);
                        
                        return path2;
                    }
                }
            }
        } catch (Exception e) {
            // silent
            // e.printStackTrace();
        }
        
        return null;
    }
    
    static protected List<String> detectRecordElement(XMLStreamReader parser, String tag) throws XMLStreamException {
        String localName = parser.getLocalName();
        String fullName = composeName(parser.getPrefix(), localName);
        if (tag.equals(parser.getLocalName()) || tag.equals(fullName)) {
            List<String> path = new LinkedList<String>();
            path.add(localName);
            
            return path;
        }
        
        while (parser.hasNext()) {
            int eventType = parser.next();
            if (eventType == XMLStreamConstants.END_ELEMENT) {
                break;
            } else if (eventType == XMLStreamConstants.START_ELEMENT) {
                List<String> path = detectRecordElement(parser, tag);
                if (path != null) {
                    path.add(0, localName);
                    return path;
                }
            }
        }
        return null;
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
        List<RecordElementCandidate> descendantCandidates = new ArrayList<RecordElementCandidate>();
        
        Map<String, Integer> immediateChildCandidateMap = new HashMap<String, Integer>();
        int textNodeCount = 0;
        int childElementNodeCount = 0;
        
        try {
            while (parser.hasNext()) {
                int eventType = parser.next();
                if (eventType == XMLStreamConstants.END_ELEMENT) {
                    break;
                } else if (eventType == XMLStreamConstants.CHARACTERS) {
                    if (parser.getText().trim().length() > 0) {
                        textNodeCount++;
                    }
                } else if (eventType == XMLStreamConstants.START_ELEMENT) {
                    childElementNodeCount++;
                    
                    String tagName = parser.getLocalName();
                    
                    immediateChildCandidateMap.put(
                        tagName, 
                        immediateChildCandidateMap.containsKey(tagName) ? 
                                immediateChildCandidateMap.get(tagName) + 1 : 1);
                    
                    String[] path2 = new String[path.length + 1];
                    System.arraycopy(path, 0, path2, 0, path.length);
                    path2[path.length] = tagName;
                    
                    RecordElementCandidate c = detectRecordElement(parser, path2);
                    if (c != null) {
                        descendantCandidates.add(c);
                    }
                }
            }
        } catch (Exception e) {
            // silent
            // e.printStackTrace();
        }
        
        if (textNodeCount > 0 && childElementNodeCount > 0) {
            // This is a mixed element
            return null;
        }
        
        if (immediateChildCandidateMap.size() > 0) {
            List<RecordElementCandidate> immediateChildCandidates = new ArrayList<RecordElementCandidate>(immediateChildCandidateMap.size());
            for (Entry<String, Integer> entry : immediateChildCandidateMap.entrySet()) {
                int count = entry.getValue();
                if (count > 1) {
                    String[] path2 = new String[path.length + 1];
                    System.arraycopy(path, 0, path2, 0, path.length);
                    path2[path.length] = entry.getKey();
                    
                    RecordElementCandidate candidate = new RecordElementCandidate();
                    candidate.path = path2;
                    candidate.count = count;
                    immediateChildCandidates.add(candidate);
                }
            }
            
            if (immediateChildCandidates.size() > 0 && immediateChildCandidates.size() < 5) {
                // There are some promising immediate child elements, but not many, 
                // that can serve as record elements.
                
                sortRecordElementCandidates(immediateChildCandidates);
                
                RecordElementCandidate ourCandidate = immediateChildCandidates.get(0);
                if (ourCandidate.count / immediateChildCandidates.size() > 5) {
                    return ourCandidate;
                }
                
                descendantCandidates.add(ourCandidate);
            }
        }
        
        if (descendantCandidates.size() > 0) {
            sortRecordElementCandidates(descendantCandidates);
            
            RecordElementCandidate candidate = descendantCandidates.get(0);
            if (candidate.count / descendantCandidates.size() > 5) {
                return candidate;
            }
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
        int startColumnIndex = project.columnModel.columns.size();
        
        List<ImportColumn> columns = new ArrayList<ImportColumn>(columnGroup.columns.values());
        Collections.sort(columns, new Comparator<ImportColumn>() {
            public int compare(ImportColumn o1, ImportColumn o2) {
                if (o1.blankOnFirstRow != o2.blankOnFirstRow) {
                    return o1.blankOnFirstRow ? 1 : -1;
                }
                
                int c = o2.nonBlankCount - o1.nonBlankCount;
                return c != 0 ? c : (o1.name.length() - o2.name.length());
            }
        });
        
        for (int i = 0; i < columns.size(); i++) {
            ImportColumn c = columns.get(i);
            
            Column column = new com.metaweb.gridworks.model.Column(c.cellIndex, c.name);
            project.columnModel.columns.add(column);
        }
        
        List<ImportColumnGroup> subgroups = new ArrayList<ImportColumnGroup>(columnGroup.subgroups.values());
        Collections.sort(subgroups, new Comparator<ImportColumnGroup>() {
            public int compare(ImportColumnGroup o1, ImportColumnGroup o2) {
                int c = o2.nonBlankCount - o1.nonBlankCount;
                return c != 0 ? c : (o1.name.length() - o2.name.length());
            }
        });
        
        for (ImportColumnGroup g : subgroups) {
            createColumnsFromImport(project, g);
        }
        
        int endColumnIndex = project.columnModel.columns.size();
        int span = endColumnIndex - startColumnIndex;
        if (span > 1 && span < project.columnModel.columns.size()) {
            project.columnModel.addColumnGroup(startColumnIndex, span, startColumnIndex);
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
        
        if (record.rows.size() > 0) {
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
    }
    
    static protected String composeName(String prefix, String localName) {
        return prefix != null && prefix.length() > 0 ? (prefix + ":" + localName) : localName;
    }
    
    static protected void processSubRecord(
        Project project, 
        XMLStreamReader parser,
        ImportColumnGroup columnGroup,
        ImportRecord record
    ) throws XMLStreamException {
        ImportColumnGroup thisColumnGroup = getColumnGroup(
                project, 
                columnGroup, 
                composeName(parser.getPrefix(), parser.getLocalName()));
        
        int commonStartingRowIndex = 0;
        for (ImportColumn column : thisColumnGroup.columns.values()) {
            if (column.cellIndex < record.columnEmptyRowIndices.size()) {
                commonStartingRowIndex = Math.max(
                        commonStartingRowIndex, 
                        record.columnEmptyRowIndices.get(column.cellIndex));
            }
        }
        
        int attributeCount = parser.getAttributeCount();
        for (int i = 0; i < attributeCount; i++) {
            String text = parser.getAttributeValue(i).trim();
            if (text.length() > 0) {
                addCell(
                    project, 
                    thisColumnGroup, 
                    record, 
                    composeName(parser.getAttributePrefix(i), parser.getAttributeLocalName(i)),
                    text,
                    commonStartingRowIndex
                );
            }
        }
        
        while (parser.hasNext()) {
            int eventType = parser.next();
            if (eventType == XMLStreamConstants.START_ELEMENT) {
                processSubRecord(
                    project,
                    parser,
                    thisColumnGroup,
                    record
                );
            } else if (//eventType == XMLStreamConstants.CDATA || 
                        eventType == XMLStreamConstants.CHARACTERS) {
                String text = parser.getText().trim();
                if (text.length() > 0) {
                    addCell(
                        project, 
                        thisColumnGroup, 
                        record, 
                        null,
                        parser.getText(),
                        commonStartingRowIndex
                    );
                }
            } else if (eventType == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        
        if (commonStartingRowIndex < record.rows.size()) {
            List<Cell> startingRow = record.rows.get(commonStartingRowIndex);
            
            for (ImportColumn c : thisColumnGroup.columns.values()) {
                int cellIndex = c.cellIndex;
                if (cellIndex >= startingRow.size() || startingRow.get(cellIndex) == null) {
                    c.blankOnFirstRow = true;
                }
            }
        }
    }
    
    static protected void addCell(
        Project project,
        ImportColumnGroup columnGroup,
        ImportRecord record,
        String columnLocalName,
        String text,
        int commonStaringRowIndex
    ) {
        if (text == null || ((String) text).isEmpty()) {
            return;
        }
        
        Serializable value = ImporterUtilities.parseCellValue(text);
        
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
        
        column.nonBlankCount++;
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
