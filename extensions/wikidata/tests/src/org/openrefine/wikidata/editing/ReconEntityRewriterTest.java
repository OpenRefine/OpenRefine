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

import static org.testng.Assert.assertEquals;

import org.openrefine.wikidata.schema.exceptions.NewItemNotCreatedYetException;
import org.openrefine.wikidata.testing.TestingData;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

public class ReconEntityRewriterTest {

	NewItemLibrary library = null;
	ReconEntityRewriter rewriter = null;
	ItemIdValue newlyCreated = Datamodel.makeWikidataItemIdValue("Q1234");

	@BeforeMethod
	public void setUp() {
		library = new NewItemLibrary();
	}

	@Test(expectedExceptions = ReconEntityRewriter.MissingEntityIdFound.class)
	public void testNotCreatedYet() {
		rewriter = new ReconEntityRewriter(library, TestingData.newIdA);
		rewriter.copy(TestingData.newIdB);
	}

	@Test
	public void testSuccessfulRewrite() {
		rewriter = new ReconEntityRewriter(library, TestingData.newIdA);
		library.setQid(4567L, "Q1234");
		assertEquals(newlyCreated, rewriter.copy(TestingData.newIdB));
	}

	@Test
	public void testSubjectNotRewritten() {
		ItemIdValue subject = TestingData.newIdA;
		rewriter = new ReconEntityRewriter(library, subject);
		assertEquals(subject, rewriter.copy(subject));
	}

	@Test
	public void testSubjectRewritten() {
		ItemIdValue subject = TestingData.newIdB;
		library.setQid(4567L, "Q1234");
		rewriter = new ReconEntityRewriter(library, subject);
		assertEquals(newlyCreated, rewriter.copy(subject));
	}

	@Test
	public void testMatched() {
		rewriter = new ReconEntityRewriter(library, TestingData.newIdA);
		assertEquals(TestingData.matchedId, rewriter.copy(TestingData.matchedId));
	}

	@Test
	public void testRewriteCreate() throws NewItemNotCreatedYetException {
		ItemIdValue subject = TestingData.newIdA;
		rewriter = new ReconEntityRewriter(library, subject);
		library.setQid(4567L, "Q1234");
		ItemUpdate update = new ItemUpdateBuilder(subject)
				.addStatement(TestingData.generateStatement(subject, TestingData.newIdB))
				.deleteStatement(TestingData.generateStatement(subject, TestingData.existingId))
				.addLabel(Datamodel.makeMonolingualTextValue("label", "de"), true)
				.addDescription(Datamodel.makeMonolingualTextValue("beschreibung", "de"), false)
				.addAlias(Datamodel.makeMonolingualTextValue("darstellung", "de")).build();
		ItemUpdate rewritten = rewriter.rewrite(update);
		ItemUpdate expected = new ItemUpdateBuilder(subject)
				.addStatement(TestingData.generateStatement(subject, newlyCreated))
				.deleteStatement(TestingData.generateStatement(subject, TestingData.existingId))
				.addLabel(Datamodel.makeMonolingualTextValue("label", "de"), true)
				.addDescription(Datamodel.makeMonolingualTextValue("beschreibung", "de"), false)
				.addAlias(Datamodel.makeMonolingualTextValue("darstellung", "de")).build();
		assertEquals(rewritten, expected);
	}

	@Test
	public void testRewriteUpdateOnPreviouslyCreatedEntity() throws NewItemNotCreatedYetException {
		ItemIdValue subject = TestingData.newIdA;
		rewriter = new ReconEntityRewriter(library, subject);
		library.setQid(4567L, "Q1234");
		ItemUpdate update = new ItemUpdateBuilder(TestingData.newIdB)
				.addDescription(Datamodel.makeMonolingualTextValue("beschreibung", "de"), false)
				.addAlias(Datamodel.makeMonolingualTextValue("darstellung", "de")).build();
		ItemUpdate rewritten = rewriter.rewrite(update);
		ItemUpdate expected = new ItemUpdateBuilder(newlyCreated)
				.addDescription(Datamodel.makeMonolingualTextValue("beschreibung", "de"), false)
				.addAlias(Datamodel.makeMonolingualTextValue("darstellung", "de")).build();
		assertEquals(rewritten, expected);
	}

	@Test
	public void testRewriteUpdateOnExistingEntity() throws NewItemNotCreatedYetException {
		ItemIdValue subject = TestingData.matchedId;
		rewriter = new ReconEntityRewriter(library, subject);
		library.setQid(4567L, "Q1234");
		ItemUpdate update = new ItemUpdateBuilder(subject)
				.addStatement(TestingData.generateStatement(subject, TestingData.newIdB))
				.deleteStatement(TestingData.generateStatement(subject, TestingData.existingId))
				.addLabel(Datamodel.makeMonolingualTextValue("label", "de"), true)
				.addDescription(Datamodel.makeMonolingualTextValue("beschreibung", "de"), false)
				.addAlias(Datamodel.makeMonolingualTextValue("darstellung", "de")).build();
		ItemUpdate rewritten = rewriter.rewrite(update);
		ItemUpdate expected = new ItemUpdateBuilder(subject)
				.addStatement(TestingData.generateStatement(subject, newlyCreated))
				.deleteStatement(TestingData.generateStatement(subject, TestingData.existingId))
				.addLabel(Datamodel.makeMonolingualTextValue("label", "de"), true)
				.addDescription(Datamodel.makeMonolingualTextValue("beschreibung", "de"), false)
				.addAlias(Datamodel.makeMonolingualTextValue("darstellung", "de")).build();
		assertEquals(rewritten, expected);
	}
}
