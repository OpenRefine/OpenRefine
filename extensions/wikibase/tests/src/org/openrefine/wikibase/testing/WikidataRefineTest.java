
package org.openrefine.wikibase.testing;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.testng.annotations.BeforeMethod;

import org.openrefine.ProjectManager;
import org.openrefine.ProjectManagerStub;
import org.openrefine.ProjectMetadata;
import org.openrefine.RefineServlet;
import org.openrefine.RefineServletStub;
import org.openrefine.RefineTest;
import org.openrefine.importers.SeparatorBasedImporter;
import org.openrefine.importing.ImportingJob;
import org.openrefine.importing.ImportingManager;
import org.openrefine.model.Project;

public class WikidataRefineTest {

    protected File workspaceDir;
    protected RefineServlet servlet;
    private List<Project> projects = new ArrayList<Project>();
    private List<ImportingJob> importingJobs = new ArrayList<ImportingJob>();

    public Project createCSVProject(String input) {
        return createCSVProject("test project", input);
    }

    protected Project createCSVProject(String projectName, String input) {
        Project project = new Project();

        ProjectMetadata metadata = new ProjectMetadata();
        metadata.setName(projectName);

        ObjectNode options = mock(ObjectNode.class);
        RefineTest.prepareImportOptions(options, ",", -1, 0, 0, 1, false, false);

        ImportingJob job = ImportingManager.createJob();

        SeparatorBasedImporter importer = new SeparatorBasedImporter();

        List<Exception> exceptions = new ArrayList<Exception>();
        importer.parseOneFile(project, metadata, job, "filesource", new StringReader(input), -1, options, exceptions);
        project.update();
        ProjectManager.singleton.registerProject(project, metadata);

        projects.add(project);
        importingJobs.add(job);
        return project;
    }

    @BeforeMethod(alwaysRun = true)
    public void initServlet() {
        servlet = new RefineServletStub();
        ProjectManager.singleton = new ProjectManagerStub();
        ImportingManager.initialize(servlet);
    }
}
