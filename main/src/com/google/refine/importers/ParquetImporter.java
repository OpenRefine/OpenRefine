
package com.google.refine.importers;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import blue.strategic.parquet.Hydrator;
import blue.strategic.parquet.HydratorSupplier;
import blue.strategic.parquet.ParquetReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.LocalInputFile;
import org.apache.parquet.schema.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.ProjectMetadata;
import com.google.refine.importing.ImportingJob;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.JSONUtilities;

public class ParquetImporter extends TabularImportingParserBase {

    final static Logger logger = LoggerFactory.getLogger(ParquetImporter.class);

    public ParquetImporter() {
        super(ImportMode.FILE);
    }

    @Override
    public ObjectNode createParserUIInitializationData(
            ImportingJob job, List<ObjectNode> fileRecords, String format) {
        ObjectNode options = super.createParserUIInitializationData(job, fileRecords, format);

        JSONUtilities.safePut(options, "trimStrings", true);
        JSONUtilities.safePut(options, "forceText", false);

        return options;
    }

    @Override
    public void parseOneFile(
            Project project,
            ProjectMetadata metadata,
            ImportingJob job,
            String fileSource,
            File file,
            int limit,
            ObjectNode options,
            List<Exception> exceptions) {

        final boolean forceText = JSONUtilities.getBoolean(options, "forceText", false);

        int limit2 = JSONUtilities.getInt(options, "limit", -1);
        if (limit > 0) {
            if (limit2 > 0) {
                limit2 = Math.min(limit, limit2);
            } else {
                limit2 = limit;
            }
        }

        try {
            InputFile parquet = new LocalInputFile(file.toPath());

            ParquetMetadata parquetMetadata = ParquetReader.readMetadata(parquet);

            // report column headers
            MessageType schema = parquetMetadata.getFileMetaData().getSchema();

            List<String> columnNames = schema.getColumns().stream()
                    .map(col -> col.getPath()[0])
                    .collect(Collectors.toList());

            ImporterUtilities.setupColumns(project, columnNames);

            Hydrator<List<Object>, List<Object>> hydrator = new Hydrator<>() {

                @Override
                public List<Object> start() {
                    return new LinkedList<>();
                }

                @Override
                public List<Object> add(List<Object> target, String heading, Object value) {
                    target.add(value);
                    return target;
                }

                @Override
                public List<Object> finish(List<Object> target) {
                    return target;
                }
            };

            // add each row to the project
            Iterator<List<Object>> it = ParquetReader.streamContent(parquet, HydratorSupplier.constantly(hydrator)).iterator();
            while (!job.canceled && it.hasNext()) {
                List<Object> cells = it.next();
                Row row = new Row(cells.size());

                for (int c = 0; c < cells.size(); c++) {
                    Serializable value = (Serializable) cells.get(c);
                    if (forceText) {
                        value = Objects.toString(value);
                    }

                    row.setCell(c, new Cell(value, null));
                }

                project.rows.add(row);

                if (limit2 > 0 && project.rows.size() >= limit2) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
