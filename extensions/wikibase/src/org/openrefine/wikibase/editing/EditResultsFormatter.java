
package org.openrefine.wikibase.editing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.google.refine.expr.EvalError;
import com.google.refine.model.Cell;
import com.google.refine.model.changes.CellAtRow;

import org.openrefine.wikibase.editing.EditBatchProcessor.EditResult;

/**
 * Formats the results of Wikibase edits into cells to be included in the project's grid.
 */
public class EditResultsFormatter {

    private final String baseUrl;
    private final Map<Integer, String> rowIdToError = new HashMap<>();
    private final Map<Integer, String> rowIdToEditLink = new HashMap<>();

    /**
     * Constructor.
     * 
     * @param mediaWikiApiEndpoint
     *            full API endpoint, ending with .../w/api.php
     */
    public EditResultsFormatter(String mediaWikiApiEndpoint) {
        this.baseUrl = mediaWikiApiEndpoint.substring(0, mediaWikiApiEndpoint.length() - "w/api.php".length());
    }

    /**
     * Log another edit result for further formatting.
     */
    public void add(EditResult result) {
        if (!result.getCorrespondingRowIds().isEmpty()) {
            int firstRowId = result.getCorrespondingRowIds().stream().min(Comparator.naturalOrder()).get();
            if (result.getErrorMessage() != null && result.getErrorCode() != null) {
                String error = String.format("[%s] %s", result.getErrorCode(), result.getErrorMessage());
                String existingError = rowIdToError.get(firstRowId);
                if (existingError == null) {
                    rowIdToError.put(firstRowId, error);
                } else {
                    rowIdToError.put(firstRowId, existingError + "; " + error);
                }
            } else if (getEditUrl(result).isPresent()) {
                String revisionLink = getEditUrl(result).get();
                String existingLinks = rowIdToEditLink.get(firstRowId);
                if (existingLinks == null) {
                    rowIdToEditLink.put(firstRowId, revisionLink);
                } else {
                    rowIdToEditLink.put(firstRowId, existingLinks + " " + revisionLink);
                }
            }
        }
    }

    private Optional<String> getEditUrl(EditResult result) {
        if (result.getLastRevisionId().isPresent() && result.getBaseRevisionId() != result.getLastRevisionId().getAsLong()) {
            return Optional.of(baseUrl + "w/index.php?diff=prev&oldid=" + result.getLastRevisionId().getAsLong());
        } else if (result.getNewEntityUrl() != null) {
            return Optional.of(result.getNewEntityUrl());
        }
        return Optional.empty();
    }

    /**
     * Generate a list of cells on specific rows representing the editing results
     */
    public List<CellAtRow> toCells() {
        List<CellAtRow> cells = new ArrayList<>();
        for (Entry<Integer, String> errorCell : rowIdToError.entrySet()) {
            cells.add(new CellAtRow(errorCell.getKey(), new Cell(new EvalError(errorCell.getValue()), null)));
        }
        for (Entry<Integer, String> editLinkCell : rowIdToEditLink.entrySet()) {
            int rowId = editLinkCell.getKey();
            if (rowIdToError.get(rowId) == null) {
                cells.add(new CellAtRow(rowId, new Cell(editLinkCell.getValue(), null)));
            }
        }
        return cells;
    }
}
