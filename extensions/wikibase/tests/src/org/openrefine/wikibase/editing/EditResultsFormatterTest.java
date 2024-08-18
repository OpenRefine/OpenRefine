
package org.openrefine.wikibase.editing;

import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.model.Cell;
import com.google.refine.model.changes.CellAtRow;

import org.openrefine.wikibase.editing.EditBatchProcessor.EditResult;

public class EditResultsFormatterTest {

    String apiEndpoint = "https://my.wikibase.instance/w/api.php";
    EditResult success1 = new EditResult(Collections.singleton(1), null, null, 1234L, OptionalLong.of(4567L), null);
    EditResult success1Dup = new EditResult(Collections.singleton(1), null, null, 111L, OptionalLong.of(3748L), null);
    EditResult success2 = new EditResult(Collections.singleton(2), null, null, 555L, OptionalLong.of(8790L), null);
    EditResult newEntity = new EditResult(Collections.singleton(2), null, null, 0L, OptionalLong.empty(), "http://foo.com/bar");
    EditResult error1 = new EditResult(Collections.singleton(1), "blocked", "You have been blocked from editing.", 1234L,
            OptionalLong.empty(), null);
    EditResult error1Dup = new EditResult(Collections.singleton(1), "illegal-value", "The value supplied is invalid", 111L,
            OptionalLong.empty(), null);
    EditResult error2 = new EditResult(Collections.singleton(2), "duplicate-item", "You are attempting to create a duplicate of [[Q1234]]",
            123L, OptionalLong.empty(), null);
    EditResult successNoEdit = new EditResult(Collections.singleton(1), null, null, 1234L, OptionalLong.of(1234L), null);

    Comparator<CellAtRow> comparator = new Comparator<>() {

        @Override
        public int compare(CellAtRow arg0, CellAtRow arg1) {
            return arg0.row - arg1.row;
        }

    };

    @Test
    public void testSuccessesAndFailures() {
        EditResultsFormatter formatter = new EditResultsFormatter(apiEndpoint);
        formatter.add(success1);
        formatter.add(error2);

        List<CellAtRow> results = formatter.toCells().stream().sorted(comparator).collect(Collectors.toList());
        assertEquals(results,
                Arrays.asList(
                        new CellAtRow(1, new Cell("https://my.wikibase.instance/w/index.php?diff=prev&oldid=4567", null)),
                        new CellAtRow(2,
                                new Cell(new EvalError("[duplicate-item] You are attempting to create a duplicate of [[Q1234]]"), null))));
    }

    @Test
    public void testSuccessesNoEdit() {
        EditResultsFormatter formatter = new EditResultsFormatter(apiEndpoint);
        formatter.add(successNoEdit);

        List<CellAtRow> results = formatter.toCells().stream().sorted(comparator).collect(Collectors.toList());
        assertEquals(results, Collections.emptyList());
    }

    @Test
    public void testFailuresHavePriority() {
        EditResultsFormatter formatter = new EditResultsFormatter(apiEndpoint);
        formatter.add(success2);
        formatter.add(error2);

        List<CellAtRow> results = formatter.toCells().stream().sorted(comparator).collect(Collectors.toList());
        assertEquals(results,
                Arrays.asList(
                        new CellAtRow(2,
                                new Cell(new EvalError("[duplicate-item] You are attempting to create a duplicate of [[Q1234]]"), null))));
    }

    @Test
    public void testMultipleErrors() {
        EditResultsFormatter formatter = new EditResultsFormatter(apiEndpoint);
        formatter.add(error1);
        formatter.add(error1Dup);

        List<CellAtRow> results = formatter.toCells().stream().sorted(comparator).collect(Collectors.toList());
        assertEquals(results,
                Arrays.asList(
                        new CellAtRow(1, new Cell(new EvalError(
                                "[blocked] You have been blocked from editing.; [illegal-value] The value supplied is invalid"), null))));
    }

    @Test
    public void testMultipleSuccesses() {
        EditResultsFormatter formatter = new EditResultsFormatter(apiEndpoint);
        formatter.add(success1);
        formatter.add(success1Dup);

        List<CellAtRow> results = formatter.toCells().stream().sorted(comparator).collect(Collectors.toList());
        assertEquals(results,
                Arrays.asList(
                        new CellAtRow(1, new Cell(
                                "https://my.wikibase.instance/w/index.php?diff=prev&oldid=4567 https://my.wikibase.instance/w/index.php?diff=prev&oldid=3748",
                                null))));
    }

    @Test
    public void testNewEntity() {
        EditResultsFormatter formatter = new EditResultsFormatter(apiEndpoint);
        formatter.add(newEntity);

        List<CellAtRow> results = formatter.toCells().stream().sorted(comparator).collect(Collectors.toList());
        assertEquals(results,
                Arrays.asList(
                        new CellAtRow(2, new Cell(
                                "http://foo.com/bar",
                                null))));
    }

}
