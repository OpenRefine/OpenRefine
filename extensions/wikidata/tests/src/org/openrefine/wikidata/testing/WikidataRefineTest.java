
package org.openrefine.wikidata.testing;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.testng.annotations.BeforeMethod;

import org.openrefine.ProjectManager;
import org.openrefine.ProjectManagerStub;
import org.openrefine.ProjectMetadata;
import org.openrefine.RefineServlet;
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

    /**
     * Initializes the importing options for the CSV importer.
     * 
     * @param options
     * @param sep
     * @param limit
     * @param skip
     * @param ignoreLines
     * @param headerLines
     * @param guessValueType
     * @param ignoreQuotes
     */
    public static void prepareImportOptions(ObjectNode options,
            String sep, int limit, int skip, int ignoreLines,
            int headerLines, boolean guessValueType, boolean ignoreQuotes) {

        whenGetStringOption("separator", options, sep);
        whenGetIntegerOption("limit", options, limit);
        whenGetIntegerOption("skipDataLines", options, skip);
        whenGetIntegerOption("ignoreLines", options, ignoreLines);
        whenGetIntegerOption("headerLines", options, headerLines);
        whenGetBooleanOption("guessCellValueTypes", options, guessValueType);
        whenGetBooleanOption("processQuotes", options, !ignoreQuotes);
        whenGetBooleanOption("storeBlankCellsAsNulls", options, true);
    }

    protected Project createCSVProject(String projectName, String input) {
        Project project = new Project();

        ProjectMetadata metadata = new ProjectMetadata();
        metadata.setName(projectName);

        ObjectNode options = mock(ObjectNode.class);
        prepareImportOptions(options, ",", -1, 0, 0, 1, false, false);

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

    // ----helpers----

    static public void whenGetBooleanOption(String name, ObjectNode options, Boolean def){
        when(options.has(name)).thenReturn(true);
        when(options.get(name)).thenReturn(def ? BooleanNode.TRUE : BooleanNode.FALSE);
    }

    static public void whenGetIntegerOption(String name, ObjectNode options, int def){
        when(options.has(name)).thenReturn(true);
        when(options.get(name)).thenReturn(new IntNode(def));
    }

    static public void whenGetStringOption(String name, ObjectNode options, String def){
        when(options.has(name)).thenReturn(true);
        when(options.get(name)).thenReturn(new TextNode(def));
    }
}
