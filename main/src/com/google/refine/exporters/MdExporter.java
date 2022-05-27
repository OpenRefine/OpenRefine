package com.google.refine.exporters;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.refine.browsing.Engine;
import com.google.refine.model.Project;
import net.steppschuh.markdowngenerator.MarkdownSerializationException;
import net.steppschuh.markdowngenerator.table.Table;
import net.steppschuh.markdowngenerator.link.Link;
import net.steppschuh.markdowngenerator.text.Text;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

public class MdExporter implements StreamExporter{
    @Override
    public String getContentType() {
        return "text/markdown";
    }

    @Override
    public void export(Project project, Properties params, Engine engine, OutputStream outputStream) throws IOException {
        final Table.Builder mdDoc = new Table.Builder();

        TabularSerializer serializer = new TabularSerializer() {
            @Override
            public void startFile(JsonNode options) {
            }

            @Override
            public void endFile() {
            }

            @Override
            public void addRow(List<CellData> cells, boolean isHeader) {
                String[] cellData = new String[cells.size()];
                for(int i = 0; i < cells.size(); i++){
                    CellData cellDatatmp = cells.get(i);
                    cellData[i] = "";
                    if(cellDatatmp != null && cellDatatmp.text != null){
                        String textInCell = cellDatatmp.text;
                        textInCell = textInCell.replaceAll("([`|])", "\\\\$1");
                        try {
                            if(cellDatatmp.link != null){
                                    cellData[i] = new Link(new Text(textInCell).serialize(), cellDatatmp.link).serialize();
                            }else{
                                cellData[i] = new Text(textInCell).serialize();
                            }
                        } catch (MarkdownSerializationException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                mdDoc.addRow(cellData);
            }
        };
        CustomizableTabularExporterUtilities.exportRows(
                project, engine, params, serializer);
        outputStream.write(mdDoc.build().toString().getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }
}
