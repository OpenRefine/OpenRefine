
package org.openrefine.browsing.columns;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.expr.ExpressionUtils;
import org.openrefine.model.Cell;
import org.openrefine.model.recon.Recon;

/**
 * A summary of the datatypes and recon statuses of cells within a column.
 */
public class ColumnStats implements Serializable {

    protected final long blanks;
    protected final long strings;
    protected final long numbers;
    protected final long booleans;
    protected final long dates;

    protected final long reconciled;
    protected final long matched;
    protected final long newElements;

    public static ColumnStats ZERO = new ColumnStats(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);

    @JsonCreator
    public ColumnStats(
            @JsonProperty("blanks") long blanks,
            @JsonProperty("strings") long strings,
            @JsonProperty("numbers") long numbers,
            @JsonProperty("booleans") long booleans,
            @JsonProperty("dates") long dates,
            @JsonProperty("reconciled") long reconciled,
            @JsonProperty("matched") long matched,
            @JsonProperty("new") long newElements) {
        this.blanks = blanks;
        this.strings = strings;
        this.numbers = numbers;
        this.booleans = booleans;
        this.dates = dates;
        this.reconciled = reconciled;
        this.matched = matched;
        this.newElements = newElements;
    }

    /**
     * The number of blank cells in the column
     * 
     * @return
     */
    @JsonProperty("blanks")
    public long getBlanks() {
        return blanks;
    }

    /**
     * The number of non-blank cells in the column.
     */
    @JsonProperty("nonBlanks")
    public long getNonBlanks() {
        return strings + numbers + booleans + dates;
    }

    /**
     * The number of non-blank strings in the column.
     */
    @JsonProperty("strings")
    public long getStrings() {
        return strings;
    }

    /**
     * The number of number cell values in the column.
     */
    @JsonProperty("numbers")
    public long getNumbers() {
        return numbers;
    }

    /**
     * The number of boolean-valued cells in the column.
     */
    @JsonProperty("booleans")
    public long getBooleans() {
        return booleans;
    }

    /**
     * The number of date-valued cells in the column.
     */
    @JsonProperty("dates")
    public long getDates() {
        return dates;
    }

    /**
     * The number of cells with a non-null recon object in them.
     */
    @JsonProperty("reconciled")
    public long getReconciled() {
        return reconciled;
    }

    /**
     * The number of cells matched to an existing entity in their recon object.
     */
    @JsonProperty("matched")
    public long getMatched() {
        return matched;
    }

    /**
     * The number of cells matched to a new entity in their recon object.
     */
    @JsonProperty("new")
    public long getNewElements() {
        return newElements;
    }

    /**
     * Returns a copy of this object after updating it with a single cell.
     *
     * @param cell
     *            the cell to extract datatype and reconciliation status from
     */
    public ColumnStats withCell(Cell cell) {
        long newBlanks = blanks;
        long newStrings = strings;
        long newNumbers = numbers;
        long newBooleans = booleans;
        long newDates = dates;

        long newReconciled = reconciled;
        long newMatched = matched;
        long newNew = newElements;

        if (cell == null || !ExpressionUtils.isNonBlankData(cell.value)) {
            newBlanks++;
        } else if (cell.value instanceof String) {
            newStrings++;
        } else if (cell.value instanceof Number) {
            newNumbers++;
        } else if (cell.value instanceof Boolean) {
            newBooleans++;
        } else if (cell.value instanceof Date || cell.value instanceof Calendar || cell.value instanceof OffsetDateTime) {
            newDates++;
        }

        if (cell != null && cell.recon != null) {
            newReconciled++;
            if (Recon.Judgment.Matched.equals(cell.recon.judgment)) {
                newMatched++;
            } else if (Recon.Judgment.New.equals(cell.recon.judgment)) {
                newNew++;
            }
        }
        return new ColumnStats(newBlanks, newStrings, newNumbers, newBooleans, newDates, newReconciled, newMatched, newNew);
    }

    /**
     * Return the column statistics obtained by summing the statistics in both objects, none of which are modified
     * (being immutable).
     */
    public ColumnStats sum(ColumnStats other) {
        return new ColumnStats(
                blanks + other.blanks,
                strings + other.strings,
                numbers + other.numbers,
                booleans + other.booleans,
                dates + other.dates,
                reconciled + other.reconciled,
                matched + other.matched,
                newElements + other.newElements);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColumnStats that = (ColumnStats) o;
        return blanks == that.blanks && strings == that.strings && numbers == that.numbers && booleans == that.booleans
                && dates == that.dates && reconciled == that.reconciled && matched == that.matched && newElements == that.newElements;
    }

    @Override
    public int hashCode() {
        return Objects.hash(blanks, strings, numbers, booleans, dates, reconciled, matched, newElements);
    }

    @Override
    public String toString() {
        return "ColumnStats{" +
                "blanks=" + blanks +
                ", strings=" + strings +
                ", numbers=" + numbers +
                ", booleans=" + booleans +
                ", dates=" + dates +
                ", reconciled=" + reconciled +
                ", matched=" + matched +
                ", new=" + newElements +
                '}';
    }

}
