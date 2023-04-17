
package org.openrefine.importers;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.List;
import java.util.Scanner;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.text.StringEscapeUtils;

import org.openrefine.ProjectMetadata;
import org.openrefine.importing.ImportingFileRecord;
import org.openrefine.importing.ImportingJob;
import org.openrefine.model.Cell;
import org.openrefine.model.Grid;
import org.openrefine.model.Row;
import org.openrefine.model.Runner;
import org.openrefine.util.CloseableIterable;
import org.openrefine.util.CloseableIterator;
import org.openrefine.util.JSONUtilities;

/**
 * An importer which reads each line of a file as a row with a single string-valued cell containing the line.
 */
public class LineBasedImporter extends ReaderImporter {

    @Override
    public ObjectNode createParserUIInitializationData(Runner runner,
            ImportingJob job, List<ImportingFileRecord> fileRecords, String format) {
        ObjectNode options = super.createParserUIInitializationData(runner, job, fileRecords, format);
        JSONUtilities.safePut(options, "separator", "\\r?\\n");

        JSONUtilities.safePut(options, "linesPerRow", 1);
        JSONUtilities.safePut(options, "headerLines", 0);
        JSONUtilities.safePut(options, "guessCellValueTypes", false);

        return options;
    }

    @Override
    public Grid parseOneFile(
            Runner runner,
            ProjectMetadata metadata,
            ImportingJob job,
            String fileSource,
            String archiveFileName,
            Supplier<Reader> reader,
            long limit,
            ObjectNode options) throws Exception {
        String sepStr = JSONUtilities.getString(options, "separator", "\\r?\\n");
        if (sepStr == null || "".equals(sepStr)) {
            sepStr = "\\r?\\n";
        }
        sepStr = StringEscapeUtils.unescapeJava(sepStr);
        Pattern sep = Pattern.compile(sepStr);

        final int linesPerRow = JSONUtilities.getInt(options, "linesPerRow", 1);

        // the default for headerLines in TabularParserHelper is 1
        if (!options.has("headerLines")) {
            JSONUtilities.safePut(options, "headerLines", 0);
        }

        CloseableIterable<Row> rowIterable = () -> {
            final Scanner lnReader = new Scanner(new BufferedReader(reader.get()));
            lnReader.useDelimiter(sep);
            CloseableIterator<String> iterator = CloseableIterator.wrapping(lnReader, lnReader);
            return iterator.grouped(linesPerRow)
                    .map(stringList -> new Row(stringList.map(string -> new Cell(string, null)).toJavaList()));
        };

        return TabularParserHelper.parseOneFile(runner, fileSource, archiveFileName, rowIterable, limit, options);
    }

}
