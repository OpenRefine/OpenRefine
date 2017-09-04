package org.openrefine.wikidata.exporters;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.util.List;
import java.util.Properties;

/*
import org.deri.grefine.rdf.Node;
import org.deri.grefine.rdf.RdfSchema;
import org.deri.grefine.rdf.Util;
import org.deri.grefine.rdf.app.ApplicationContext;
import org.deri.grefine.rdf.vocab.Vocabulary;
import org.deri.grefine.rdf.vocab.VocabularyIndexException;
import org.openrdf.model.*;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.openrdf.sail.memory.MemoryStore;

import info.aduna.iteration.CloseableIteration;
*/

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.exporters.WriterExporter;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

import org.openrefine.wikidata.schema.WbChangeExpr;
import org.openrefine.wikidata.schema.WbItemStatementsExpr;
import org.openrefine.wikidata.schema.WikibaseSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.DatatypeIdValue;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueVisitor;

public class QuickStatementsExporter implements WriterExporter {

    final static Logger logger = LoggerFactory.getLogger("QuickStatementsExporter");

    public QuickStatementsExporter(){
    }
     
    @Override
    public String getContentType() {
        return "text";
    }


    @Override
    public void export(Project project, Properties options, Engine engine, Writer writer)
            throws IOException {
        WikibaseSchema schema = (WikibaseSchema) project.overlayModels.get("wikibaseSchema");
        if (schema == null) {
            return;
        }
        translateSchema(project, schema, writer);
    }
    
    public void translateSchema(Project project, WikibaseSchema schema, Writer writer) throws IOException {
        List<StatementGroup> statements = schema.evaluate(project);
        for (StatementGroup group : statements) {
            translateStatementGroup(group, writer);
        }
    }
    
    protected void translateStatementGroup(StatementGroup group, Writer writer) throws IOException {
        String pid = group.getProperty().getId();
        for(Statement statement : group.getStatements()) {
            translateStatement(statement, pid, writer);
        }
    }
    
    protected void translateStatement(Statement statement, String pid, Writer writer) throws IOException {
        Claim claim = statement.getClaim();
        String qid = claim.getSubject().getId();
        Value val = claim.getValue();
        ValueVisitor<String> vv = new ValuePrinter();
        String targetValue = val.accept(vv);
        writer.write(qid + "\t" + pid + "\t" + targetValue + "\n");
    }
   
    class ValuePrinter implements ValueVisitor<String> {

        @Override
        public String visit(DatatypeIdValue value) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String visit(EntityIdValue value) {
            return value.getId();
        }

        @Override
        public String visit(GlobeCoordinatesValue value) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String visit(MonolingualTextValue value) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String visit(QuantityValue value) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String visit(StringValue value) {
            return "\"" + value.getString() + "\"";
        }

        @Override
        public String visit(TimeValue value) {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
}
