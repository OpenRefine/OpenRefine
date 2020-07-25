package org.openrefine.model.changes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

/**
 * Helper class to represent an item in the map from
 * row ids to change values in the context of a {@class ChangeData}
 * object.
 * 
 * @author Antonin Delpeuch
 *
 * @param <T>
 */
public class IndexedData<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = 6928586351690626940L;
    
    private final long rowId;
    private final T data;
    
    public IndexedData(
            long rowId,
            T data) {
        this.rowId = rowId;
        this.data = data;
    }
    
    public long getId() {
        return rowId;
    }
    
    public T getData() {
        return data;
    }
    
    public void write(OutputStreamWriter os, ChangeDataSerializer<T> serializer) throws IOException {
        os.write(Long.toString(rowId));
        os.write(",");
        os.write(serializer.serialize(data));
        os.write("\n");
    }

    public String writeAsString(ChangeDataSerializer<T> serializer) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(baos);
        write(writer, serializer);
        writer.close();
        return new String(baos.toByteArray()).strip();
    }
    
    public static <T extends Serializable> IndexedData<T> read(String line, ChangeDataSerializer<T> serializer) throws IOException {
        int index = line.indexOf(',');
        if (index == -1) {
            throw new IOException("Unexpected change data line: no comma");
        }
        long id = Long.valueOf(line.substring(0, index));
        T data = serializer.deserialize(line.substring(index+1, line.length()));
        return new IndexedData<T>(id, data);
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof IndexedData)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        IndexedData<T> otherData = (IndexedData<T>)other;
        return rowId == otherData.getId() && data.equals(otherData.getData());
    }
    
    @Override
    public int hashCode() {
        return (int)rowId;
    }
    
    @Override
    public String toString() {
        return String.format("[IndexedData %d %s]", rowId, data);
    }

}