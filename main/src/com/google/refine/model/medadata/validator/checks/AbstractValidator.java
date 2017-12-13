package com.google.refine.model.medadata.validator.checks;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.medadata.validator.ValidatorSpec;

import io.frictionlessdata.tableschema.Field;


public abstract class AbstractValidator implements Validator {
    protected Project project;
    protected int cellIndex;
    protected JSONObject options;
    protected Field field;
    protected String code;
    
    protected JSONArray jsonErros = null;
    
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
        this.field = project.getSchema().getField(project.columnModel.getColumnNames().get(cellIndex));
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
        Map<String, String> data = new HashMap<String, String>(6);
        
        data.put("value", value);
        data.put("row_number", Integer.toString(rowIndex));
        data.put("column_number", Integer.toString(cellIndex));
        data.put("constraint", code);
        data.put("field_type", value);
        data.put("field_format", value);
        
        StrSubstitutor sub = new StrSubstitutor(data);
        String result = sub.replace(message);
        System.out.println("XXXXXXXX:" + result);
        return result;
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
