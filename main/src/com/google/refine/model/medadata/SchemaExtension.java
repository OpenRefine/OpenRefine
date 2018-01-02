package com.google.refine.model.medadata;

import com.google.refine.model.Column;
import com.google.refine.model.ColumnModel;

import io.frictionlessdata.datapackage.Resource;
import io.frictionlessdata.tableschema.Field;
import io.frictionlessdata.tableschema.Schema;

/** 
 * This class contains some methods which is not included in the official "table schema" repo for now.
 * Some methods can be removed after the official library provide the corresponding function.
 */
public class SchemaExtension {
    private static final String DEFAULT_RESOURCE_PATH = "data/";
    private static final String DEFAULT_RESOURCE_SUFFIX = ".csv";
    
    /**
     * insert the field to schema at specified position
     * @param schema
     * @param field
     * @param position
     */
    public static void insertField(Schema schema, Field field, int position) {
        schema.getFields().add(position, field);
    }
    
    /**
     * Remove the filed from the schema at specified position
     * @param schema
     * @param index
     * @return
     */
    public static Field removeField(Schema schema, int index) {
        return schema.getFields().remove(index);
    }
    
    /**
     * Create a resource by name, get the schema information from the ColumnModel
     * @param resourceName
     * @param columnModel
     * @return
     * @see ColumnModel
     */
    public static Resource createResource(String resourceName,  ColumnModel columnModel) {
        // populate the data package schema from the openrefine column model
        Schema schema = new Schema();
        for (Column column : columnModel.columns) {
            schema.addField(new Field(column.getName(),
                    column.getType(),
                    column.getFormat(),
                    column.getTitle(),
                    column.getDescription(),
                    column.getConstraints()));
        }
        
        Resource resource = new Resource(resourceName, 
                DEFAULT_RESOURCE_PATH + resourceName + DEFAULT_RESOURCE_SUFFIX,
                schema.getJson());
        
        return resource;
    }

}
