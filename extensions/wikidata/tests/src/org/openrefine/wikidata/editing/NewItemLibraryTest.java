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
package org.openrefine.wikidata.editing;

import static org.junit.Assert.assertEquals;

import org.openrefine.wikidata.testing.JacksonSerializationTest;
import org.openrefine.wikidata.testing.TestingData;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.tests.RefineTest;

public class NewItemLibraryTest extends RefineTest {

    private NewItemLibrary library;

    @BeforeMethod
    public void setUp() {
        library = new NewItemLibrary();
        library.setQid(1234L, "Q345");
        library.setQid(3289L, "Q384");
    }

    @Test
    public void testRetrieveItem() {
        assertEquals("Q345", library.getQid(1234L));
    }

    @Test
    public void testUpdateReconciledCells() {
        Project project = createCSVProject(TestingData.inceptionWithNewCsv);
        project.rows.get(0).cells.set(0, TestingData.makeNewItemCell(3289L, "University of Ljubljana"));
        project.rows.get(1).cells.set(0, TestingData.makeMatchedCell("Q865528", "University of Warwick"));
        project.rows.get(2).cells.set(0, TestingData.makeNewItemCell(1234L, "new uni"));
        isNewTo(3289L, project.rows.get(0).cells.get(0));
        isMatchedTo("Q865528", project.rows.get(1).cells.get(0));
        isNewTo(1234L, project.rows.get(2).cells.get(0));
        library.updateReconciledCells(project, false);
        isMatchedTo("Q384", project.rows.get(0).cells.get(0));
        isMatchedTo("Q865528", project.rows.get(1).cells.get(0));
        isMatchedTo("Q345", project.rows.get(2).cells.get(0));
        library.updateReconciledCells(project, true);
        isNewTo(3289L, project.rows.get(0).cells.get(0));
        isMatchedTo("Q865528", project.rows.get(1).cells.get(0));
        isNewTo(1234L, project.rows.get(2).cells.get(0));
    }

    @Test
    public void testSerialize() {
        JacksonSerializationTest.canonicalSerialization(NewItemLibrary.class, library,
                "{\"qidMap\":{\"1234\":\"Q345\",\"3289\":\"Q384\"}}");
    }

    private void isMatchedTo(String qid, Cell cell) {
        assertEquals(Recon.Judgment.Matched, cell.recon.judgment);
        assertEquals(qid, cell.recon.match.id);
    }

    private void isNewTo(long id, Cell cell) {
        assertEquals(Recon.Judgment.New, cell.recon.judgment);
        assertEquals(id, cell.recon.id);
    }
}
