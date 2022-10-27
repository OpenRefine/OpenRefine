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

import static org.testng.Assert.assertEquals;

import java.io.LineNumberReader;

import org.openrefine.wikibase.testing.TestingData;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.browsing.EngineConfig;
import com.google.refine.history.Change;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Recon;
import com.google.refine.util.ParsingUtilities;

public class PerformWikibaseEditsOperationTest extends OperationTest {

    @BeforeMethod
    public void registerOperation() {
        registerOperation("perform-wikibase-edits", PerformWikibaseEditsOperation.class);
    }

    @Override
    public AbstractOperation reconstruct()
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
        new PerformWikibaseEditsOperation(EngineConfig.reconstruct("{}"), "", 5, "", 60, "tag");
    }

    @Test
    public void testLoadChange()
            throws Exception {
        String changeString = "newItems={\"qidMap\":{\"1234\":\"Q789\"}}\n" + "/ec/\n";
        LineNumberReader reader = makeReader(changeString);
        Change change = PerformWikibaseEditsOperation.PerformWikibaseEditsChange.load(reader, pool);

        project.rows.get(0).cells.set(0, TestingData.makeNewItemCell(1234L, "my new item"));

        change.apply(project);

        assertEquals(Recon.Judgment.Matched, project.rows.get(0).cells.get(0).recon.judgment);
        assertEquals("Q789", project.rows.get(0).cells.get(0).recon.match.id);

        change.revert(project);

        assertEquals(Recon.Judgment.New, project.rows.get(0).cells.get(0).recon.judgment);

        assertEquals(changeString, saveChange(change));
    }

}
