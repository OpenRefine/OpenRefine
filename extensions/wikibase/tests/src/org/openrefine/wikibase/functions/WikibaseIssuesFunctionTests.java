
package org.openrefine.wikibase.functions;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import org.openrefine.wikibase.manifests.Manifest;
import org.openrefine.wikibase.manifests.ManifestException;
import org.openrefine.wikibase.manifests.ManifestParser;
import org.openrefine.wikibase.schema.WikibaseSchema;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.refine.ProjectManager;
import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.ParsingUtilities;

public class WikibaseIssuesFunctionTests extends RefineTest {

    String schemaJson = "{\"entityEdits\":[{\"type\":\"wbitemeditexpr\",\"subject\":"
            + "{\"type\":\"wbentityidvalueconstant\",\"id\":\"Q4115189\",\"label\":\"Fancy Sandbox\"},"
            + "\"nameDescs\":[{\"name_type\":\"LABEL_IF_NEW\",\"value\":{\"type\":\"wbmonolingualexpr\","
            + "\"language\":{\"type\":\"wblanguageconstant\",\"id\":\"en\",\"label\":\"en\"},"
            + "\"value\":{\"type\":\"wbstringconstant\",\"value\":\"my  label\"}}}],\"statementGroups\":[]}],"
            + "\"siteIri\":\"http://www.my.fancy.wiki/entity/\","
            + "\"entityTypeSiteIRI\":{\"item\":\"http://www.my.fancy.wiki/entity/\",\"property\":\"http://www.my.fancy.wiki/entity/\"},"
            + "\"mediaWikiApiEndpoint\":\"https://www.my.fancy.wiki/w/api.php\"}";

    String manifestJson = "{\"version\":\"1.0\",\"mediawiki\":{\"name\":\"Fancy\",\"root\":\"https://www.my.fancy.wiki/wiki/\","
            + "\"main_page\":\"https://www.my.fancy.wiki/wiki/Fancy:Main_Page\",\"api\":\"https://www.my.fancy.wiki/w/api.php\"},"
            + "\"wikibase\":{\"site_iri\":\"http://www.my.fancy.wiki/entity/\",\"maxlag\":5,\"properties\":{\"instance_of\":\"P31\",\"subclass_of\":\"P279\"},"
            + "\"constraints\":{}},\"oauth\":{\"registration_page\":\"https://fancy.org/\"},"
            + "\"reconciliation\":{\"endpoint\":\"https://fancyrecon.link/${lang}/api\"},"
            + "\"editgroups\":{\"url_schema\":\"([[:toollabs:editgroups/b/OR/${batch_id}|details]])\"}}";

    WikibaseSchema schema;
    Manifest manifest;
    Project project;
    Row row;
    int rowId;
    Properties bindings;

    WikibaseIssuesFunction SUT = new WikibaseIssuesFunction();

    @BeforeMethod
    public void setUpDependencies() throws IOException, ManifestException {
        ControlFunctionRegistry.registerFunction("wikibaseIssues", SUT);

        schema = WikibaseSchema.reconstruct(schemaJson);
        manifest = ManifestParser.parse(manifestJson);
        project = createCSVProject("my project",
                "a,b\nc,d\ne,f");
        project.overlayModels.put("wikibaseSchema", schema);
        ProjectManager.singleton.getPreferenceStore().put("wikibase.manifests", ParsingUtilities.mapper.readTree("[" + manifestJson + "]"));
        row = project.rows.get(0);
        rowId = 0;
        bindings = ExpressionUtils.createBindings(project);
        ExpressionUtils.bind(bindings, row, rowId, "a", row.getCell(0));
    }

    @Test
    public void testGenerateIssues() {
        Object returnValue = SUT.call(bindings, new Object[] {});

        assertEquals(returnValue, Collections.singletonList("duplicate-whitespace"));
    }

    @Test
    public void testNoSchema() {
        project.overlayModels.remove("wikibaseSchema");

        Object returnValue = SUT.call(bindings, new Object[] {});
        assertEquals(returnValue, new EvalError("No wikibase schema associated with this project"));
    }

    @Test
    public void testInvalidArguments() {
        Object returnValue = SUT.call(bindings, new Object[] { 1, 2, 3 });
        assertEquals(returnValue, new EvalError("wikibaseIssues() does not expect any arguments"));
    }

    @Test
    public void testNoManifest() throws JsonMappingException, JsonProcessingException {
        ProjectManager.singleton.getPreferenceStore().put("wikibase.manifests", ParsingUtilities.mapper.readTree("[]"));

        Object returnValue = SUT.call(bindings, new Object[] {});
        assertEquals(returnValue, new EvalError("No Wikibase manifest found for MediaWiki API URL https://www.my.fancy.wiki/w/api.php"));
    }
}
