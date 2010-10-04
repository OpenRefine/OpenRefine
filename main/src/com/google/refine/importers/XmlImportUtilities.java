package com.google.refine.importers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.importers.parsers.TreeParser;
import com.google.refine.importers.parsers.TreeParserToken;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class XmlImportUtilities extends TreeImportUtilities {
    final static Logger logger = LoggerFactory.getLogger("XmlImportUtilities");

    static public String[] detectPathFromTag(TreeParser parser, String tag) {
        try {
            while (parser.hasNext()) {
                TreeParserToken eventType = parser.next();
                if (eventType == TreeParserToken.StartEntity) {//XMLStreamConstants.START_ELEMENT) {
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
     * Looks for an element with the given tag name in the Tree data being parsed, returning the path hierarchy to reach it.
     *
     * @param parser
     * @param tag
     *         The element name (can be qualified) to search for
     * @return
     *         If the tag is found, an array of strings is returned.
     *         If the tag is at the top level, the tag will be the only item in the array.
     *         If the tag is nested beneath the top level, the array is filled with the hierarchy with the tag name at the last index
     *         null if the the tag is not found.
     * @throws ServletException
     */
    static protected List<String> detectRecordElement(TreeParser parser, String tag) throws ServletException {
        try{
            if(parser.getEventType() == TreeParserToken.Ignorable)//XMLStreamConstants.START_DOCUMENT)
                parser.next();

            String localName = parser.getLocalName();
            String fullName = composeName(parser.getPrefix(), localName);
            if (tag.equals(parser.getLocalName()) || tag.equals(fullName)) {
                List<String> path = new LinkedList<String>();
                path.add(localName);

                return path;
            }

            while (parser.hasNext()) {
                TreeParserToken eventType = parser.next();
                if (eventType == TreeParserToken.EndEntity) {//XMLStreamConstants.END_ELEMENT) {
                    break;
                } else if (eventType == TreeParserToken.StartEntity) {//XMLStreamConstants.START_ELEMENT) {
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
     * Seeks for recurring element in a parsed document
     * which are likely candidates for being data records
     * @param parser
     *              The parser loaded with tree data
     * @return
     *              The path to the most numerous of the possible candidates.
     *              null if no candidates were found (less than 6 recurrences)
     */
    static public String[] detectRecordElement(TreeParser parser) {
        logger.trace("detectRecordElement(inputStream)");
        List<RecordElementCandidate> candidates = new ArrayList<RecordElementCandidate>();

        try {
            while (parser.hasNext()) {
                TreeParserToken eventType = parser.next();
                if (eventType == TreeParserToken.StartEntity) {
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
        logger.info("No candidate elements were found in data - at least 6 similar elements are required");
        return null;
    }

    static protected RecordElementCandidate detectRecordElement(TreeParser parser, String[] path) {
        logger.trace("detectRecordElement(TreeParser, String[])");
        List<RecordElementCandidate> descendantCandidates = new ArrayList<RecordElementCandidate>();

        Map<String, Integer> immediateChildCandidateMap = new HashMap<String, Integer>();
        int textNodeCount = 0;
        int childElementNodeCount = 0;

        try {
            while (parser.hasNext()) {
                TreeParserToken eventType = parser.next();
                if (eventType == TreeParserToken.EndEntity ) {
                    break;
                } else if (eventType == TreeParserToken.Value) {
                    try{
                        if (parser.getText().trim().length() > 0) {
                            textNodeCount++;
                        }
                    }catch(Exception e){
                        //silent
                    }
                } else if (eventType == TreeParserToken.StartEntity) {
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



    static public void importTreeData(
        TreeParser parser,
        Project project,
        String[] recordPath,
        ImportColumnGroup rootColumnGroup
    ) {
        logger.trace("importTreeData(TreeParser, Project, String[], ImportColumnGroup)");
        try {
            while (parser.hasNext()) {
                TreeParserToken eventType = parser.next();
                if (eventType == TreeParserToken.StartEntity) {
                    findRecord(project, parser, recordPath, 0, rootColumnGroup);
                }
            }
        } catch (Exception e) {
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
     * @throws ServletException
     */
    static protected void findRecord(
        Project project,
        TreeParser parser,
        String[] recordPath,
        int pathIndex,
        ImportColumnGroup rootColumnGroup
    ) throws ServletException {
        logger.trace("findRecord(Project, TreeParser, String[], int, ImportColumnGroup");
        
        if(parser.getEventType() == TreeParserToken.Ignorable){//XMLStreamConstants.START_DOCUMENT){
            logger.warn("Cannot use findRecord method for START_DOCUMENT event");
            return;
        }
        
        String tagName = parser.getLocalName();
        if (tagName.equals(recordPath[pathIndex])) {
            if (pathIndex < recordPath.length - 1) {
                while (parser.hasNext()) {
                    TreeParserToken eventType = parser.next();
                    if (eventType == TreeParserToken.StartEntity) {
                        findRecord(project, parser, recordPath, pathIndex + 1, rootColumnGroup);
                    } else if (eventType == TreeParserToken.EndEntity ) {
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

    static protected void skip(TreeParser parser) throws ServletException {
        while (parser.hasNext()) {
            TreeParserToken eventType = parser.next();
            if (eventType == TreeParserToken.StartEntity) {//XMLStreamConstants.START_ELEMENT) {
                skip(parser);
            } else if (eventType == TreeParserToken.EndEntity) { //XMLStreamConstants.END_ELEMENT) {
                return;
            }
        }
    }

    /**
     * processRecord parses Tree data for a single element and it's sub-elements,
     * adding the parsed data as a row to the project
     * @param project
     * @param parser
     * @param rootColumnGroup
     * @throws ServletException
     */
    static protected void processRecord(
        Project project,
        TreeParser parser,
        ImportColumnGroup rootColumnGroup
    ) throws ServletException {
        logger.trace("processRecord(Project,TreeParser,ImportColumnGroup)");
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
     * @throws ServletException
     */
    static protected void processSubRecord(
        Project project,
        TreeParser parser,
        ImportColumnGroup columnGroup,
        ImportRecord record
    ) throws ServletException {
        logger.trace("processSubRecord(Project,TreeParser,ImportColumnGroup,ImportRecord)");
        
        if(parser.getEventType() == TreeParserToken.Ignorable)
            return;
        
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
            TreeParserToken eventType = parser.next();
            if (eventType == TreeParserToken.StartEntity) {
                processSubRecord(
                    project,
                    parser,
                    thisColumnGroup,
                    record
                );
            } else if (//eventType == XMLStreamConstants.CDATA ||
                        eventType == TreeParserToken.Value) { //XMLStreamConstants.CHARACTERS) {
                String text = parser.getText();
                String colName = parser.getLocalName();
                if(text != null){
                    text = text.trim();
                    if (text.length() > 0) {
                        addCell(
                                project,
                                thisColumnGroup,
                                record,
                                colName,
                                parser.getText()
                        );
                    }
                }
            } else if (eventType == TreeParserToken.EndEntity) {
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
