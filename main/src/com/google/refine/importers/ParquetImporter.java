
package com.google.refine.importers;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
        super(true);
    }

    @Override
    public ObjectNode createParserUIInitializationData(
            ImportingJob job, List<ObjectNode> fileRecords, String format) {
        ObjectNode options = super.createParserUIInitializationData(job, fileRecords, format);

        JSONUtilities.safePut(options, "trimStrings", true);

        return options;
    }

    @Override
    public void parseOneFile(
            Project project,
            ProjectMetadata metadata,
            ImportingJob job,
            String fileSource,
            InputStream inputStream,
            int limit,
            ObjectNode options,
            List<Exception> exceptions) {

        int limit2 = JSONUtilities.getInt(options, "limit", -1);
        if (limit > 0) {
            if (limit2 > 0) {
                limit2 = Math.min(limit, limit2);
            } else {
                limit2 = limit;
            }
        }

        try {
            // The structure information is stored at the end of the Parquet file. Therefore, The Parquet readers
            // needs to know the length of the file. Store the data from the input stream in a temporary local file.
            Path tempFile = Files.createTempFile("import", ".parquet");
            tempFile.toFile().deleteOnExit();
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            InputFile parquet = new LocalInputFile(tempFile);

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
                    row.setCell(c, new Cell((Serializable) cells.get(c), null));
                }

                project.rows.add(row);

                if (limit2 > 0 && project.rows.size() >= limit2) {
                    break;
                }
            }

            // delete the temporary file
            tempFile.toFile().delete();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
