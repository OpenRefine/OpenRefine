
package org.openrefine.model.changes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.Objects;

/**
 * Helper class to represent an item in the map from row ids to change values in the context of a {@link ChangeData}
 * object. The row id is always provided. The associated data may be null. This happens when the underlying change data
 * computing process returned null or that the row/record with this id was simply excluded from the computation (via
 * facets) in the first place. Additionally, the indexed data can be marked as "being computed", meaning that it is not
 * available yet but may become non-null later.
 */
public class IndexedData<T> implements Serializable {

    private static final long serialVersionUID = 6928586351690626940L;

    private final long rowId;
    private final T data;
    private final boolean pending;

    public IndexedData(
            long rowId,
            T data) {
        this.rowId = rowId;
        this.data = data;
        this.pending = false;
    }

    public IndexedData(long rowId) {
        this.rowId = rowId;
        this.data = null;
        this.pending = true;
    }

    public long getId() {
        return rowId;
    }

    public T getData() {
        return data;
    }

    public boolean isPending() {
        return pending;
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

    public static <T> IndexedData<T> read(String line, ChangeDataSerializer<T> serializer) throws IOException {
        int index = line.indexOf(',');
        if (index == -1) {
            throw new IOException("Unexpected change data line: no comma");
        }
        long id = Long.valueOf(line.substring(0, index));
        T data = serializer.deserialize(line.substring(index + 1, line.length()));
        return new IndexedData<T>(id, data);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof IndexedData)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        IndexedData<T> otherData = (IndexedData<T>) other;
        return rowId == otherData.getId() && pending == otherData.isPending() && Objects.equals(data, otherData.getData());
    }

    @Override
    public int hashCode() {
        return (int) rowId;
    }

    @Override
    public String toString() {
        if (pending) {
            return String.format("[IndexedData %d (pending)]", rowId);
        } else {
            return String.format("[IndexedData %d %s]", rowId, data);
        }
    }

}
