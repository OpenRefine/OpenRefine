/**
 * 
 */
package com.metaweb.gridworks.model.changes;

import java.util.List;

import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.ReconStats;
import com.metaweb.gridworks.model.recon.ReconConfig;

public class ReconChange extends MassCellChange {
    private static final long serialVersionUID = 7048806528587330543L;
    
    final protected ReconConfig _newReconConfig;
    protected ReconStats _newReconStats;
    
    protected ReconConfig _oldReconConfig;
    protected ReconStats _oldReconStats;
    
    public ReconChange(
        List<CellChange> 	cellChanges,
        String 				commonColumnName,
        ReconConfig 		newReconConfig,
        ReconStats			newReconStats // can be null
    ) {
        super(cellChanges, commonColumnName, false);
        _newReconConfig = newReconConfig;
        _newReconStats = newReconStats;
    }
    
    public ReconChange(
        CellChange	 	cellChange,
        String 			commonColumnName,
        ReconConfig 	newReconConfig,
        ReconStats		newReconStats // can be null
    ) {
        super(cellChange, commonColumnName, false);
        _newReconConfig = newReconConfig;
        _newReconStats = newReconStats;
    }
    
    @Override
    public void apply(Project project) {
        synchronized (project) {
            super.apply(project);
            
            Column column = project.columnModel.getColumnByName(_commonColumnName);
            
            if (_newReconStats == null) {
            	_newReconStats = ReconStats.create(project, column.getCellIndex());
            }
            
            _oldReconConfig = column.getReconConfig();
            _oldReconStats = column.getReconStats();
            
            column.setReconConfig(_newReconConfig);
            column.setReconStats(_newReconStats);
        }
    }
    
    @Override
    public void revert(Project project) {
        synchronized (project) {
            super.revert(project);
            
            Column column = project.columnModel.getColumnByName(_commonColumnName);
            column.setReconConfig(_oldReconConfig);
            column.setReconStats(_oldReconStats);
        }
    }
}