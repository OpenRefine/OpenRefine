
package org.openrefine.importers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import org.openrefine.ProjectMetadata;
import org.openrefine.expr.EvalError;
import org.openrefine.importing.ImportingJob;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.Row;
import org.openrefine.model.Runner;
import org.openrefine.model.recon.Recon;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.overlay.OverlayModelResolver;
import org.openrefine.util.ParsingUtilities;

/**
 * Imports an OpenRefine project from a 3.x project archive.
 * 
 * @author Antonin Delpeuch
 *
 */
public class LegacyProjectImporter extends InputStreamImporter {

    @Override
    public Grid parseOneFile(Runner runner, ProjectMetadata metadata, ImportingJob job, String fileSource,
            String archiveFileName, InputStream inputStream, long limit, ObjectNode options) throws Exception {
        // open the project archive
        if (!fileSource.endsWith(".tar")) {
            inputStream = new GZIPInputStream(inputStream);
        }

        TarArchiveInputStream tin = new TarArchiveInputStream(inputStream);
        TarArchiveEntry tarEntry = null;

        // Read the pool and the grid from the zip file
        Map<Long, Recon> pool = new HashMap<>();
        List<SerializedRow> grid = new ArrayList<>();
        ColumnModel columnModel = null;
        Map<String, OverlayModel> overlayModels = new HashMap<>();

        while ((tarEntry = tin.getNextTarEntry()) != null) {
            if ("data.zip".equals(tarEntry.getName())) {
                ZipInputStream zis = new ZipInputStream(tin);
                ZipEntry zipEntry = null;
                while ((zipEntry = zis.getNextEntry()) != null) {
                    if ("pool.txt".equals(zipEntry.getName())) {
                        Reader reader = new InputStreamReader(zis, "UTF-8");
                        LineNumberReader lines = new LineNumberReader(reader);

                        loadPool(lines, pool);
                    } else if ("data.txt".equals(zipEntry.getName())) {
                        Reader reader = new InputStreamReader(zis, "UTF-8");
                        LineNumberReader lines = new LineNumberReader(reader);

                        // burn the first line, which contains the version
                        lines.readLine();

                        String line;
                        while ((line = lines.readLine()) != null) {
                            int equal = line.indexOf('=');
                            String field = line.substring(0, equal);
                            String value = line.substring(equal + 1);

                            // backward compatibility
                            if ("protograph".equals(field)) {
                                field = "overlayModel:freebaseProtograph";
                            }

                            if ("columnModel".equals(field)) {
                                columnModel = loadColumnModel(lines);
                            } else if ("history".equals(field)) {
                                skipHistory(lines);
                            } else if ("rowCount".equals(field)) {
                                int count = Integer.parseInt(value);

                                for (int i = 0; i < count; i++) {
                                    line = lines.readLine();
                                    if (line != null) {
                                        SerializedRow row = ParsingUtilities.mapper.readValue(line, SerializedRow.class);
                                        grid.add(row);
                                    }
                                }
                            } else if (field.startsWith("overlayModel:")) {
                                String modelName = field.substring("overlayModel:".length());
                                Class<? extends OverlayModel> klass = OverlayModelResolver.getClass(modelName);
                                if (klass != null) {
                                    try {
                                        OverlayModel overlayModel = ParsingUtilities.mapper.readValue(value, klass);

                                        overlayModels.put(modelName, overlayModel);
                                    } catch (IOException e) {
                                        logger.error("Failed to load overlay model " + modelName);
                                    }
                                }
                            }
                        }
                    }
                }
                zis.close();
                break;
            }
        }

        tin.close();

        // translate the grid to actual rows
        List<Row> rows = grid.stream().map(r -> r.toRow(pool)).collect(Collectors.toList());

        // form the final grid
        return runner.create(columnModel, rows, overlayModels);
    }

