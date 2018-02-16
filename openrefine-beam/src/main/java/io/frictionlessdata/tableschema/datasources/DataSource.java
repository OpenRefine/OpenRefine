/*
 *
 */
package io.frictionlessdata.tableschema.datasources;

import java.util.Iterator;
import java.util.List;

/**
 *
 */
public interface DataSource {  
    public Iterator<String[]> iterator() throws Exception;
    public String[] getHeaders() throws Exception;
    public List<String[]> data() throws Exception;
    public void write(String outputFilePath) throws Exception;
}
