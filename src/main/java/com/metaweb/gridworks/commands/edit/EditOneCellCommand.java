package com.metaweb.gridworks.commands.edit;

import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONWriter;

import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.changes.CellChange;
import com.metaweb.gridworks.process.QuickHistoryEntryProcess;
import com.metaweb.gridworks.util.ParsingUtilities;
import com.metaweb.gridworks.util.Pool;

public class EditOneCellCommand extends Command {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            request.setCharacterEncoding("UTF-8");
            response.setCharacterEncoding("UTF-8");
            
            Project project = getProject(request);
            
            int rowIndex = Integer.parseInt(request.getParameter("row"));
            int cellIndex = Integer.parseInt(request.getParameter("cell"));
            
            String type = request.getParameter("type");
            String valueString = request.getParameter("value");
            Serializable value = null;
            
            if ("number".equals(type)) {
                value = Double.parseDouble(valueString);
            } else if ("boolean".equals(type)) {
                value = "true".equalsIgnoreCase(valueString);
            } else if ("date".equals(type)) {
                value = ParsingUtilities.stringToDate(valueString);
            } else {
                value = valueString;
            }

            EditOneCellProcess process = new EditOneCellProcess(
                project, 
                "Edit single cell",
                rowIndex, 
                cellIndex, 
                value
            );
            
            HistoryEntry historyEntry = project.processManager.queueProcess(process);
            if (historyEntry != null) {
                /*
                 * If the operation has been done, return the new cell's data
                 * so the client side can update the cell's rendering right away.
                 */
                JSONWriter writer = new JSONWriter(response.getWriter());
                
                Pool pool = new Pool();
                Properties options = new Properties();
                options.put("pool", pool);
                
                writer.object();
                writer.key("code"); writer.value("ok");
                writer.key("historyEntry"); historyEntry.write(writer, options);
                writer.key("cell"); process.newCell.write(writer, options);
                writer.key("pool"); pool.write(writer, options);
                writer.endObject();
            } else {
                respond(response, "{ \"code\" : \"pending\" }");
            }
        } catch (Exception e) {
            respondException(response, e);
        }
    }
    
    protected static class EditOneCellProcess extends QuickHistoryEntryProcess {
        final int rowIndex;
        final int cellIndex;
        final Serializable value;
        Cell newCell;
        
        EditOneCellProcess(
            Project project, 
            String briefDescription, 
            int rowIndex, 
            int cellIndex, 
            Serializable value
        ) {
            super(project, briefDescription);
            
            this.rowIndex = rowIndex;
            this.cellIndex = cellIndex;
            this.value = value;
        }

        protected HistoryEntry createHistoryEntry(long historyEntryID) throws Exception {
            Cell cell = _project.rows.get(rowIndex).getCell(cellIndex);
            Column column = _project.columnModel.getColumnByCellIndex(cellIndex);
            if (column == null) {
                throw new Exception("No such column");
            }
            
            newCell = new Cell(
                value, 
                cell != null ? cell.recon : null
            );
            
            String description = 
                "Edit single cell on row " + (rowIndex + 1) + 
                ", column " + column.getName();

            Change change = new CellChange(rowIndex, cellIndex, cell, newCell);
                
            return new HistoryEntry(
                historyEntryID, _project, description, null, change);
        }
    }
}
