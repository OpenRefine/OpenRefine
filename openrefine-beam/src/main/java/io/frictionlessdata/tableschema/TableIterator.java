package io.frictionlessdata.tableschema;

import io.frictionlessdata.tableschema.exceptions.ConstraintsException;
import io.frictionlessdata.tableschema.exceptions.InvalidCastException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * 
 */
public class TableIterator<T> implements Iterator<T> {
    
    private String[] headers = null;
    private Schema schema = null;
    private Iterator<String[]> iter = null;
    private boolean keyed = false;
    private boolean extended = false; 
    private boolean cast = true;
    private boolean relations = false;
    private int index = 0;

    public TableIterator(Table table) throws Exception{
        this.init(table);
        this.headers = table.getHeaders();
        this.schema = table.getSchema();
        this.iter = table.getDataSource().iterator();
    }
    
    public TableIterator(Table table, boolean keyed) throws Exception{
        this.init(table);
        this.headers = table.getHeaders();
        this.schema = table.getSchema();
        this.iter = table.getDataSource().iterator();
        this.keyed = keyed;
    }
    
    public TableIterator(Table table, boolean keyed, boolean extended) throws Exception{
        this.init(table);
        this.headers = table.getHeaders();
        this.schema = table.getSchema();
        this.iter = table.getDataSource().iterator();
        this.keyed = keyed;
        this.extended = extended;
    }
    
    public TableIterator(Table table, boolean keyed, boolean extended, boolean cast) throws Exception{
        this.init(table);
        this.headers = table.getHeaders();
        this.schema = table.getSchema();
        this.iter = table.getDataSource().iterator();
        this.keyed = keyed;
        this.extended = extended;
        this.cast = cast;
    }
    
    public TableIterator(Table table, boolean keyed, boolean extended, boolean cast, boolean relations) throws Exception{
        this.init(table);
        this.keyed = keyed;
        this.extended = extended;
        this.cast = cast;
        this.relations = relations;
    }
    
    private void init(Table table) throws Exception{
        this.headers = table.getHeaders();
        this.schema = table.getSchema();
        this.iter = table.getDataSource().iterator();
    }
    
    @Override
    public boolean hasNext() {
        return this.iter.hasNext();
    }
    
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T next() {
        String[] row = this.iter.next();
        
        Map<String, Object> keyedRow = new HashMap();
        Object[] extendedRow = new Object[3];
        Object[] castRow = new Object[row.length];
        
        // If there's a schema, attempt to cast the row.
        if(this.schema != null){
            try{
                for(int i=0; i<row.length; i++){
                    Field field = this.schema.getFields().get(i);
                    Object val = field.castValue(row[i], true);

                    if(!extended && keyed){
                        keyedRow.put(this.headers[i], val);
                    }else{
                        castRow[i] = val;
                    } 
                }
                
                if(extended){
                    extendedRow = new Object[]{index, this.headers, castRow}; 
                    index++;
                    return (T)extendedRow;
                    
                }else if(keyed && !extended){
                    return (T)keyedRow;

                }else if(!keyed && !extended){
                    return (T)castRow;

                }else{
                    return (T)row;
                }
            
            }catch(InvalidCastException | ConstraintsException e){
                // The row data types do not match schema definition.
                // Or the row values do not respect the Constraint rules.
                // Do noting and string with String[] typed row.                
                return (T)row;
            }
            
        }else{
            // Enter here of no Schema has been defined.            
            if(extended){
                extendedRow = new Object[]{index, this.headers, row}; 
                index++;
                return (T)extendedRow;

            }else if(keyed && !extended){
                keyedRow = new HashMap();
                for(int i=0; i<row.length; i++){
                    keyedRow.put(this.headers[i], row[i]);
                }  
                return (T)keyedRow;

            }else{
                return (T)row;
            }
        }  
    }
}
