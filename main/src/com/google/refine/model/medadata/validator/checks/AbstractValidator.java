package com.google.refine.model.medadata.validator.checks;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;


public abstract class AbstractValidator implements Validator {
    protected Project project;
    protected int cellIndex;
    protected JSONObject options;
    
    protected JSONArray jsonErros = new JSONArray();
    
    /**
     * Constructor
     * @param project
     * @param cellIndex
     * @param options
     */
    public AbstractValidator(Project project, int cellIndex, JSONObject options) {
        this.project = project;
        this.cellIndex = cellIndex;
        this.options = options;
    }
    
    protected AbstractValidator() {
        
    }
    
    @Override
    public JSONArray validate() {
        for (Row row : project.rows) {
            Cell cell = row.getCell(cellIndex);
            if (filter(cell))
                continue;
            
            boolean checkResult = checkCell(cell);
            if (!checkResult) {
                addError(formatErrorMessage(cell));
            }
        }
        return null;
    }
    
    @Override
    public JSONObject formatErrorMessage(Cell cell) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public boolean filter(Cell cell) {
        return false;
    }
    
    @Override
    public boolean checkCell(Cell cell) {
        return false;
    }
    
    @Override
    public void addError(JSONObject result) {
        this.jsonErros.put(result);
    }

}
