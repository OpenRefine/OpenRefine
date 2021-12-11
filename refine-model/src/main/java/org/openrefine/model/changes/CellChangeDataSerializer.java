package org.openrefine.model.changes;

import java.io.IOException;

import org.openrefine.model.Cell;
import org.openrefine.util.ParsingUtilities;

import com.fasterxml.jackson.core.JsonProcessingException;

public class CellChangeDataSerializer implements ChangeDataSerializer<Cell> {

    private static final long serialVersionUID = 606360403156779037L;

    @Override
    public String serialize(Cell changeDataItem) {
        try {
            return ParsingUtilities.saveWriter.writeValueAsString(changeDataItem);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cell serialization failed", e);
        }
    }

    @Override
    public Cell deserialize(String serialized) throws IOException {
        return ParsingUtilities.mapper.readValue(serialized, Cell.class);
    }
    
}