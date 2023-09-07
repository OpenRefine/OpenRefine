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

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.history.GridPreservation;
import org.openrefine.model.Grid;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.ChangeResult;
import org.openrefine.operations.Operation;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.wikibase.schema.WikibaseSchema;
import org.openrefine.wikibase.testing.TestingData;

public class SaveWikibaseSchemaOperationTest extends OperationTest {

    @BeforeMethod
    public void registerOperation() {
        registerOperation("save-wikibase-schema", SaveWikibaseSchemaOperation.class);
    }

    @Override
    public Operation reconstruct()
            throws Exception {
        return ParsingUtilities.mapper.readValue(getJson(), SaveWikibaseSchemaOperation.class);
    }

    @Override
    public String getJson()
            throws Exception {
        return TestingData.jsonFromFile("operations/save-schema.json");
    }

    @Test
    public void testApply()
            throws Exception {
        Operation operation = reconstruct();
        ChangeContext context = mock(ChangeContext.class);

        ChangeResult changeResult = operation.apply(project.getCurrentGrid(), context);
        Assert.assertEquals(changeResult.getGridPreservation(), GridPreservation.PRESERVES_RECORDS);
        Grid applied = changeResult.getGrid();

        Assert.assertTrue(applied.getOverlayModels().get("wikibaseSchema") instanceof WikibaseSchema);
    }
}
