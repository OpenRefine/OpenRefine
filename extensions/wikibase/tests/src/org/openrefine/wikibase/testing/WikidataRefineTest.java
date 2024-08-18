
package org.openrefine.wikibase.testing;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeMethod;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectManagerStub;
import com.google.refine.ProjectMetadata;
import com.google.refine.RefineServlet;
import com.google.refine.RefineServletStub;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingManager;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class WikidataRefineTest {

    protected File workspaceDir;
    protected RefineServlet servlet;
    private List<Project> projects = new ArrayList<Project>();
    private List<ImportingJob> importingJobs = new ArrayList<ImportingJob>();

    /**
     * Utility method to create a project with pre-defined contents.
     * 
     * @param columnNames
     *            names of the columns
     * @param grid
     *            contents of the project grid, which can be either {@link Cell} instances or just the cell values (for
     *            convenience)
     * @return a test project with the given contents
     */
    public Project createProject(String[] columnNames, Serializable[][] grid) {
        return createProject("test project", columnNames, grid);
    }

    /**
     * Utility method to create a project with pre-defined contents.
     * 
     * @param name
     *            project name
     * @param columnNames
     *            names of the columns
     * @param grid
     *            contents of the project grid, which can be either {@link Cell} instances or just the cell values (for
     *            convenience)
     * @return a test project with the given contents
     */
    public Project createProject(String name, String[] columnNames, Serializable[][] grid) {
        Project project = new Project();
        ProjectMetadata pm = new ProjectMetadata();
        pm.setName(name);
        ProjectManager.singleton.registerProject(project, pm);

        try {
            for (String columnName : columnNames) {
                int index = project.columnModel.allocateNewCellIndex();
                Column column = new Column(index, columnName);
                project.columnModel.addColumn(index, column, true);
            }
        } catch (ModelException e) {
            fail("The column names provided to create a test project contain duplicates");
        }
        for (Serializable[] rawRow : grid) {
            assertEquals(columnNames.length, rawRow.length, "Unexpected row length in test project");
            Row row = new Row(columnNames.length);
            for (int i = 0; i != columnNames.length; i++) {
                Serializable rawCell = rawRow[i];
                if (rawCell == null || rawCell instanceof Cell) {
                    row.setCell(i, (Cell) rawCell);
                } else {
                    row.setCell(i, new Cell(rawCell, null));
                }
            }
            project.rows.add(row);
        }
        project.update();
        projects.add(project);
        return project;
    }

    @BeforeMethod(alwaysRun = true)
    public void initServlet() {
        servlet = new RefineServletStub();
        ProjectManager.singleton = new ProjectManagerStub();
        ImportingManager.initialize(servlet);
    }
}
