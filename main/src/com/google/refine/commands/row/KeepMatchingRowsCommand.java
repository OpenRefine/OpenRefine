
package com.google.refine.commands.row;

import javax.servlet.http.HttpServletRequest;

import com.google.refine.browsing.EngineConfig;
import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.row.RowKeepMatchedOperation;

public class KeepMatchingRowsCommand extends EngineDependentCommand {

    @Override
    protected AbstractOperation createOperation(Project project, HttpServletRequest request, EngineConfig engineConfig) throws Exception {
        return new RowKeepMatchedOperation(engineConfig);
    }
}
