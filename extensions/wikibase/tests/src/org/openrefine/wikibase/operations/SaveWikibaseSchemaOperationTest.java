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
import static org.testng.Assert.assertNull;

import java.io.LineNumberReader;

import org.openrefine.wikibase.schema.WikibaseSchema;
import org.openrefine.wikibase.testing.TestingData;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.history.Change;
import com.google.refine.model.AbstractOperation;
import com.google.refine.util.ParsingUtilities;

public class SaveWikibaseSchemaOperationTest extends OperationTest {

    @BeforeMethod
    public void registerOperation() {
        registerOperation("save-wikibase-schema", SaveWikibaseSchemaOperation.class);
    }

    @Override
    public AbstractOperation reconstruct()
            throws Exception {
        return ParsingUtilities.mapper.readValue(getJson(), SaveWikibaseSchemaOperation.class);
    }

    @Override
    public String getJson()
            throws Exception {
        return TestingData.jsonFromFile("operations/save-schema.json");
    }

    @Test
    public void testLoadChange()
            throws Exception {
        String schemaJson = TestingData.jsonFromFile("schema/inception.json");
        String changeString = "newSchema=" + schemaJson + "\n" + "oldSchema=\n" + "/ec/";
        WikibaseSchema schema = WikibaseSchema.reconstruct(schemaJson);

        LineNumberReader reader = makeReader(changeString);
        Change change = SaveWikibaseSchemaOperation.WikibaseSchemaChange.load(reader, pool);

        change.apply(project);

        assertEquals(schema,
                project.overlayModels.get(SaveWikibaseSchemaOperation.WikibaseSchemaChange.overlayModelKey));

        change.revert(project);

        assertNull(project.overlayModels.get(SaveWikibaseSchemaOperation.WikibaseSchemaChange.overlayModelKey));

        saveChange(change); // not checking for equality because JSON serialization varies
    }
}
