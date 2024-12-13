
package com.google.refine.commands.row;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.type.TypeReference;

import com.google.refine.browsing.EngineConfig;
import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.row.RowDuplicatesRemovalOperation;
import com.google.refine.util.ParsingUtilities;

public class RemoveDuplicateRowsCommand extends EngineDependentCommand {

    @Override
    protected AbstractOperation createOperation(Project project,
            HttpServletRequest request, EngineConfig engineConfig) throws Exception {

        String duplicateCriteria = request.getParameter("criteria");
        return new RowDuplicatesRemovalOperation(engineConfig,
                ParsingUtilities.mapper.readValue(duplicateCriteria, new TypeReference<List<String>>() {
                }));
    }
}
