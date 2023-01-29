
package org.openrefine.model.changes;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import org.openrefine.model.Cell;
import org.openrefine.util.ParsingUtilities;

public class CellListChangeDataSerializer implements ChangeDataSerializer<List<Cell>> {

    private static final long serialVersionUID = 3810800496455130098L;

    @Override
    public String serialize(List<Cell> changeDataItem) {
        try {
            return ParsingUtilities.saveWriter.writeValueAsString(changeDataItem);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cell serialization failed", e);
        }
    }

    @Override
    public List<Cell> deserialize(String serialized) throws IOException {
        TypeReference<List<Cell>> typeRef = new TypeReference<List<Cell>>() {
        };
        return ParsingUtilities.mapper.readValue(serialized, typeRef);
    }

}
