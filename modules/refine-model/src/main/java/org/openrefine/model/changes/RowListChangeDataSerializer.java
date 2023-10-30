
package org.openrefine.model.changes;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import org.openrefine.model.Row;
import org.openrefine.util.ParsingUtilities;

public class RowListChangeDataSerializer implements ChangeDataSerializer<List<Row>> {

    private static final long serialVersionUID = 3810800496455130098L;
    private TypeReference<List<Row>> typeRef = new TypeReference<List<Row>>() {
    };

    @Override
    public String serialize(List<Row> changeDataItem) {
        try {
            return ParsingUtilities.saveWriter.writeValueAsString(changeDataItem);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cell serialization failed", e);
        }
    }

    @Override
    public List<Row> deserialize(String serialized) throws IOException {

        return ParsingUtilities.mapper.readValue(serialized, typeRef);
    }

}
