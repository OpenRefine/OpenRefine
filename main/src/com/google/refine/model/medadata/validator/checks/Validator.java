package com.google.refine.model.medadata.validator.checks;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.refine.model.Cell;

public interface Validator {
    /**
     * Given the options and cell index, apply the validate operation.
     * @return
     */
    public JSONArray validate();
    
    /**
     * Skip if cell is incomplete
     * @return
     */
    public boolean filter(Cell cell);
    
    /**
     * check the cell against the table schema
     * @param cell
     * @return false if fails the validation / check. Otherwise return true
     */
    public boolean checkCell(Cell cell);
    
    /**
     * Add error into the report for return
     */
    public void addError(JSONObject result);

    public JSONObject formatErrorMessage(Cell cell, int rowIndex);

    public void customizedFormat();
}
