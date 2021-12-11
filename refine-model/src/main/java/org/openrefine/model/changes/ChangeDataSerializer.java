package org.openrefine.model.changes;

import java.io.IOException;
import java.io.Serializable;

/**
 * Defines how each item of a {@link ChangeData} object
 * is serialized as a string.
 * 
 * @author Antonin Delpeuch
 *
 * @param <T>
 */
public interface ChangeDataSerializer<T> extends Serializable {

    /**
     * Serialize a change data item to a string.
     * @param changeDataItem
     * @return
     */
    public String serialize(T changeDataItem);
    
    /**
     * Deserialize a change data item from a string.
     * @param serialized
     * @return
     * @throws IOException
     */
    public T deserialize(String serialized) throws IOException;
}
