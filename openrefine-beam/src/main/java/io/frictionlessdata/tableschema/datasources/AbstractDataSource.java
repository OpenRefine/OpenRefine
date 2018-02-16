package io.frictionlessdata.tableschema.datasources;

import java.util.Iterator;
import java.util.List;

/**
 *
 * 
 */
public abstract class AbstractDataSource implements DataSource {

    @Override
    abstract public Iterator<String[]> iterator() throws Exception;
    
    @Override
    abstract public String[] getHeaders() throws Exception;
    
    @Override
    abstract public List<String[]> data() throws Exception;
    
    @Override
    abstract public void write(String outputFilePath) throws Exception;
}
