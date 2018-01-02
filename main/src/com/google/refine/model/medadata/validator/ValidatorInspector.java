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

import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.medadata.validator.checks.TypeorFormatError;
import com.google.refine.model.medadata.validator.checks.Validator;
import com.google.refine.util.JSONUtilities;

public class ValidatorInspector {
    private final static Logger logger = LoggerFactory.getLogger(ValidatorInspector.class);
    
    /**
     * Return a report contains the validate result
     * @param project
     * @param options
     * @return
     */
    public static JSONObject inspect(Project project, JSONObject options) {
        List<String> columnNames;
        String COLUMN_NAMES_KEY = "columnNames";
        Map<String, List<Validator>> columnToCheckersMap = new HashMap<String, List<Validator>>();
        JSONArray validateReport = new JSONArray();
        
        logger.info("starting inspect with options:" + options.toString());
        columnNames = JSONUtilities.toStringList(options.getJSONArray(COLUMN_NAMES_KEY));
        
        // build the check items
        List<Validator> validatorList = null;
        for(String columnName : columnNames) {
            validatorList = compileChecks(project, columnName, options);
            if (validatorList.size() >= 0)
                columnToCheckersMap.put(columnName, validatorList);
        }
        logger.info("==========================================================");
        logger.info("Inspector finished the checks compile. will do following check:");
        for (Entry<String, List<Validator>> entry : columnToCheckersMap.entrySet()) {
            logger.info("Column Name: " + entry.getKey());
            for (Validator v : entry.getValue()) {
                logger.info("\t Validator: " + v.getClass().getSimpleName());
            }
        }
        logger.info("==========================================================");

        // do the inspect in another loop:
        for(String columnName : columnNames) {
            List<Validator> validators = columnToCheckersMap.get(columnName);
            if (validators != null) {
                for (Validator validator : validators) {
                    JSONArray result = validator.validate();
                    if (result != null && result.length() > 0)
                        JSONUtilities.concatArray(validateReport, result);
                }
            }
        }
        logger.info("Inspector finished the validation.");
        
        return new JSONObject().put("validation-reports", (Object)validateReport);
    }

    private static List<Validator> compileChecks(Project project, String columnName, JSONObject options) {
        Map<String, Class> constraintHandlersMap = ValidatorRegistry.getInstance().getConstraintHandlersMap();
        
        Column column = project.columnModel.getColumnByName(columnName);
        List<Validator> validatorList = new ArrayList<Validator>();
        
        int columnIndex = project.columnModel.getColumnIndexByName(columnName);
        
        validatorList.add(new TypeorFormatError(project, columnIndex, options));
        
        
        if (column.getConstraints() != null) {
            for (Entry<String, Object> entry : column.getConstraints().entrySet()) {
                Class<Validator> clazz = constraintHandlersMap.get(entry.getKey());
                try {
                    Constructor<Validator> c = clazz.getConstructor(Project.class, int.class, JSONObject.class);
                    validatorList.add(c.newInstance(project, columnIndex, options));
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                    logger.error("failed to do compileChecks:" + ExceptionUtils.getStackTrace(e));
                }
            }
        }
        
        return validatorList;
    }
    
    
}
