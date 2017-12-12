package com.google.refine.model.medadata.validator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.model.Project;
import com.google.refine.model.medadata.validator.checks.TypeorFormatError;
import com.google.refine.model.medadata.validator.checks.Validator;
import com.google.refine.util.JSONUtilities;

import io.frictionlessdata.tableschema.Field;

public class ValidatorInspector {
    final static Logger logger = LoggerFactory.getLogger(ValidatorInspector.class);
    
    /**
     * Return a report contains the validate result
     * @param project
     * @param options
     * @return
     */
    static JSONObject inspect(Project project, JSONObject options) {
        List<String> columnNames;
        String COLUMN_NAMES_KEY = "columnNames";
        Map<String, List<Validator>> columnToCheckersMap = new HashMap<String, List<Validator>>();
        JSONArray validateReport = new JSONArray();
        
        columnNames = JSONUtilities.toStringList(options.getJSONArray(COLUMN_NAMES_KEY));
        
        // build the check items
        boolean compilePass = false;
        for(String columnName : columnNames) {
            compilePass = compileChecks(project, columnName, options);
        }
        
        if (!compilePass)
            return null;
        
        // do the inspect in another loop:
        for(String columnName : columnNames) {
            for (Validator validator : columnToCheckersMap.get(columnName)) {
                JSONArray result = validator.validate();
                JSONUtilities.concatArray(validateReport, result);
            }
        }
        
        return new JSONObject(validateReport);
    }

    private static boolean compileChecks(Project project, String columnName, JSONObject options) {
        boolean result = true;
        Map<String, Class> constraintHandlersMap = ValidatorRegistry.getInstance().getConstraintHandlersMap();
        
        Field field = project.getSchema().getField(columnName);
        List<Validator> validatorList = new ArrayList<Validator>();
        
        int columnIndex = project.columnModel.getColumnIndexByName(columnName);
        
        validatorList.add(new TypeorFormatError(project, columnIndex, options));
        for (Entry<String, Object> entry : field.getConstraints().entrySet()) {
            Class<Validator> clazz = constraintHandlersMap.get(entry.getKey());
            try {
                options.put("constraint", new JSONObject().put(entry.getKey(), entry.getValue()));
                Constructor<Validator> c = clazz.getConstructor(Project.class, String.class, JSONObject.class);
                validatorList.add(c.newInstance(project, columnIndex, options));
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                logger.error("failed to do compileChecks:" + ExceptionUtils.getStackTrace(e));
                result = false;
            }
        }
        
        return result;
    }
    
    
}
