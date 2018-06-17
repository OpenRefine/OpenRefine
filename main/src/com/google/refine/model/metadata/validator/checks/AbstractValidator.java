package com.google.refine.model.metadata.validator.checks;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.metadata.validator.ValidatorSpec;

public abstract class AbstractValidator implements Validator {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    protected Project project;
    protected int cellIndex;
    protected JSONObject options;
    protected Column column;
    protected String code;
    
    protected JSONArray jsonErros = null;
    
    protected Map<String, String> lookup = new HashMap<String, String>(6);
    
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
        this.column = project.columnModel.getColumnByCellIndex(cellIndex);
    }
    
    @Override
    public JSONArray validate() {
        for (int rowIndex = 0;rowIndex < project.rows.size();rowIndex++) {
            Row row = project.rows.get(rowIndex);
            Cell cell = row.getCell(cellIndex);
            if (filter(cell))
                continue;
            
            boolean checkResult = checkCell(cell);
            if (!checkResult) {
                addError(formatErrorMessage(cell, rowIndex + 1));
            }
        }
        return jsonErros;
    }
    
    @Override
    public JSONObject formatErrorMessage(Cell cell, int rowIndex) {
        String message = null;
        message = ValidatorSpec.getInstance().getMessage(code);
        String formattedMessage = format(message, cell.value.toString(), rowIndex, cellIndex, code);
        JSONObject json = new JSONObject();
        json.put("code", code);
        json.put("message", formattedMessage);
        json.put("row-number", rowIndex);
        json.put("column-number", cellIndex);
        
        return json;
    }

    /**
     * MessageFormat.format cannot take the named parameters.
     * @param message
     * @param value
     * @param rowIndex
     * @param cellIndex
     * @param code
     * @return
     */
    private String format(String message, String value, int rowIndex, int cellIndex, String code) {
        lookup.put("value", value);
        lookup.put("row_number", Integer.toString(rowIndex));
        lookup.put("column_number", Integer.toString(cellIndex));
        lookup.put("constraint", code);
        customizedFormat();
        
        return new StrSubstitutor(lookup).replace(message);
    }
    
    /*
     * Empty body since default there is no customized Format
     * @see com.google.refine.model.metadata.validator.checks.Validator#customizedFormat()
     */
    @Override
    public void customizedFormat() {
    }
    
    /**
     * will skip the cell if return true
     */
    @Override
    public boolean filter(Cell cell) {
        return cell == null || cell.value == null;
    }
    
    @Override
    public boolean checkCell(Cell cell) {
        return false;
    }
    
    @Override
    public void addError(JSONObject result) {
        if (jsonErros == null)
            jsonErros = new JSONArray();
        
        jsonErros.put(result);
    }

}
