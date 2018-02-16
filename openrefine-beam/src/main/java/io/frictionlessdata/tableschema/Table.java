package io.frictionlessdata.tableschema;

import io.frictionlessdata.tableschema.exceptions.TypeInferringException;
import io.frictionlessdata.tableschema.datasources.CsvDataSource;
import io.frictionlessdata.tableschema.datasources.DataSource;
import io.frictionlessdata.tableschema.exceptions.InvalidCastException;
import java.io.File;
import org.json.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * 
 */
public class Table{
    private DataSource dataSource = null;
    private Schema schema = null;
    
    public Table(File dataSource) throws Exception{
        this.dataSource = new CsvDataSource(dataSource);
    }
    
    public Table(File dataSource, JSONObject schema) throws Exception{
        this.dataSource = new CsvDataSource(dataSource);
        this.schema = new Schema(schema);
    }
    
    public Table(File dataSource, Schema schema) throws Exception{
        this.dataSource = new CsvDataSource(dataSource);
        this.schema = schema;
    }
    
    public Table(String dataSource) throws Exception{
        this.dataSource = new CsvDataSource(dataSource);
    }
    
    public Table(String dataSource, JSONObject schema) throws Exception{
        this.dataSource = new CsvDataSource(dataSource);
        this.schema = new Schema(schema);
    }
    
    public Table(String dataSource, Schema schema) throws Exception{
        this.dataSource = new CsvDataSource(dataSource);
        this.schema = schema;
    }
    
    public Table(URL dataSource) throws Exception{
        this.dataSource = new CsvDataSource(dataSource);
    }
    
    public Table(URL dataSource, JSONObject schema) throws Exception{
        this.dataSource = new CsvDataSource(dataSource);
        this.schema = new Schema(schema);
    }
    
    public Table(URL dataSource, Schema schema) throws Exception{
        this.dataSource = new CsvDataSource(dataSource);
        this.schema = schema;
    }
    
    public Table(URL dataSource, URL schema) throws Exception{
        this.dataSource = new CsvDataSource(dataSource);
        this.schema = new Schema(schema);
    }
    
    public Table(JSONArray dataSource) throws Exception{
        this.dataSource = new CsvDataSource(dataSource);
    }
    
    public Table(JSONArray dataSource, JSONObject schemaJson) throws Exception{
        this.dataSource = new CsvDataSource(dataSource);
        this.schema = new Schema(schemaJson);
    }
    
    public Table(JSONArray dataSource, Schema schema) throws Exception{
        this.dataSource = new CsvDataSource(dataSource);
        this.schema = schema;
    }
    
    public Iterator iterator() throws Exception{
       return new TableIterator(this);
    }
    
    public Iterator iterator(boolean keyed) throws Exception{
       return new TableIterator(this, keyed);
    }
    
    public Iterator iterator(boolean keyed, boolean extended) throws Exception{
       return new TableIterator(this, keyed, extended);
    }
    
    public Iterator iterator(boolean keyed, boolean extended, boolean cast) throws Exception{
       return new TableIterator(this, keyed, extended, cast);
    }
    
    public Iterator iterator(boolean keyed, boolean extended, boolean cast, boolean relations) throws Exception{
       return new TableIterator(this, keyed, extended, cast, relations);
    }
    
    public String[] getHeaders() throws Exception{
        return this.dataSource.getHeaders();
    }
    
    public void save(String outputFilePath) throws Exception{
       this.dataSource.write(outputFilePath);
    }
    public List<Object[]> read(boolean cast) throws Exception{
        if(cast && !this.schema.hasFields()){
            throw new InvalidCastException();
        }
        
        List<Object[]> rows = new ArrayList();
        
        Iterator<Object[]> iter = this.iterator(false, false, cast, false);
        while(iter.hasNext()){
            Object[] row = iter.next();
            rows.add(row);
        }

        return rows;
    }
    
    public List<Object[]> read() throws Exception{
        return this.read(false);
    }
    
    public Schema inferSchema() throws TypeInferringException{
        try{
            JSONObject schemaJson = TypeInferrer.getInstance().infer(this.read(), this.getHeaders());
            this.schema = new Schema(schemaJson);
            return this.schema;
            
        }catch(Exception e){
            throw new TypeInferringException();
        }
    }
    
    public Schema inferSchema(int rowLimit) throws TypeInferringException{
        try{
            JSONObject schemaJson = TypeInferrer.getInstance().infer(this.read(), this.getHeaders(), rowLimit);
            this.schema = new Schema(schemaJson);
            return this.schema;
            
        }catch(Exception e){
            throw new TypeInferringException();
        }
    }
    
    public Schema getSchema(){
        return this.schema;
    }
    
    public DataSource getDataSource(){
        return this.dataSource;
    }
}