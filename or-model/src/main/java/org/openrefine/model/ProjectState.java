
package org.openrefine.model;

import org.apache.spark.api.java.JavaRDD;

/**
 * Immutable class which holds the state of a project at a given point in the workflow. The state consists of the lists
 * of columns with their metadata and the grid itself.
 * 
 * @author Antonin Delpeuch
 */
public class ProjectState {

    protected final ColumnModel columnModel;
    protected final JavaRDD<Row> grid;

    public ProjectState(ColumnModel columnModel, JavaRDD<Row> grid) {
        this.columnModel = columnModel;
        this.grid = grid;
    }
}