    protected ColumnModel loadColumnModel(LineNumberReader reader) throws IOException {
        int keyColumnIndex = 0;
        List<ColumnMetadata> columns = new ArrayList<>();

        String line;
        while ((line = reader.readLine()) != null && !"/e/".equals(line)) {
            int equal = line.indexOf('=');
            String field = line.substring(0, equal);
            String value = line.substring(equal + 1);
            if ("maxCellIndex".equals(field)) {
                // not used anymore
                continue;
            } else if ("keyColumnIndex".equals(field)) {
                keyColumnIndex = Integer.parseInt(value);
            } else if ("columnCount".equals(field)) {
                int nbColumns = Integer.parseInt(value);
                for (int i = 0; i != nbColumns; i++) {
                    columns.add(ParsingUtilities.mapper.readValue(reader.readLine(), ColumnMetadata.class));
                }
            } else if ("columnGroupCount".equals(field)) {
                int nbColumnGroups = Integer.parseInt(value);
                // not currently used
                for (int i = 0; i != nbColumnGroups; i++) {
                    reader.readLine();
                }
            }
        }
        return new ColumnModel(columns).withKeyColumnIndex(keyColumnIndex);
    }

    protected void skipHistory(LineNumberReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !"/e/".equals(line)) {
            int equal = line.indexOf('=');
            String field = line.substring(0, equal);
            String value = line.substring(equal + 1);
            if ("maxCellIndex".equals(field)) {
                // not used anymore
                continue;
            } else if ("pastEntryCount".equals(field)) {
                int nbEntries = Integer.parseInt(value);
                // not currently used
                for (int i = 0; i != nbEntries; i++) {
                    reader.readLine();
                }
            } else if ("futureEntryCount".equals(field)) {
                int nbEntries = Integer.parseInt(value);
                // not currently used
                for (int i = 0; i != nbEntries; i++) {
                    reader.readLine();
                }
            }
        }
    }

    protected void loadPool(LineNumberReader reader, Map<Long, Recon> pool) throws IOException {
        /* String version = */ reader.readLine();

        String line;
        while ((line = reader.readLine()) != null) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);
            String value = line.substring(equal + 1);

            if ("reconCount".equals(field)) {
                int count = Integer.parseInt(value);

                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        Recon recon = Recon.loadStreaming(line);
                        if (recon != null) {
                            pool.put(recon.id, recon);
                        }
                    }
                }
            }
        }
    }

    protected void loadGrid(LineNumberReader reader, List<SerializedRow> grid) {

    }

    /**
     * Helper class to deserialize a cell as serialized by OpenRefine up to 3.x. Used to import old projects in new
     * workspaces.
     * 
     * @author Antonin Delpeuch
     *
     */
    protected static class SerializedCell implements Serializable {

        private static final long serialVersionUID = 2723956335921206644L;

        private final Serializable _value;
        private final Long _reconId;

        public SerializedCell(
                Serializable value,
                Long reconId) {
            _value = value;
            _reconId = reconId;
        }

        @JsonCreator
        static public SerializedCell deserialize(
                @JsonProperty("v") Object value,
                @JsonProperty("t") String type,
                @JsonProperty("r") String reconId,
                @JsonProperty("e") String error) {
            if (type != null && "date".equals(type)) {
                value = ParsingUtilities.stringToDate((String) value);
            }
            if (error != null) {
                value = new EvalError(error);
            }
            return new SerializedCell((Serializable) value, reconId == null ? null : Long.parseLong(reconId));
        }

        @JsonProperty("v")
        public Serializable getValue() {
            return _value;
        }

        @JsonProperty("r")
        public Long getReconId() {
            return _reconId;
        }

        public Cell toCell(Map<Long, Recon> pool) {
            return new Cell(_value, _reconId == null ? null : pool.get(_reconId));
        }
    }

    /**
     * Legacy serialized version of a row, only used to read old projects.
     * 
     * @author Antonin Delpeuch
     *
     */
    protected static class SerializedRow {

        private final boolean _starred;
        private final boolean _flagged;
        private final List<SerializedCell> _cells;

        @JsonCreator
        public SerializedRow(
                @JsonProperty("cells") List<SerializedCell> cells,
                @JsonProperty("flagged") boolean flagged,
                @JsonProperty("starred") boolean starred) {
            _flagged = flagged;
            _starred = starred;
            _cells = cells == null ? Collections.emptyList() : cells;
        }

        public Row toRow(Map<Long, Recon> pool) {
            return new Row(_cells.stream().map(c -> c == null ? null : c.toCell(pool)).collect(Collectors.toList()), _flagged, _starred);
        }
    }

}
