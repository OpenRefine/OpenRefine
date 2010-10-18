package com.google.refine.extension.gdata;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gdata.client.spreadsheet.FeedURLFactory;
import com.google.gdata.client.spreadsheet.SpreadsheetQuery;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.client.spreadsheet.WorksheetQuery;
import com.google.gdata.data.Link;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.exporters.UrlExporter;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;


public class GDataExporter implements UrlExporter {

    final static Logger logger = LoggerFactory.getLogger("CsvExporter");

    private SpreadsheetService service;

    private FeedURLFactory factory;

    public GDataExporter() throws Exception {
      factory = FeedURLFactory.getDefault();
      service = new SpreadsheetService("");
    }
    
    /**
     * Creates a client object for which the provided username and password
     * produces a valid authentication.
     *
     * @param username the Google service user name
     * @param password the corresponding password for the user name
     * @throws Exception if error is encountered, such as invalid username and
     * password pair
     */
    public GDataExporter(String username, String password) throws Exception {
      this();
      service.setUserCredentials(username, password);
    }

    @Override
    public void export(Project project, Properties options, Engine engine, URL url) throws IOException {
        {
            RowVisitor visitor = new RowVisitor() {
                
                boolean columnHeader = true; //the first row should also add the column headers

                public boolean visit(Project project, int rowIndex, Row row) {
                    String[] vals = null;

                    if( columnHeader ){
                        String[] cols = new String[project.columnModel.columns.size()];
                        for(int i = 0; i < cols.length; i++){
                            cols[i] = project.columnModel.columns.get(i).getName();
                        }

                        // TODO: Write out column headers
                        
                        columnHeader = false; //switch off flag
                    }

                    vals = new String[row.cells.size()];
                    for(int i = 0; i < vals.length; i++){
                        Cell cell = row.cells.get(i);
                        if(cell != null){
                            vals[i] = row.cells.get(i).value.toString();
                        }
                    }

                    
                    // TODO: Write out values
                    
                    
                    return false;
                }

                @Override
                public void start(Project project) {
                    // nothing to do
                }

                @Override
                public void end(Project project) {
                        // Clean up
                }

            };

            FilteredRows filteredRows = engine.getAllFilteredRows();
            filteredRows.accept(project, visitor);
        }
    }

    @Override
    public String getContentType() {
        return "application/x-unknown";
    }

   
    
    /**
     * Gets the SpreadsheetEntry for the first spreadsheet with that name
     * retrieved in the feed.
     *
     * @param spreadsheet the name of the spreadsheet
     * @return the first SpreadsheetEntry in the returned feed, so latest
     * spreadsheet with the specified name
     * @throws Exception if error is encountered, such as no spreadsheets with the
     * name
     */
    public SpreadsheetEntry getSpreadsheet(String spreadsheet)
        throws Exception {
      
        SpreadsheetQuery spreadsheetQuery 
          = new SpreadsheetQuery(factory.getSpreadsheetsFeedUrl());
        spreadsheetQuery.setTitleQuery(spreadsheet);
        SpreadsheetFeed spreadsheetFeed = service.query(spreadsheetQuery, 
            SpreadsheetFeed.class);
        List<SpreadsheetEntry> spreadsheets = spreadsheetFeed.getEntries();
        if (spreadsheets.isEmpty()) {
          throw new Exception("No spreadsheets with that name");
        }

        return spreadsheets.get(0);
    }

    /**
     * Get the WorksheetEntry for the worksheet in the spreadsheet with the
     * specified name.
     *
     * @param spreadsheet the name of the spreadsheet
     * @param worksheet the name of the worksheet in the spreadsheet
     * @return worksheet with the specified name in the spreadsheet with the
     * specified name
     * @throws Exception if error is encountered, such as no spreadsheets with the
     * name, or no worksheet wiht the name in the spreadsheet
     */
    public WorksheetEntry getWorksheet(String spreadsheet, String worksheet) 
        throws Exception {

      SpreadsheetEntry spreadsheetEntry = getSpreadsheet(spreadsheet);

      WorksheetQuery worksheetQuery
        = new WorksheetQuery(spreadsheetEntry.getWorksheetFeedUrl());

      worksheetQuery.setTitleQuery(worksheet);
      WorksheetFeed worksheetFeed = service.query(worksheetQuery,
          WorksheetFeed.class);
      List<WorksheetEntry> worksheets = worksheetFeed.getEntries();
      if (worksheets.isEmpty()) {
        throw new Exception("No worksheets with that name in spreadhsheet "
            + spreadsheetEntry.getTitle().getPlainText());
      }

      return worksheets.get(0);
    }

    /**
     * Clears all the cell entries in the worksheet.
     *
     * @param spreadsheet the name of the spreadsheet
     * @param worksheet the name of the worksheet
     * @throws Exception if error is encountered, such as bad permissions
     */
    public void purgeWorksheet(String spreadsheet, String worksheet) 
        throws Exception {

      WorksheetEntry worksheetEntry = getWorksheet(spreadsheet, worksheet);
      CellFeed cellFeed = service.getFeed(worksheetEntry.getCellFeedUrl(), 
          CellFeed.class);

      List<CellEntry> cells = cellFeed.getEntries();
      for (CellEntry cell : cells) {
        Link editLink = cell.getEditLink();
        service.delete(new URL(editLink.getHref()));
      }
    }
    
    /**
     * Inserts a cell entry in the worksheet.
     *
     * @param spreadsheet the name of the spreadsheet
     * @param worksheet the name of the worksheet
     * @param row the index of the row
     * @param column the index of the column
     * @param input the input string for the cell
     * @throws Exception if error is encountered, such as bad permissions
     */
    public void insertCellEntry(String spreadsheet, String worksheet,
        int row, int column, String input) throws Exception {
      
      URL cellFeedUrl = getWorksheet(spreadsheet, worksheet).getCellFeedUrl();

      CellEntry newEntry = new CellEntry(row, column, input);

      service.insert(cellFeedUrl, newEntry);
    }


    /**
     * Main entry point. Parses arguments and creates and invokes the
     * ImportClient.
     */
    public void main(String[] args) throws Exception {

      String username = "user";
      String password = "pass";
      String filename = "file";
      String spreadsheet = "spreadsheet";
      String worksheet = "worksheet";

      GDataExporter client = new GDataExporter(username, password);

      client.purgeWorksheet(spreadsheet, worksheet);

      BufferedReader reader = null;
      try {
        reader = new BufferedReader(new FileReader(filename));
        String line = reader.readLine();
        int row = 0;
        while (line != null) {
        
          // Break up the line by the delimiter and insert the cells
          String[] cells = {};//delim.split(line, -1);  
          for (int col = 0; col < cells.length; col++) {
            client.insertCellEntry(spreadsheet, worksheet, 
                row + 1, col + 1, cells[col]);
          }
        
          // Advance the loop
          line = reader.readLine();
          row++;
        }
      } catch (Exception e) {
        throw e;
      } finally {
        if (reader != null) {
          reader.close();
        }
      }
    }
}
