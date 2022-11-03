/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.openrefine.importers.tree;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

import com.google.common.base.CharMatcher;
import org.openrefine.importers.tree.TreeReader.Token;
import org.openrefine.model.Cell;
import org.openrefine.model.Row;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlImportUtilities extends TreeImportUtilities {

    final static Logger logger = LoggerFactory.getLogger("XmlImportUtilities");

    static public String[] detectPathFromTag(TreeReader parser, String tag) throws TreeReaderException {
        while (parser.hasNext()) {
            Token eventType = parser.next();
            if (eventType == Token.StartEntity) {// XMLStreamConstants.START_ELEMENT) {
                List<String> path = detectRecordElement(parser, tag);
                if (path != null) {
                    String[] path2 = new String[path.size()];

                    path.toArray(path2);

                    return path2;
                }
            }
        }

        return null;
    }

    /**
     * Looks for an element with the given tag name in the Tree data being parsed, returning the path hierarchy to reach
     * it.
     *
     * @param parser
     * @param tag
     *            The element name (can be qualified) to search for
     * @return If the tag is found, an array of strings is returned. If the tag is at the top level, the tag will be the
     *         only item in the array. If the tag is nested beneath the top level, the array is filled with the
     *         hierarchy with the tag name at the last index null if the the tag is not found.
     * @throws TreeReaderException
     */
    static protected List<String> detectRecordElement(TreeReader parser, String tag) throws TreeReaderException {
        if (parser.current() == Token.Ignorable) {
            parser.next();
        }

        String localName = parser.getFieldName();
        String fullName = composeName(parser.getPrefix(), localName);
        if (tag.equals(parser.getFieldName()) || tag.equals(fullName)) {
            List<String> path = new LinkedList<String>();
            path.add(localName);

            return path;
        }

        while (parser.hasNext()) {
            Token eventType = parser.next();
            if (eventType == Token.EndEntity) {// XMLStreamConstants.END_ELEMENT) {
                break;
            } else if (eventType == Token.StartEntity) {// XMLStreamConstants.START_ELEMENT) {
                List<String> path = detectRecordElement(parser, tag);
                if (path != null) {
                    path.add(0, localName);
                    return path;
                }
            }
        }
        return null;
    }

    static protected String composeName(String prefix, String localName) {
        return prefix != null && prefix.length() > 0 ? (prefix + ":" + localName) : localName;
    }

    /**
     * Seeks for recurring element in a parsed document which are likely candidates for being data records
     * 
     * @param parser
     *            The parser loaded with tree data
     * @return The path to the most numerous of the possible candidates. null if no candidates were found (less than 6
     *         recurrences)
     */
    static public String[] detectRecordElement(TreeReader parser) {
        if (logger.isTraceEnabled()) {
            logger.trace("detectRecordElement(inputStream)");
        }
        List<RecordElementCandidate> candidates = new ArrayList<RecordElementCandidate>();

        try {
            while (parser.hasNext()) {
                Token eventType = parser.next();
                if (eventType == Token.StartEntity) {
                    RecordElementCandidate candidate = detectRecordElement(
                            parser,
                            new String[] { parser.getFieldName() });

                    if (candidate != null) {
                        candidates.add(candidate);
                    }
                }
            }
        } catch (TreeReaderException e) {
            // silent
            e.printStackTrace();
        }

        if (candidates.size() > 0) {
            sortRecordElementCandidates(candidates);

            return candidates.get(0).path;
        }
        logger.info("No candidate elements were found in data - at least 6 similar elements are required");
        return null;
    }

    static protected RecordElementCandidate detectRecordElement(TreeReader parser, String[] path) {
        if (logger.isTraceEnabled()) {
            logger.trace("detectRecordElement(TreeReader, String[])");
        }
        List<RecordElementCandidate> descendantCandidates = new ArrayList<RecordElementCandidate>();

        Map<String, Integer> immediateChildCandidateMap = new HashMap<String, Integer>();

        try {
            while (parser.hasNext()) {
                Token eventType = parser.next();
                if (eventType == Token.EndEntity) {
                    break;
                } else if (eventType == Token.StartEntity) {
                    String tagName = parser.getFieldName();

                    immediateChildCandidateMap.put(
                            tagName,
                            immediateChildCandidateMap.containsKey(tagName) ? immediateChildCandidateMap.get(tagName) + 1 : 1);

                    String[] path2 = new String[path.length + 1];
                    System.arraycopy(path, 0, path2, 0, path.length);
                    path2[path.length] = tagName;

                    RecordElementCandidate c = detectRecordElement(parser, path2);
                    if (c != null) {
                        descendantCandidates.add(c);
                    }
                }
            }
        } catch (TreeReaderException e) {
            // silent
            e.printStackTrace();
        }

        if (immediateChildCandidateMap.size() > 0) {
            List<RecordElementCandidate> immediateChildCandidates = new ArrayList<RecordElementCandidate>(
                    immediateChildCandidateMap.size());
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
                if (logger.isTraceEnabled()) {
                    logger.trace("ourCandidate.count : " + ourCandidate.count + "; immediateChildCandidates.size() : "
                            + immediateChildCandidates.size());
                }
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

    /**
     * @param parser
     * @param columnIndexAllocator
     * @param rows
     * @param recordPath
     * @param rootColumnGroup
     * @param limit
     * @param parameters
     * @throws TreeReaderException
     * @deprecated 2020-07-23 Use the version of this method which expands all parameters in the signature of the
     *             function
     */
    @Deprecated
    static public void importTreeData(
            TreeReader parser,
            ColumnIndexAllocator columnIndexAllocator,
            List<Row> rows,
            String[] recordPath,
            ImportColumnGroup rootColumnGroup,
            int limit,
            ImportParameters parameters) throws TreeReaderException {
        importTreeData(parser, columnIndexAllocator, rows, recordPath, rootColumnGroup, limit,
                parameters.trimStrings, parameters.storeEmptyStrings, parameters.guessDataType,
                parameters.includeFileSources, parameters.fileSource, false, null);
    }

    static public void importTreeData(
            TreeReader parser,
            ColumnIndexAllocator columnIndexAllocator,
            List<Row> rows,
            String[] recordPath,
            ImportColumnGroup rootColumnGroup,
            long limit,
            boolean trimStrings,
            boolean storeEmptyStrings,
            boolean guessDataTypes,
            boolean includeFileSources,
            String fileSource,
            boolean includeArchiveName,
            String archiveFileName) throws TreeReaderException {
        if (logger.isTraceEnabled()) {
            logger.trace("importTreeData(TreeReader, Project, String[], ImportColumnGroup)");
        }
        try {
            while (parser.hasNext()) {
                Token eventType = parser.next();
                if (eventType == Token.StartEntity) {
                    findRecord(columnIndexAllocator, rows, parser, recordPath, 0, rootColumnGroup, limit--, trimStrings, storeEmptyStrings,
                            guessDataTypes, includeFileSources, fileSource, includeArchiveName, archiveFileName);
                }
            }
        } catch (TreeReaderException e) {
            logger.error("Exception from XML parse", e);
            throw e;
        }
    }

    /**
     * @param allocator
     * @param rows
     * @param parser
     * @param recordPath
     * @param pathIndex
     * @param rootColumnGroup
     * @param limit
     * @param parameters
     * @throws TreeReaderException
     * @deprecated Use the version of this method which expands all parameters in the signature of the function
     */
    @Deprecated
    static protected void findRecord(
            ColumnIndexAllocator allocator,
            List<Row> rows,
            TreeReader parser,
            String[] recordPath,
            int pathIndex,
            ImportColumnGroup rootColumnGroup,
            int limit,
            ImportParameters parameters) throws TreeReaderException {
        findRecord(allocator, rows, parser, recordPath, pathIndex, rootColumnGroup, limit, parameters.trimStrings,
                parameters.storeEmptyStrings, parameters.guessDataType, parameters.includeFileSources, parameters.fileSource, false, null);
    }

    /**
     * @param columnIndexAllocator
     * @param rows
     * @param parser
     * @param recordPath
     * @param pathIndex
     * @param rootColumnGroup
     * @param limit
     * @param trimStrings
     *            trim whitespace from strings if true
     * @param storeEmptyStrings
     *            store empty strings if true
     * @param guessDataTypes
     *            guess whether strings represent numbers and convert
     * @param archiveFileName
     * @param includeArchiveName
     * @throws TreeReaderException
     */
    static protected void findRecord(
            ColumnIndexAllocator columnIndexAllocator,
            List<Row> rows,
            TreeReader parser,
            String[] recordPath,
            int pathIndex,
            ImportColumnGroup rootColumnGroup,
            long limit,
            boolean trimStrings,
            boolean storeEmptyStrings,
            boolean guessDataTypes,
            boolean includeFileSource,
            String fileSource,
            boolean includeArchiveName,
            String archiveFileName) throws TreeReaderException {
        if (logger.isTraceEnabled()) {
            logger.trace("findRecord(Project, TreeReader, String[], int, ImportColumnGroup - path:" + Arrays.toString(recordPath));
        }
        if (parser.current() == Token.Ignorable) {// XMLStreamConstants.START_DOCUMENT){
            logger.warn("Cannot use findRecord method for START_DOCUMENT event");
            return;
        }

        String recordPathSegment = recordPath[pathIndex];

        String localName = parser.getFieldName();
        String fullName = composeName(parser.getPrefix(), localName);
        if (recordPathSegment.equals(localName) || recordPathSegment.equals(fullName)) {
            if (pathIndex < recordPath.length - 1) {
                while (parser.hasNext() && limit != 0) {
                    Token eventType = parser.next();
                    if (eventType == Token.StartEntity) {
                        findRecord(columnIndexAllocator, rows, parser, recordPath, pathIndex + 1, rootColumnGroup, limit--,
                                trimStrings, storeEmptyStrings, guessDataTypes, includeFileSource, fileSource, includeArchiveName,
                                archiveFileName);
                    } else if (eventType == Token.EndEntity) {
                        break;
                    } else if (eventType == Token.Value) {
                        // This is when the user picks a specific field to import, not a whole object or element.
                        if (pathIndex == recordPath.length - 2) {
                            String desiredFieldName = recordPath[pathIndex + 1];
                            String currentFieldName = parser.getFieldName();
                            if (desiredFieldName.equals(currentFieldName)) {
                                processFieldAsRecord(columnIndexAllocator, rows, parser, rootColumnGroup, trimStrings, storeEmptyStrings,
                                        guessDataTypes, includeFileSource, fileSource, includeArchiveName, archiveFileName);
                            }
                        }
                    }
                }
            } else {
                processRecord(columnIndexAllocator, rows, parser, rootColumnGroup, trimStrings, storeEmptyStrings, guessDataTypes,
                        includeFileSource, fileSource, includeArchiveName, archiveFileName);
            }
        } else {
            skip(parser);
        }
    }

    static protected void skip(TreeReader parser) throws TreeReaderException {
        while (parser.hasNext()) {
            Token eventType = parser.next();
            if (eventType == Token.StartEntity) {// XMLStreamConstants.START_ELEMENT) {
                skip(parser);
            } else if (eventType == Token.EndEntity) { // XMLStreamConstants.END_ELEMENT) {
                return;
            }
        }
    }

    /**
     * @param allocator
     * @param rows
     * @param parser
     * @param rootColumnGroup
     * @param parameter
     * @throws TreeReaderException
     * @deprecated Use the version of this method which expands all parameters in the signature of the function
     */
    @Deprecated
    static protected void processRecord(
            ColumnIndexAllocator allocator,
            List<Row> rows,
            TreeReader parser,
            ImportColumnGroup rootColumnGroup,
            ImportParameters parameter) throws TreeReaderException {
        processRecord(allocator, rows, parser, rootColumnGroup,
                parameter.trimStrings, parameter.storeEmptyStrings,
                parameter.guessDataType, parameter.includeFileSources,
                parameter.fileSource, false, null);
    }

    /**
     * processRecord parses Tree data for a single element and it's sub-elements, adding the parsed data as a row to the
     * project
     * 
     * @param columnIndexAllocator
     * @param rows
     * @param parser
     * @param rootColumnGroup
     * @param archiveFileName
     * @param includeArchiveName
     * @throws TreeReaderException
     */
    static protected void processRecord(
            ColumnIndexAllocator columnIndexAllocator,
            List<Row> rows,
            TreeReader parser,
            ImportColumnGroup rootColumnGroup,
            boolean trimStrings,
            boolean storeEmptyStrings,
            boolean guessDataTypes,
            boolean includeFileSources,
            String fileSource,
            boolean includeArchiveName,
            String archiveFileName) throws TreeReaderException {
        if (logger.isTraceEnabled()) {
            logger.trace("processRecord(Project,TreeReader,ImportColumnGroup)");
        }
        ImportRecord record = new ImportRecord();

        processSubRecord(columnIndexAllocator, rows, parser, rootColumnGroup, record, 0, trimStrings, storeEmptyStrings, guessDataTypes);
        addImportRecordToProject(record, rows, includeFileSources, fileSource, includeArchiveName, archiveFileName);
    }

    /**
     * processFieldAsRecord parses Tree data for a single element and its sub-elements, adding the parsed data as a row
     * to the project
     * 
     * @param columnIndexAllocator
     * @param parser
     * @param rootColumnGroup
     * @throws TreeReaderException
     */
    static protected void processFieldAsRecord(
            ColumnIndexAllocator columnIndexAllocator,
            List<Row> rows,
            TreeReader parser,
            ImportColumnGroup rootColumnGroup,
            boolean trimStrings,
            boolean storeEmptyStrings,
            boolean guessDataType,
            boolean includeFileSources,
            String fileSource,
            boolean includeArchiveName,
            String archiveFileName) throws TreeReaderException {
        if (logger.isTraceEnabled()) {
            logger.trace("processFieldAsRecord(Project,TreeReader,ImportColumnGroup)");
        }
        Serializable value = parser.getValue();
        ImportRecord record = null;
        if (value instanceof String) {
            String text = (String) value;
            if (trimStrings) {
                text = CharMatcher.whitespace().trimFrom(text);
            }
            if (text.length() > 0 | !storeEmptyStrings) {
                record = new ImportRecord();
                addCell(
                        columnIndexAllocator,
                        rootColumnGroup,
                        record,
                        parser.getFieldName(),
                        (String) value,
                        storeEmptyStrings,
                        guessDataType);
            }
        } else {
            record = new ImportRecord();
            addCell(
                    columnIndexAllocator,
                    rootColumnGroup,
                    record,
                    parser.getFieldName(),
                    value);
        }
        if (record != null) {
            addImportRecordToProject(record, rows, includeFileSources, fileSource, includeArchiveName, archiveFileName);
        }
    }

    static protected void addImportRecordToProject(ImportRecord record, List<Row> project, boolean includeFileSources, String fileSource,
            boolean includeArchiveName, String archiveName) {
        for (List<Cell> row : record.rows) {
            if (row.size() > 0) {
                Row realRow = new Row(row, false, false);
                if (includeFileSources) {
                    realRow = realRow.insertCell(0, new Cell(fileSource, null));
                }
                if (includeArchiveName) {
                    realRow = realRow.insertCell(0, new Cell(archiveName, null));
                }
                project.add(realRow);
            }
        }
    }

    @Deprecated
    static protected void processSubRecord(
            ColumnIndexAllocator columnIndexAllocator,
            List<Row> rows,
            TreeReader parser,
            ImportColumnGroup columnGroup,
            ImportRecord record,
            int level,
            ImportParameters parameter) throws TreeReaderException {
        processSubRecord(columnIndexAllocator, rows, parser, columnGroup, record, level, parameter.trimStrings,
                parameter.storeEmptyStrings, parameter.guessDataType);
    }

    /**
     *
     * @param columnIndexAllocator
     * @param rows
     * @param parser
     * @param columnGroup
     * @param record
     * @throws TreeReaderException
     */
    static protected void processSubRecord(
            ColumnIndexAllocator columnIndexAllocator,
            List<Row> rows,
            TreeReader parser,
            ImportColumnGroup columnGroup,
            ImportRecord record,
            int level,
            boolean trimStrings,
            boolean storeEmptyStrings,
            boolean guessDataType) throws TreeReaderException {
        if (logger.isTraceEnabled()) {
            logger.trace("processSubRecord(Project,TreeReader,ImportColumnGroup,ImportRecord) lvl:" + level + " " + columnGroup);
        }

        if (parser.current() == Token.Ignorable) {
            return;
        }

        ImportColumnGroup thisColumnGroup = getColumnGroup(
                columnGroup,
                composeName(parser.getPrefix(), parser.getFieldName()));

        thisColumnGroup.nextRowIndex = Math.max(thisColumnGroup.nextRowIndex, columnGroup.nextRowIndex);

        int attributeCount = parser.getAttributeCount();
        for (int i = 0; i < attributeCount; i++) {
            String text = parser.getAttributeValue(i);
            if (trimStrings) {
                text = CharMatcher.whitespace().trimFrom(text);
            }
            if (text.length() > 0 | !storeEmptyStrings) {
                addCell(
                        columnIndexAllocator,
                        thisColumnGroup,
                        record,
                        composeName(parser.getAttributePrefix(i), parser.getAttributeLocalName(i)),
                        text,
                        storeEmptyStrings,
                        guessDataType);
            }
        }

        while (parser.hasNext()) {
            Token eventType = parser.next();
            if (eventType == Token.StartEntity) {
                processSubRecord(
                        columnIndexAllocator,
                        rows,
                        parser,
                        thisColumnGroup,
                        record,
                        level + 1,
                        trimStrings,
                        storeEmptyStrings,
                        guessDataType);
            } else if (// eventType == XMLStreamConstants.CDATA ||
            eventType == Token.Value) { // XMLStreamConstants.CHARACTERS) {
                Serializable value = parser.getValue();
                String colName = parser.getFieldName();
                if (value instanceof String) {
                    String text = (String) value;
                    if (trimStrings) {
                        text = text.trim();
                    }
                    addCell(columnIndexAllocator, thisColumnGroup, record, colName, text,
                            storeEmptyStrings, guessDataType);
                } else {
                    addCell(columnIndexAllocator, thisColumnGroup, record, colName, value);
                }
            } else if (eventType == Token.EndEntity) {
                break;
            } else if (eventType == Token.Ignorable) {
                continue;
            } else {
                logger.info("unknown event type " + eventType);
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
