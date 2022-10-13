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

package com.google.refine.importers.tree;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.CharMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.importers.tree.TreeReader.Token;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

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
     * @param parser
     * @param project
     * @param recordPath
     * @param rootColumnGroup
     * @param limit
     * @param parameters
     * @throws TreeReaderException
     * @deprecated 2020-07-23 Use
     *             {@link XmlImportUtilities#importTreeData(TreeReader, Project, String[], ImportColumnGroup, int, boolean, boolean, boolean)}
     */
    @Deprecated
    static public void importTreeData(
            TreeReader parser,
            Project project,
            String[] recordPath,
            ImportColumnGroup rootColumnGroup,
            int limit,
            ImportParameters parameters) throws TreeReaderException {
        importTreeData(parser, project, recordPath, rootColumnGroup, limit, parameters.trimStrings, parameters.storeEmptyStrings,
                parameters.guessDataType);
    }

    static public void importTreeData(
            TreeReader parser,
            Project project,
            String[] recordPath,
            ImportColumnGroup rootColumnGroup,
            int limit,
            boolean trimStrings,
            boolean storeEmptyStrings,
            boolean guessDataTypes) throws TreeReaderException {
        if (logger.isTraceEnabled()) {
            logger.trace("importTreeData(TreeReader, Project, String[], ImportColumnGroup)");
        }
        try {
            while (parser.hasNext()) {
                Token eventType = parser.next();
                if (eventType == Token.StartEntity) {
                    findRecord(project, parser, recordPath, 0, rootColumnGroup, limit--, trimStrings, storeEmptyStrings,
                            guessDataTypes);
                }
            }
        } catch (TreeReaderException e) {
            logger.error("Exception from XML parse", e);
            throw e;
        }
    }

    /**
     * @param project
     * @param parser
     * @param recordPath
     * @param pathIndex
     * @param rootColumnGroup
     * @param limit
     * @param parameters
     * @throws TreeReaderException
     * @deprecated Use
     *             {@link XmlImportUtilities#findRecord(Project, TreeReader, String[], int, ImportColumnGroup, int, boolean, boolean, boolean)}
     */
    @Deprecated
    static protected void findRecord(
            Project project,
            TreeReader parser,
            String[] recordPath,
            int pathIndex,
            ImportColumnGroup rootColumnGroup,
            int limit,
            ImportParameters parameters) throws TreeReaderException {
        findRecord(project, parser, recordPath, pathIndex, rootColumnGroup, limit, parameters.trimStrings,
                parameters.storeEmptyStrings, parameters.guessDataType);
    }

    /**
     * @param project
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
     * @throws TreeReaderException
     */
    static protected void findRecord(
            Project project,
            TreeReader parser,
            String[] recordPath,
            int pathIndex,
            ImportColumnGroup rootColumnGroup,
            int limit,
            boolean trimStrings,
            boolean storeEmptyStrings,
            boolean guessDataTypes) throws TreeReaderException {
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
                        findRecord(project, parser, recordPath, pathIndex + 1, rootColumnGroup, limit--,
                                trimStrings, storeEmptyStrings, guessDataTypes);
                    } else if (eventType == Token.EndEntity) {
                        break;
                    } else if (eventType == Token.Value) {
                        // This is when the user picks a specific field to import, not a whole object or element.
                        if (pathIndex == recordPath.length - 2) {
                            String desiredFieldName = recordPath[pathIndex + 1];
                            String currentFieldName = parser.getFieldName();
                            if (desiredFieldName.equals(currentFieldName)) {
                                processFieldAsRecord(project, parser, rootColumnGroup, trimStrings, storeEmptyStrings, guessDataTypes);
                            }
                        }
                    }
                }
            } else {
                processRecord(project, parser, rootColumnGroup, trimStrings, storeEmptyStrings, guessDataTypes);
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
     * @param project
     * @param parser
     * @param rootColumnGroup
     * @param parameter
     * @throws TreeReaderException
     * @deprecated Use
     *             {@link XmlImportUtilities#processRecord(Project, TreeReader, ImportColumnGroup, boolean, boolean, boolean)}
     */
    @Deprecated
    static protected void processRecord(
            Project project,
            TreeReader parser,
            ImportColumnGroup rootColumnGroup,
            ImportParameters parameter) throws TreeReaderException {
        processRecord(project, parser, rootColumnGroup, parameter.trimStrings, parameter.storeEmptyStrings, parameter.guessDataType);
    }

    /**
     * processRecord parses Tree data for a single element and it's sub-elements, adding the parsed data as a row to the
     * project
     * 
     * @param project
     * @param parser
     * @param rootColumnGroup
     * @throws TreeReaderException
     */
    static protected void processRecord(
            Project project,
            TreeReader parser,
            ImportColumnGroup rootColumnGroup,
            boolean trimStrings,
            boolean storeEmptyStrings,
            boolean guessDataTypes) throws TreeReaderException {
        if (logger.isTraceEnabled()) {
            logger.trace("processRecord(Project,TreeReader,ImportColumnGroup)");
        }
        ImportRecord record = new ImportRecord();

        processSubRecord(project, parser, rootColumnGroup, record, 0, trimStrings, storeEmptyStrings, guessDataTypes);
        addImportRecordToProject(record, project);
    }

    /**
     * processFieldAsRecord parses Tree data for a single element and it's sub-elements, adding the parsed data as a row
     * to the project
     * 
     * @param project
     * @param parser
     * @param rootColumnGroup
     * @throws TreeReaderException
     */
    @Deprecated
    static protected void processFieldAsRecord(Project project, TreeReader parser, ImportColumnGroup rootColumnGroup,
            ImportParameters parameter) throws TreeReaderException {
        processFieldAsRecord(project, parser, rootColumnGroup, parameter.trimStrings, parameter.storeEmptyStrings,
                parameter.guessDataType);
    }

    /**
     * processFieldAsRecord parses Tree data for a single element and it's sub-elements, adding the parsed data as a row
     * to the project
     * 
     * @param project
     * @param parser
     * @param rootColumnGroup
     * @throws TreeReaderException
     */
    static protected void processFieldAsRecord(
            Project project,
            TreeReader parser,
            ImportColumnGroup rootColumnGroup,
            boolean trimStrings,
            boolean storeEmptyStrings,
            boolean guessDataType) throws TreeReaderException {
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
                        project,
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
                    project,
                    rootColumnGroup,
                    record,
                    parser.getFieldName(),
                    value);
        }
        if (record != null) {
            addImportRecordToProject(record, project);
        }
    }

    @Deprecated
    static protected void addImportRecordToProject(ImportRecord record, Project project,
            boolean includeFileSources, String fileSource, boolean includeArchiveFileName, String archiveFileName) {
        addImportRecordToProject(record, project);
    }

    static protected void addImportRecordToProject(ImportRecord record, Project project) {
        for (List<Cell> row : record.rows) {
            if (row.size() > 0) {
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

    @Deprecated
    static protected void processSubRecord(
            Project project,
            TreeReader parser,
            ImportColumnGroup columnGroup,
            ImportRecord record,
            int level,
            ImportParameters parameter) throws TreeReaderException {
        processSubRecord(project, parser, columnGroup, record, level, parameter.trimStrings,
                parameter.storeEmptyStrings, parameter.guessDataType);
    }

    /**
     *
     * @param project
     * @param parser
     * @param columnGroup
     * @param record
     * @throws TreeReaderException
     */
    static protected void processSubRecord(
            Project project,
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
                project,
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
                        project,
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
                        project,
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
                        text = CharMatcher.whitespace().trimFrom(text);
                    }
                    addCell(project, thisColumnGroup, record, colName, text,
                            storeEmptyStrings, guessDataType);
                } else {
                    addCell(project, thisColumnGroup, record, colName, value);
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
