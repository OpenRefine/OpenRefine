
package org.openrefine.importers;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.function.Supplier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.model.Cell;
import org.openrefine.model.Grid;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.model.recon.ReconCandidate;
import org.openrefine.model.recon.ReconConfig;
import org.openrefine.model.recon.StandardReconConfig;
import org.openrefine.util.ParsingUtilities;

public class LegacyProjectImporterTests extends ImporterTest {

    // System Under Test
    LegacyProjectImporter SUT = null;

    // dependencies
    private String reconConfigJson = "{"
            + "\"mode\":\"standard-service\","
            + "\"service\":\"https://wdreconcile.toolforge.org/en/api\","
            + "\"identifierSpace\":\"http://www.wikidata.org/entity/\","
            + "\"schemaSpace\":\"http://www.wikidata.org/prop/direct/\","
            + "\"autoMatch\":true,"
            + "\"columnDetails\":[],"
            + "\"limit\":0}";

    @Override
    @BeforeMethod
    public void setUp() {
        super.setUp();
        SUT = new LegacyProjectImporter();
        ReconConfig.registerReconConfig("core", "standard-service", StandardReconConfig.class);
    }

    @Test
    public void testLoadLegacyProject() throws Exception {
        Supplier<InputStream> stream = () -> this.getClass().getClassLoader()
                .getResourceAsStream("importers/legacy-openrefine-project.tar.gz");

        Grid grid = parseOneFile(SUT, stream);

        ReconCandidate match = new ReconCandidate("Q573", "day", null, 100.0);
        StandardReconConfig reconConfig = ParsingUtilities.mapper.readValue(reconConfigJson, StandardReconConfig.class);
        Recon matchedRecon = new Recon(1609493969067968688L, 1609494792472L, Judgment.Matched, match, null, Collections.emptyList(),
                reconConfig.service, reconConfig.identifierSpace, reconConfig.schemaSpace, "similar", -1);
        Recon unmatchedRecon = new Recon(1609493961679556613L, 1609494430802L, Judgment.None, null, null, Collections.emptyList(),
                reconConfig.service, reconConfig.identifierSpace, reconConfig.schemaSpace, "unknown", -1);

        Grid expected = createGrid(new String[] { "a", "b", "trim" },
                new Serializable[][] {
                        { "c", new Cell("d", matchedRecon), "d" },
                        { "e", new Cell("f", unmatchedRecon), "f" }
                });

        expected = expected.withColumnModel(expected.getColumnModel()
                .withReconConfig(1, reconConfig));

        assertGridEquals(grid, expected);
    }

}
