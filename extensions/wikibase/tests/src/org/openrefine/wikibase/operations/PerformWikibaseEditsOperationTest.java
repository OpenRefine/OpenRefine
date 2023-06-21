/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package org.openrefine.wikibase.operations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.mockito.Mockito;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.ListFacet;
import org.openrefine.history.GridPreservation;
import org.openrefine.expr.EvalError;
import org.openrefine.expr.MetaParser;
import org.openrefine.grel.Parser;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.Row;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.ReconConfig;
import org.openrefine.operations.ChangeResult;
import org.openrefine.operations.Operation;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.openrefine.wikibase.schema.WikibaseSchema;
import org.openrefine.wikibase.testing.TestingData;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PerformWikibaseEditsOperationTest extends OperationTest {

    ReconConfig reconConfig;
    ColumnModel columnModel;
    WikibaseSchema schema;
    Map<String, OverlayModel> overlayModels;
    Grid grid;
    PerformWikibaseEditsOperation operation;
    ChangeContext context;

    @BeforeMethod
    public void registerOperation() {
        registerOperation("perform-wikibase-edits", PerformWikibaseEditsOperation.class);
    }

    @BeforeMethod
    public void setUpDependencies() {
        reconConfig = mock(ReconConfig.class);
        columnModel = new ColumnModel(
                Arrays.asList(new ColumnMetadata("foo").withReconConfig(reconConfig),
                        new ColumnMetadata("bar")));
        schema = new WikibaseSchema(
                Collections.emptyList(),
                null,
                "http://site.iri",
                Collections.singletonMap("item", "http://site.iri"),
                "https://mediawiki.endpoint");
        overlayModels = Collections.singletonMap("wikibaseSchema", schema);
        grid = createGrid(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { TestingData.makeNewItemCell(1234L, "my new item"), "hey" }
                })
                        .withColumnModel(columnModel)
                        .withOverlayModels(overlayModels);

        operation = new PerformWikibaseEditsOperation(
                EngineConfig.reconstruct("{}"), "summary", 5, 50, "", 60, "tag", "editing results");

        context = mock(ChangeContext.class);
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
    }

    @Override
    public Operation reconstruct()
            throws Exception {
        return ParsingUtilities.mapper.readValue(getJson(), PerformWikibaseEditsOperation.class);
    }

    @Override
    public String getJson()
            throws Exception {
        return TestingData.jsonFromFile("operations/perform-edits.json");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testConstructor() {
        new PerformWikibaseEditsOperation(EngineConfig.reconstruct("{}"), "", 5, 50, "", 60, "tag", "errors");
    }

    @Test
    public void testApplyOperationWithSuccessfulEdit()
            throws Exception {
        PerformWikibaseEditsOperation.RowEditingResults rowNewReconUpdate = new PerformWikibaseEditsOperation.RowEditingResults(
                Collections.singletonMap(1234L, "Q789"), Collections.emptyList());
        ChangeData<PerformWikibaseEditsOperation.RowEditingResults> changeData = runner().changeDataFromList(
                Collections.singletonList(new IndexedData<>(0L, rowNewReconUpdate)));

        when(context.<PerformWikibaseEditsOperation.RowEditingResults> getChangeData(Mockito.eq(PerformWikibaseEditsOperation.changeDataId),
                Mockito.any(), Mockito.any()))
                        .thenReturn(changeData);

        ChangeResult changeResult = operation.apply(grid, context);
        assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);

        Grid applied = changeResult.getGrid();
        ColumnModel columnModel = applied.getColumnModel();
        assertEquals(columnModel.getColumnNames(), Arrays.asList("foo", "bar", "editing results"));

        Row row = applied.getRow(0L);
        assertEquals(row.getCell(0).recon.judgment, Recon.Judgment.Matched);
        assertEquals(row.getCell(0).recon.match.id, "Q789");
        assertEquals(row.getCell(2), null);
        String expectedJson = "[ {"
                + "  \"columnName\" : \"editing results\","
                + "  \"expression\" : \"grel:if(isError(value), 'failed edit', 'successful edit')\","
                + "  \"invert\" : false,"
                + "  \"name\" : \"Wikibase editing status\","
                + "  \"omitBlank\" : false,"
                + "  \"omitError\" : false,"
                + "  \"selectBlank\" : false,"
                + "  \"selectError\" : false,"
                + "  \"selection\" : [ ]"
                + "} ] ";
        TestUtils.isSerializedTo(changeResult.getCreatedFacets(), expectedJson, ParsingUtilities.defaultWriter);
    }

    @Test
    public void testApplyOperationWithFailingEdit()
            throws Exception {
        PerformWikibaseEditsOperation.RowEditingResults rowNewReconUpdate = new PerformWikibaseEditsOperation.RowEditingResults(
                Collections.emptyMap(), Collections.singletonList("error: this entity already exists"));
        ChangeData<PerformWikibaseEditsOperation.RowEditingResults> changeData = runner().changeDataFromList(
                Collections.singletonList(new IndexedData<>(0L, rowNewReconUpdate)));

        when(context.<PerformWikibaseEditsOperation.RowEditingResults> getChangeData(Mockito.eq(PerformWikibaseEditsOperation.changeDataId),
                Mockito.any(), Mockito.any()))
                        .thenReturn(changeData);

        ChangeResult changeResult = operation.apply(grid, context);
        assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);

        Grid applied = changeResult.getGrid();
        ColumnModel columnModel = applied.getColumnModel();
        assertEquals(columnModel.getColumnNames(), Arrays.asList("foo", "bar", "editing results"));

        Row row = applied.getRow(0L);
        assertEquals(row.getCell(0).recon.judgment, Recon.Judgment.New);
        assertEquals(row.getCell(2).value, new EvalError("error: this entity already exists"));
    }

    @Test
    public void testPendingChange()
            throws Exception {
        ChangeData<PerformWikibaseEditsOperation.RowEditingResults> changeData = runner().emptyChangeData();

        when(context.<PerformWikibaseEditsOperation.RowEditingResults> getChangeData(Mockito.eq(PerformWikibaseEditsOperation.changeDataId),
                Mockito.any(), Mockito.any()))
                        .thenReturn(changeData);

        ChangeResult changeResult = operation.apply(grid, context);
        assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);
        Grid applied = changeResult.getGrid();

        Row row = applied.getRow(0L);
        assertEquals(row.getCell(0).recon.judgment, Recon.Judgment.New);
        assertTrue(row.getCell(0).isPending());
    }

}
