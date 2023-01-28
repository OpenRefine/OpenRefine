
package org.openrefine.model.changes;

import java.io.IOException;
import java.io.Serializable;

/**
 * Defines how each item of a {@link ChangeData} object is serialized as a string.
 * 
 *
 * @param <T>
 */
public interface ChangeDataSerializer<T> extends Serializable {

    /**
     * Serialize a change data item to a string.
     */
    public String serialize(T changeDataItem);

    /**
     * Deserialize a change data item from a string.
     */
    public T deserialize(String serialized) throws IOException;
}
