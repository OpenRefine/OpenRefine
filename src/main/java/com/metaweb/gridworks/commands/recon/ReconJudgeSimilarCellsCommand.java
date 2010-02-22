package com.metaweb.gridworks.commands.recon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONWriter;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Recon;
import com.metaweb.gridworks.model.ReconCandidate;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.changes.CellChange;
import com.metaweb.gridworks.model.changes.MassCellChange;
import com.metaweb.gridworks.process.QuickHistoryEntryProcess;

public class ReconJudgeSimilarCellsCommand extends Command {
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		try {
			Project project = getProject(request);
			Engine engine = getEngine(request, project);
			
			int rowIndex = Integer.parseInt(request.getParameter("row"));
			int cellIndex = Integer.parseInt(request.getParameter("cell"));
			String judgment = request.getParameter("judgment"); 

			JudgeSimilarCellsProcess process = null;
			
			if (judgment != null) {
				process = new JudgeSimilarCellsProcess(
					project, 
					engine,
					"Judge one cell's recon result",
					judgment,
					rowIndex, 
					cellIndex, 
					request.getParameter("candidate")
				);
			} else {
				ReconCandidate match = new ReconCandidate(
					request.getParameter("topicID"),
					request.getParameter("topicGUID"),
					request.getParameter("topicName"),
					request.getParameter("types").split(","),
					100
				);
				
				process = new JudgeSimilarCellsProcess(
					project, 
					engine,
					"Judge one cell's recon result",
					rowIndex, 
					cellIndex, 
					match
				);
			}
			
			boolean done = project.processManager.queueProcess(process);
			if (done) {
				JSONWriter writer = new JSONWriter(response.getWriter());
				writer.object();
				writer.key("code"); writer.value("ok");
				writer.endObject();
			} else {
				respond(response, "{ \"code\" : \"pending\" }");
			}
		} catch (Exception e) {
			respondException(response, e);
		}
	}
	
	protected class JudgeSimilarCellsProcess extends QuickHistoryEntryProcess {
	    final Engine engine;
		final int rowIndex;
		final int cellIndex;
		final String judgment;
		final String candidateID;
		final ReconCandidate match;
		
		JudgeSimilarCellsProcess(Project project, Engine engine, String briefDescription, String judgment, int rowIndex, int cellIndex, String candidateID) {
			super(project, briefDescription);
			this.engine = engine;
			this.rowIndex = rowIndex;
			this.cellIndex = cellIndex;
			this.judgment = judgment;
			this.candidateID = candidateID;
			this.match = null;
		}
		
		JudgeSimilarCellsProcess(Project project, Engine engine, String briefDescription, int rowIndex, int cellIndex, ReconCandidate match) {
			super(project, briefDescription);
			this.engine = engine;
			this.rowIndex = rowIndex;
			this.cellIndex = cellIndex;
			this.judgment = null;
			this.candidateID = null;
			this.match = match;
		}

		protected HistoryEntry createHistoryEntry() throws Exception {
			Cell cell = _project.rows.get(rowIndex).getCell(cellIndex);
			if (cell == null || ExpressionUtils.isBlank(cell.value)) {
				throw new Exception("Cell is blank");
			}
			
			Column column = _project.columnModel.getColumnByCellIndex(cellIndex);
			if (column == null) {
				throw new Exception("No such column");
			}
			
	        List<CellChange> cellChanges = new ArrayList<CellChange>(_project.rows.size());
            String similarValue = cell.value.toString();
	        
	        RowVisitor rowVisitor = new RowVisitor() {
	            List<CellChange> cellChanges;
	            String similarValue;
	            
	            public RowVisitor init(List<CellChange> cellChanges, String similarValue) {
	                this.cellChanges = cellChanges;
	                this.similarValue = similarValue;
	                return this;
	            }
	            
	            public boolean visit(Project project, int rowIndex, Row row, boolean contextual) {
	                Cell cell = row.getCell(cellIndex);
	                if (cell != null && !ExpressionUtils.isBlank(cell.value) && similarValue.equals(cell.value)) {
	                    Cell newCell = new Cell(
                            cell.value, 
                            cell.recon == null ? new Recon() : cell.recon.dup()
                        );
	                    
	                    if (match != null) {
	                        newCell.recon.judgment = Recon.Judgment.Matched;
	                        newCell.recon.match = match;
	                    } else {
	                        if ("match".equals(judgment)) {
	                            ReconCandidate match = null;
	                            
	                            if (cell.recon != null) {
	                                for (ReconCandidate c : cell.recon.candidates) {
	                                    if (candidateID.equals(c.topicID)) {
	                                        match = c;
	                                        break;
	                                    }
	                                }
	                            }
	                            if (match == null) {
	                                return false;
	                            }
	                            
	                            newCell.recon.judgment = Recon.Judgment.Matched;
	                            newCell.recon.match = match;
	                        } else if ("new".equals(judgment)) {
	                            newCell.recon.judgment = Recon.Judgment.New;
	                        } else if ("discard".equals(judgment)) {
	                            newCell.recon.judgment = Recon.Judgment.None;
	                            newCell.recon.match = null;
	                        }
	                    }
	                    
                        CellChange cellChange = new CellChange(rowIndex, cellIndex, cell, newCell);
                        cellChanges.add(cellChange);
	                }
	                return false;
	            }
	        }.init(cellChanges, similarValue);
	        
	        FilteredRows filteredRows = engine.getAllFilteredRows(false);
	        filteredRows.accept(_project, rowVisitor);
	        
	        String description = null;
            if (match != null) {
                description = "Match " + 
                    cellChanges.size() + " cell(s) in column " + 
                    column.getHeaderLabel() + " containing " +
                    similarValue + " to topic " + 
                    match.topicName + " (" +
                    match.topicID + ")";
            } else {
                if ("match".equals(judgment)) {
                    description = "Match " + 
                        cellChanges.size() + " cell(s) in column " + 
                        column.getHeaderLabel() + " containing " +
                        similarValue + " to topic " + 
                        candidateID;
                } else if ("new".equals(judgment)) {
                    description = "Mark to create new topic for " + 
                        cellChanges.size() + " cell(s) in column " + 
                        column.getHeaderLabel() + " containing " +
                        similarValue;
                } else if ("discard".equals(judgment)) {
                    description = "Discard recon judgments for " + 
                        cellChanges.size() + " cell(s) in column " + 
                        column.getHeaderLabel() + " containing " +
                        similarValue;
                }
            }
	        
	        MassCellChange massCellChange = new MassCellChange(
                cellChanges, column.getHeaderLabel(), false);
	        
	        return new HistoryEntry(_project, description, null, massCellChange);
		}
	}
}
