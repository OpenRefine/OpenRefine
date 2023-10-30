
package org.openrefine.model.changes;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.openrefine.model.Row;
import org.openrefine.util.ParsingUtilities;

public class RowChangeDataSerializer implements ChangeDataSerializer<Row> {

    private static final long serialVersionUID = 606360403156779037L;

    @Override
    public String serialize(Row changeDataItem) {
        try {
            return ParsingUtilities.saveWriter.writeValueAsString(changeDataItem);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("row serialization failed", e);
        }
    }

    @Override
    public Row deserialize(String serialized) throws IOException {
        return ParsingUtilities.mapper.readValue(serialized, Row.class);
    }

}
