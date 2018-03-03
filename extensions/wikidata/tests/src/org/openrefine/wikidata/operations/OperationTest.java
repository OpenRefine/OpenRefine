package org.openrefine.wikidata.operations;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

import org.json.JSONObject;
import org.json.JSONWriter;
import org.openrefine.wikidata.testing.JacksonSerializationTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.refine.history.Change;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.tests.RefineTest;
import com.google.refine.util.Pool;

import edu.mit.simile.butterfly.ButterflyModule;

public abstract class OperationTest extends RefineTest {
    
    protected Project project = null;
    protected ButterflyModule module = null;
    protected Pool pool = null;
    
    @BeforeMethod
    public void setUp() {
        project = createCSVProject("a,b\nc,d");
        module = mock(ButterflyModule.class);
        when(module.getName()).thenReturn("wikidata");
        pool = new Pool();
    }
    
    protected void registerOperation(String name, Class klass) {
        OperationRegistry.registerOperation(module, name, klass);
    }
    
    public abstract AbstractOperation reconstruct() throws Exception;
    
    public abstract JSONObject getJson() throws Exception;
    
    @Test
    public void testReconstruct() throws Exception {
        JSONObject json = getJson();
        AbstractOperation op = reconstruct();
        StringWriter writer = new StringWriter();
        JSONWriter jsonWriter = new JSONWriter(writer);
        op.write(jsonWriter, new Properties());
        JacksonSerializationTest.assertJsonEquals(json.toString(), writer.toString());
    }
    
    protected LineNumberReader makeReader(String input) {
        StringReader reader = new StringReader(input);
        return new LineNumberReader(reader);
    }
    
    protected String saveChange(Change change) throws IOException {
        StringWriter writer = new StringWriter();
        change.save(writer, new Properties());
        return writer.toString();
    }

}
