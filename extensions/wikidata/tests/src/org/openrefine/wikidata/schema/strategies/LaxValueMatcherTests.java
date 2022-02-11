package org.openrefine.wikidata.schema.strategies;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class LaxValueMatcherTests {
	
	LaxValueMatcher SUT = new LaxValueMatcher();
	
	@Test
	public void testQids() {
		EntityIdValue qid1 = Datamodel.makeItemIdValue("Q123", "https://foo.com");
		EntityIdValue qid2 = Datamodel.makeItemIdValue("Q123", "https://bar.com");
		EntityIdValue qid3 = Datamodel.makeItemIdValue("Q456", "https://foo.com");

		
		assertTrue(SUT.match(qid1, qid1));
		assertTrue(SUT.match(qid1, qid2));
		assertFalse(SUT.match(qid1, qid3));
	}
	
	@Test
	public void testUrls() {
		StringValue value1 = Datamodel.makeStringValue("https://gnu.org");
		StringValue value2 = Datamodel.makeStringValue("http://gnu.org/");
		StringValue value3 = Datamodel.makeStringValue("http://gnu.org/page");
		
		assertTrue(SUT.match(value1, value2));
		assertTrue(SUT.match(value1, value1));
		assertFalse(SUT.match(value2, value3));
	}
	
	@Test
	public void testWhitespace() {
		StringValue value1 = Datamodel.makeStringValue("foo");
		StringValue value2 = Datamodel.makeStringValue("\tfoo ");
		StringValue value3 = Datamodel.makeStringValue("bar");

		assertTrue(SUT.match(value1, value2));
		assertFalse(SUT.match(value1, value3));
	}
	
	@Test
	public void testMonolingualText() {
		MonolingualTextValue value1 = Datamodel.makeMonolingualTextValue("foo", "en");
		MonolingualTextValue value2 = Datamodel.makeMonolingualTextValue("\tfoo ", "en");
		MonolingualTextValue value3 = Datamodel.makeMonolingualTextValue("bar", "en");

		assertTrue(SUT.match(value1, value2));
		assertFalse(SUT.match(value1, value3));
	}
	
	@Test
	public void testEquality() {
		assertEquals(SUT, new LaxValueMatcher());
		assertNotEquals(SUT, new StrictValueMatcher());
	}
	
	@Test
	public void testHashCode() {
		assertEquals(SUT.hashCode(), new LaxValueMatcher().hashCode());
		assertNotEquals(SUT.hashCode(), new StrictValueMatcher().hashCode());
	}
	
	@Test
	public void testJsonSerialization() throws JsonProcessingException {
		TestUtils.assertEqualsAsJson(ParsingUtilities.mapper.writeValueAsString(SUT), "{\"type\":\"lax\"}");
	}

}
