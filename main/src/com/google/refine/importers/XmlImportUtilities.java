package com.google.refine.importers;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.importers.parsers.TreeParser;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class XmlImportUtilities extends TreeImporter {
    final static Logger logger = LoggerFactory.getLogger("XmlImporterUtilities");

    static public String[] detectPathFromTag(TreeParser parser, String tag) {
        try {
            while (parser.hasNext()) {
                int eventType = parser.next();
                if (eventType == XMLStreamConstants.START_ELEMENT) { //FIXME uses Xml
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
    
    /**
     * Looks for an element with the given tag name in the Xml being parsed, returning the path hierarchy to reach it.
     *
     * @param parser
     * @param tag
     *         The element name (can be qualified) to search for
     * @return
     *         If the tag is found, an array of strings is returned.
     *         If the tag is at the top level, the tag will be the only item in the array.
     *         If the tag is nested beneath the top level, the array is filled with the hierarchy with the tag name at the last index
     *         Null if the the tag is not found.
     * @throws XMLStreamException
     */
    static protected List<String> detectRecordElement(TreeParser parser, String tag) throws ServletException {
        try{
            if(parser.getEventType() == XMLStreamConstants.START_DOCUMENT) //FIXME uses Xml, and is not generic
                parser.next();
        
            String localName = parser.getLocalName();
            String fullName = composeName(parser.getPrefix(), localName);
            if (tag.equals(parser.getLocalName()) || tag.equals(fullName)) {
                List<String> path = new LinkedList<String>();
                path.add(localName);

                return path;
            }

            while (parser.hasNext()) {
                int eventType = parser.next();
                if (eventType == XMLStreamConstants.END_ELEMENT) {  //FIXME uses Xml, and is not generic
                    break;
                } else if (eventType == XMLStreamConstants.START_ELEMENT) { //FIXME uses Xml, and is not generic
                    List<String> path = detectRecordElement(parser, tag);
                    if (path != null) {
                        path.add(0, localName);
                        return path;
                    }
                }
            }
        }catch(ServletException e){
            // silent
            // e.printStackTrace();
        }
        return null;
    }
    
    static protected String composeName(String prefix, String localName) {
        return prefix != null && prefix.length() > 0 ? (prefix + ":" + localName) : localName;
    }
    
    /**
     * Seeks for recurring XML element in an InputStream
     * which are likely candidates for being data records
     * @param inputStream
     *              The XML data as a stream
     * @return
     *              The path to the most numerous of the possible candidates.
     *              null if no candidates were found (less than 6 recurrences)
     */
    static public String[] detectRecordElement(InputStream inputStream) {
        logger.trace("detectRecordElement(inputStream)");
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
        logger.info("No candidate elements were found in Xml - at least 6 similar elements are required");
        return null;
    }

    static protected RecordElementCandidate detectRecordElement(XMLStreamReader parser, String[] path) {
        logger.trace("detectRecordElement(XMLStreamReader, String[])");
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
                logger.trace("ourCandidate.count : " + ourCandidate.count + "; immediateChildCandidates.size() : " + immediateChildCandidates.size());
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
            e.printStackTrace();
            // silent
        }
    }



    /**
     *
     * @param project
     * @param parser
     * @param recordPath
     * @param pathIndex
     * @param rootColumnGroup
     * @throws XMLStreamException
     */
    static protected void findRecord(
        Project project,
        XMLStreamReader parser,
        String[] recordPath,
        int pathIndex,
        ImportColumnGroup rootColumnGroup
    ) throws XMLStreamException {
        if(parser.getEventType() == XMLStreamConstants.START_DOCUMENT){
            logger.warn("Cannot use findRecord method for START_DOCUMENT event");
            return;
        }
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

    /**
     * processRecord parsesXml for a single element and it's sub-elements,
     * adding the parsed data as a row to the project
     * @param project
     * @param parser
     * @param rootColumnGroup
     * @throws XMLStreamException
     */
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
                int cellCount = 0;

                for (int c = 0; c < row.size(); c++) {
                    Cell cell = row.get(c);
                    if (cell != null) {
                        realRow.setCell(c, cell);
                        cellCount++;
                    }
                }
                
                if (cellCount > 0) {
                    project.rows.add(realRow);
                }
            }
        }
    }

    /**
     *
     * @param project
     * @param parser
     * @param columnGroup
     * @param record
     * @throws XMLStreamException
     */
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
        
        thisColumnGroup.nextRowIndex = Math.max(thisColumnGroup.nextRowIndex, columnGroup.nextRowIndex);
        
        int attributeCount = parser.getAttributeCount();
        for (int i = 0; i < attributeCount; i++) {
            String text = parser.getAttributeValue(i).trim();
            if (text.length() > 0) {
                addCell(
                    project,
                    thisColumnGroup,
                    record,
                    composeName(parser.getAttributePrefix(i), parser.getAttributeLocalName(i)),
                    text
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
                        parser.getText()
                    );
                }
            } else if (eventType == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        
        int nextRowIndex = thisColumnGroup.nextRowIndex;
        for (ImportColumn column2 : thisColumnGroup.columns.values()) {
            nextRowIndex = Math.max(nextRowIndex, column2.nextRowIndex);
        }
        for (ImportColumnGroup columnGroup2 : thisColumnGroup.subgroups.values()) {
            nextRowIndex = Math.max(nextRowIndex, columnGroup2.nextRowIndex);
        }
        thisColumnGroup.nextRowIndex = nextRowIndex;
    }



    
}
