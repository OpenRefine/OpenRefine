
package org.openrefine.commands.project;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;

import javax.servlet.ServletException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.ProjectManager;
import org.openrefine.ProjectManagerStub;
import org.openrefine.RefineServlet;
import org.openrefine.commands.CommandTestBase;
import org.openrefine.importers.LegacyProjectImporter;
import org.openrefine.importing.FormatRegistry;
import org.openrefine.importing.ImportingManager;
import org.openrefine.io.FileProjectManager;
import org.openrefine.model.Cell;
import org.openrefine.model.GridState;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.model.recon.ReconCandidate;
import org.openrefine.model.recon.ReconConfig;
import org.openrefine.model.recon.StandardReconConfig;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class ImportProjectCommandTests extends CommandTestBase {

    private String reconConfigJson = "{"
            + "\"mode\":\"standard-service\","
            + "\"service\":\"https://wdreconcile.toolforge.org/en/api\","
            + "\"identifierSpace\":\"http://www.wikidata.org/entity/\","
            + "\"schemaSpace\":\"http://www.wikidata.org/prop/direct/\","
            + "\"autoMatch\":true,"
            + "\"columnDetails\":[],"
            + "\"limit\":0}";

    private GridState expectedGrid = null;
    private File tempDir;

    @BeforeMethod
    public void setUpCommand() throws JsonParseException, JsonMappingException, IOException {
        command = new ImportProjectCommand();
        ReconConfig.registerReconConfig("core", "standard-service", StandardReconConfig.class);
        FormatRegistry.registerFormat("openrefine-legacy", "OpenRefine legacy project file", "uiClass", new LegacyProjectImporter());

        RefineServlet servlet = mock(RefineServlet.class);
        tempDir = TestUtils.createTempDirectory("openrefine-import-project-command-test");
        when(servlet.getTempDir()).thenReturn(tempDir);
        ImportingManager.initialize(servlet);
        // for this test we need a real project manager which actually imports the projects
        ProjectManager.singleton = null;
        FileProjectManager.initialize(runner(), tempDir);

        // Build expected grid
        ReconCandidate match = new ReconCandidate("Q573", "day", null, 100.0);
        StandardReconConfig reconConfig = ParsingUtilities.mapper.readValue(reconConfigJson, StandardReconConfig.class);
        Recon matchedRecon = new Recon(1609493969067968688L, 1609494792472L, Judgment.Matched, match, null, Collections.emptyList(),
                reconConfig.service, reconConfig.identifierSpace, reconConfig.schemaSpace, "similar", -1);
        Recon unmatchedRecon = new Recon(1609493961679556613L, 1609494430802L, Judgment.None, null, null, Collections.emptyList(),
                reconConfig.service, reconConfig.identifierSpace, reconConfig.schemaSpace, "unknown", -1);
        expectedGrid = createGrid(new String[] { "a", "b", "trim" },
                new Serializable[][] {
                        { "c", new Cell("d", matchedRecon), "d" },
                        { "e", new Cell("f", unmatchedRecon), "f" }
                });

        expectedGrid = expectedGrid.withColumnModel(expectedGrid.getColumnModel()
                .withReconConfig(1, reconConfig));
    }

    @AfterMethod
    public void cleanupTempDir() throws IOException {
        FileUtils.deleteDirectory(tempDir);
        // restore stub project manager
        ProjectManager.singleton = new ProjectManagerStub(runner());
    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }

    @Test
    public void testUploadCurrentProject() throws IOException {
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream("importers/openrefine-project.tar.gz");
        ImportProjectCommand SUT = (ImportProjectCommand) command;
        long projectId = SUT.importProject(stream, true);

        GridState grid = ProjectManager.singleton.getProject(projectId).getCurrentGridState();

        assertGridEquals(grid, expectedGrid);
    }

    @Test
    public void testUploadLegacyProject() throws IOException {
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream("importers/legacy-openrefine-project.tar.gz");
        // to bypass the move of project from importing directory to workspace
        ProjectManager.singleton = new ProjectManagerStub(runner());

        ImportProjectCommand SUT = (ImportProjectCommand) command;
        long projectId = SUT.importProject(stream, true);

        GridState grid = ProjectManager.singleton.getProject(projectId).getCurrentGridState();

        assertGridEquals(grid, expectedGrid);
    }
}
