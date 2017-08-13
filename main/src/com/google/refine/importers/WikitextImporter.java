package com.google.refine.importers;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.io.CharStreams;
import de.fau.cs.osr.ptk.common.AstVisitor;

import org.sweble.wikitext.parser.ParserConfig;
import org.sweble.wikitext.parser.utils.SimpleParserConfig;
import org.sweble.wikitext.parser.WikitextParser;
import org.sweble.wikitext.parser.nodes.WtNode;
import org.sweble.wikitext.parser.nodes.WtText;
import org.sweble.wikitext.parser.nodes.WtInternalLink;
import org.sweble.wikitext.parser.nodes.WtExternalLink;
import org.sweble.wikitext.parser.nodes.WtLinkTitle;
import org.sweble.wikitext.parser.nodes.WtLinkTitle.WtNoLinkTitle;
import org.sweble.wikitext.parser.nodes.WtUrl;
import org.sweble.wikitext.parser.nodes.WtTable;
import org.sweble.wikitext.parser.nodes.WtTableHeader;
import org.sweble.wikitext.parser.nodes.WtTableRow;
import org.sweble.wikitext.parser.nodes.WtTableCell;
import org.sweble.wikitext.parser.nodes.WtParsedWikitextPage;
import org.sweble.wikitext.parser.nodes.WtBody;

import org.sweble.wikitext.parser.WikitextEncodingValidator;
import org.sweble.wikitext.parser.WikitextPreprocessor;
import org.sweble.wikitext.parser.encval.ValidatedWikitext;
import org.sweble.wikitext.parser.nodes.WtParsedWikitextPage;
import org.sweble.wikitext.parser.nodes.WtPreproWikitextPage;
import org.sweble.wikitext.parser.parser.PreprocessorToParserTransformer;
import org.sweble.wikitext.parser.preprocessor.PreprocessedWikitext;

import xtc.parser.ParseException;

import com.google.refine.ProjectMetadata;
import com.google.refine.importing.ImportingJob;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;



public class WikitextImporter extends TabularImportingParserBase {
    static final private Logger logger = LoggerFactory.getLogger(WikitextImporter.class);
    
    public WikitextImporter() {
        super(false);
    }
    
    @Override
    public JSONObject createParserUIInitializationData(
            ImportingJob job, List<JSONObject> fileRecords, String format) {
        JSONObject options = super.createParserUIInitializationData(job, fileRecords, format);
        
        JSONUtilities.safePut(options, "guessCellValueTypes", false);
        
        return options;
    }
    
    public class WikitextTableVisitor extends AstVisitor<WtNode> {
        
        public List<String> header;
        public List<List<String>> rows;
        private List<String> currentRow;
        private StringBuilder currentStringBuilder;
        private String currentInternalLink;
        private String currentExternalLink;
        
        public WikitextTableVisitor() {
            header = new ArrayList<String>();
            rows = new ArrayList<List<String>>();
            currentStringBuilder = null;
            currentInternalLink = null;
            currentExternalLink = null;
        }
        
        @Override
        protected WtNode before(WtNode node) {
            return super.before(node);
        }
        
        public void visit(WtNode e) {
            // Ignore other nodes
        }
        
        public void visit(WtParsedWikitextPage e) {
            iterate(e);
        }
        
        public void visit(WtBody e) {
            iterate(e);
        }
        
        public void visit(WtTable e) {
            iterate(e);
        }
        
        public void visit(WtTableHeader e) {
            header.add(renderAsString(e));
        }
 
        public void visit(WtTableRow e)
        {
            if (currentRow == null) {
                currentRow = new ArrayList<String>();
                iterate(e);
                if(currentRow.size() > 0) {
                    rows.add(currentRow);
                } 
                currentRow = null;
            }
        }
        
        public void visit(WtTableCell e)
        {
            if (currentRow != null) {
                currentRow.add(renderAsString(e));
            }
        }
        
        public String renderAsString(WtNode e) {
            currentStringBuilder = new StringBuilder();
            iterate(e);
            String value = currentStringBuilder.toString().trim();
            currentStringBuilder = null;
            return value;
        }
        
        
        public void visit(WtText text) {
            if (currentStringBuilder != null) {
                currentStringBuilder.append(text.getContent());
            }
        }
        
        public void visit(WtNoLinkTitle e) {
            if (currentInternalLink != null) {
                currentStringBuilder.append(currentInternalLink);
            } else if (currentExternalLink != null) {
                currentStringBuilder.append(currentExternalLink);
            }
        }
        
        public void visit(WtLinkTitle e) {
            iterate(e);
        }
        
        public void visit(WtInternalLink e) {
            currentInternalLink = e.getTarget().getAsString();
            iterate(e);
            currentInternalLink = null;
        }
        
        public void visit(WtExternalLink e) {
            WtUrl url = e.getTarget();
            currentExternalLink = url.getProtocol() + ":" + url.getPath();
            iterate(e);
            currentExternalLink = null;
        }
        
        @Override
        protected Object after(WtNode node, Object result)
        {
            return rows;
        }
    }
    
    public class WikiTableDataReader implements TableDataReader {
        private int currentRow = -1;
        private WikitextTableVisitor visitor = null;
    
        public WikiTableDataReader(WikitextTableVisitor visitor) {
            this.visitor = visitor;
            currentRow = -1;
        }
        
        @Override
        public List<Object> getNextRowOfCells() throws IOException {
            List<Object> row = null;
            List<String> origRow = null;
            if (currentRow == -1) {
                origRow = this.visitor.header;
            } else if(currentRow < this.visitor.rows.size()) {
                origRow = this.visitor.rows.get(currentRow);
            }
            currentRow++;
            
            if (origRow != null) {
                row = new ArrayList<Object>();
                for (int i = 0; i < origRow.size(); i++) {
                    row.add(origRow.get(i));
                }
            }
            return row;
        }
    }

    @Override
    public void parseOneFile(
        Project project,
        ProjectMetadata metadata,
        ImportingJob job,
        String fileSource,
        Reader reader,
        int limit,
        JSONObject options,
        List<Exception> exceptions
    ) {
        // Set-up a simple wiki configuration
        ParserConfig parserConfig = new SimpleParserConfig();
        
        try {
            // Encoding validation

            WikitextEncodingValidator v = new WikitextEncodingValidator();

            String wikitext = CharStreams.toString(reader);
            String title = "Page title";
            ValidatedWikitext validated = v.validate(parserConfig, wikitext, title);

            // Pre-processing
            WikitextPreprocessor prep = new WikitextPreprocessor(parserConfig);

            WtPreproWikitextPage prepArticle =
                            (WtPreproWikitextPage) prep.parseArticle(validated, title, false);

            // Parsing
            PreprocessedWikitext ppw = PreprocessorToParserTransformer
                            .transform(prepArticle);

            WikitextParser parser = new WikitextParser(parserConfig);

            WtParsedWikitextPage parsedArticle;
            parsedArticle = (WtParsedWikitextPage) parser.parseArticle(ppw, title);
            
            // Compile the retrieved page
            final WikitextTableVisitor vs = new WikitextTableVisitor();
            vs.go(parsedArticle);
            
            TableDataReader dataReader = new WikiTableDataReader(vs);
            
            JSONUtilities.safePut(options, "headerLines", 1);
            
            TabularImportingParserBase.readTable(project, metadata, job, dataReader, fileSource, limit, options, exceptions);
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (ParseException e1) {
            exceptions.add(e1);
            e1.printStackTrace();
        }
    }
}
