package com.google.refine.exporters;

import java.io.IOException;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.databind.JsonNode;

import com.google.refine.ProjectManager;
import com.google.refine.browsing.Engine;
import com.google.refine.exporters.TabularSerializer.CellData;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

/**
 * Base class of the exporters that write a project as a spreadsheet file (Excel and OpenDocument).
 * <p>
 * Template Method pattern: this class defines the fixed skeleton of an export (create the document, write the header
 * and the rows, save the document) and of the writing of each row (start the row, start each cell, decide which kind of
 * value it holds, write it, optionally write its hyperlink). Subclasses only supply the format-specific steps, which
 * are the abstract and hook methods below.
 * <p>
 * Exporters are registered once and shared by all requests (see {@code registerExporters} in the core module's
 * {@code controller.js}), so a subclass must <b>not</b> keep export data in instance fields. Everything that belongs to
 * one export (the document, the current row and cell, counters...) lives in a state object of type {@code S}, created
 * by {@link #createState(Project)} and handed to every step.
 *
 * @param <S>
 *            the state of one export, defined by each subclass
 */
public abstract class AbstractSpreadsheetExporter<S> implements StreamExporter {

    /**
     * The template method: the skeleton of an export. It is final so that subclasses cannot change the order of the
     * steps, only the steps themselves.
     */
    @Override
    public final void export(final Project project, Properties params, Engine engine,
            OutputStream outputStream) throws IOException {

        final S state = createState(project);

        TabularSerializer serializer = new TabularSerializer() {

            @Override
            public void startFile(JsonNode options) {
                startSheet(state, ProjectManager.singleton.getProjectMetadata(project.id).getName());
            }

            @Override
            public void endFile() {
            }

            @Override
            public void addRow(List<CellData> cells, boolean isHeader) {
                writeRow(state, cells);
            }
        };

        CustomizableTabularExporterUtilities.exportRows(
                project, engine, params, serializer);

        writeDocument(state, outputStream);
        outputStream.flush();
        closeDocument(state);
    }

    /**
     * Template of the writing of one row: the same for every spreadsheet format.
     */
    private void writeRow(S state, List<CellData> cells) {
        startRow(state);

        for (int i = 0; i < cells.size(); i++) {
            startCell(state, i, cells.size());
            CellData cellData = cells.get(i);

            if (cellData != null && cellData.text != null && cellData.value != null) {
                writeValue(state, i, cellData);

                if (cellData.link != null) {
                    writeLink(state, cellData.text, cellData.link);
                }
            }
        }

        endRow(state);
    }

    /**
     * Chooses how to write a value according to its type, then delegates the actual writing to the subclass.
     */
    private void writeValue(S state, int column, CellData cellData) {
        Object v = cellData.value;
        if (v instanceof Number) {
            writeNumber(state, ((Number) v).doubleValue());
        } else if (v instanceof Boolean) {
            writeBoolean(state, ((Boolean) v).booleanValue());
        } else if (v instanceof OffsetDateTime) {
            writeDate(state, ParsingUtilities.offsetDateTimeToCalendar((OffsetDateTime) v));
        } else {
            writeText(state, column, cellData.text);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Steps supplied by the subclasses
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Creates the document and the state of a new export.
     */
    protected abstract S createState(Project project) throws IOException;

    /**
     * Creates the sheet (or table) that will receive the rows.
     *
     * @param projectName
     *            the name of the project, to be used as the name of the sheet
     */
    protected abstract void startSheet(S state, String projectName);

    /**
     * Starts a new row, which becomes the current row.
     */
    protected abstract void startRow(S state);

    /**
     * Starts the cell at the given index in the current row, which becomes the current cell. It is called for every
     * column, even when the cell has no value to write.
     *
     * @param column
     *            index of the cell in the row
     * @param columnCount
     *            number of cells in the row
     */
    protected abstract void startCell(S state, int column, int columnCount);

    protected abstract void writeNumber(S state, double value);

    protected abstract void writeBoolean(S state, boolean value);

    protected abstract void writeDate(S state, Calendar value);

    /**
     * @param column
     *            index of the cell in the row, for error messages
     */
    protected abstract void writeText(S state, int column, String text);

    /**
     * Writes the finished document to the output stream.
     */
    protected abstract void writeDocument(S state, OutputStream outputStream) throws IOException;

    // ---------------------------------------------------------------------------------------------------------
    // Hooks: optional steps, which do nothing by default
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Writes a hyperlink in the current cell. By default hyperlinks are ignored, for the formats that cannot write
     * them.
     */
    protected void writeLink(S state, String text, String link) {
    }

    /**
     * Called after all the cells of a row have been written.
     */
    protected void endRow(S state) {
    }

    /**
     * Releases the resources of the document, after it has been written and the stream flushed.
     */
    protected void closeDocument(S state) throws IOException {
    }
}