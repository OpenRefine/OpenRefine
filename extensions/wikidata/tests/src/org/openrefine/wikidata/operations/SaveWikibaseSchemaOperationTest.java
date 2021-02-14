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

package org.openrefine.wikidata.operations;

import static org.mockito.Mockito.mock;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.model.GridState;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.Operation;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.wikidata.schema.WikibaseSchema;
import org.openrefine.wikidata.testing.TestingData;

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
        Change change = operation.createChange();
        ChangeContext context = mock(ChangeContext.class);

        GridState applied = change.apply(project.getCurrentGridState(), context);

        Assert.assertTrue(applied.getOverlayModels().get("wikibaseSchema") instanceof WikibaseSchema);
    }
}
