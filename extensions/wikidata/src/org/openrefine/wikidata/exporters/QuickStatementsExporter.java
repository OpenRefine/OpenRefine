package org.openrefine.wikidata.exporters;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Properties;

import com.google.refine.browsing.Engine;
import com.google.refine.exporters.WriterExporter;
import com.google.refine.model.Project;

import org.openrefine.wikidata.schema.ItemUpdate;
import org.openrefine.wikidata.schema.WikibaseSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.DatatypeIdValue;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
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
        translateSchema(project, engine, schema, writer);
    }
    
    public void translateSchema(Project project, Engine engine, WikibaseSchema schema, Writer writer) throws IOException {
        List<ItemUpdate> items = schema.evaluate(project, engine);
        for (ItemUpdate item : items) {
            translateItem(item, writer);
        }
    }
    
    protected void translateItem(ItemUpdate item, Writer writer) throws IOException {
        if (item.getItemId().equals(ItemIdValue.NULL)) {
            writer.write("CREATE\n");
        }
        for (Statement s : item.getAddedStatements()) {
            translateStatement(s, s.getClaim().getMainSnak().getPropertyId().getId(), true, writer);
        }
        for (Statement s : item.getDeletedStatements()) {
            translateStatement(s, s.getClaim().getMainSnak().getPropertyId().getId(), false, writer);
        }
    }
    
    protected void translateStatement(Statement statement, String pid, boolean add, Writer writer) throws IOException {
        Claim claim = statement.getClaim();
        String qid = claim.getSubject().getId();
        if (claim.getSubject().equals(ItemIdValue.NULL)) {
            qid = "LAST";
        }
        Value val = claim.getValue();
        ValueVisitor<String> vv = new ValuePrinter();
        String targetValue = val.accept(vv);
        if (targetValue != null) {
           if (! add) {
               writer.write("- ");
           }
           writer.write(qid + "\t" + pid + "\t" + targetValue);
           for(SnakGroup q : claim.getQualifiers()) {
               translateSnakGroup(q, false, writer);
           }
           for(Reference r : statement.getReferences()) {
               for(SnakGroup g : r.getSnakGroups()) {
                   translateSnakGroup(g, true, writer);
               }
               break; // QS only supports one reference
           }
           writer.write("\n");
        }
    }
    
    protected void translateSnakGroup(SnakGroup sg, boolean reference, Writer writer) throws IOException {
        for(Snak s : sg.getSnaks()) {
            translateSnak(s, reference, writer);
        }
    }
    
    protected void translateSnak(Snak s, boolean reference, Writer writer) throws IOException {
        String pid = s.getPropertyId().getId();
        if (reference) {
            pid = pid.replace('P', 'S');
        }
        Value val = s.getValue();
        ValueVisitor<String> vv = new ValuePrinter();
        String valStr = val.accept(vv);
        if(valStr != null) {
            writer.write("\t" + pid + "\t" + valStr);
        }
    }
   
    class ValuePrinter implements ValueVisitor<String> {

        @Override
        public String visit(DatatypeIdValue value) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String visit(EntityIdValue value) {
            if (value.equals(ItemIdValue.NULL)) {
                return null;
            }
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
