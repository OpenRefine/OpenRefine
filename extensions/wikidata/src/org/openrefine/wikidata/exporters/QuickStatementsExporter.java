/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package org.openrefine.wikidata.exporters;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.openrefine.wikidata.schema.WikibaseSchema;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.scheduler.ImpossibleSchedulingException;
import org.openrefine.wikidata.updates.scheduler.QuickStatementsUpdateScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueVisitor;

import com.google.refine.browsing.Engine;
import com.google.refine.exporters.WriterExporter;
import com.google.refine.model.Project;

public class QuickStatementsExporter implements WriterExporter {

    final static Logger logger = LoggerFactory.getLogger("QuickStatementsExporter");

    public static final String impossibleSchedulingErrorMessage = "This edit batch cannot be performed with QuickStatements due to the structure of its new items.";
    public static final String noSchemaErrorMessage = "No schema was provided. You need to align your project with Wikidata first.";

    public QuickStatementsExporter() {
    }

    @Override
    public String getContentType() {
        return "text/plain";
    }

    @Override
    public void export(Project project, Properties options, Engine engine, Writer writer)
            throws IOException {
        WikibaseSchema schema = (WikibaseSchema) project.overlayModels.get("wikibaseSchema");
        if (schema == null) {
            writer.write(noSchemaErrorMessage);
        } else {
            translateSchema(project, engine, schema, writer);
        }
    }

    /**
     * Exports a project and a schema to a QuickStatements file
     * 
     * @param project
     *            the project to translate
     * @param engine
     *            the engine used for evaluation of the edits
     * @param schema
     *            the WikibaseSchema used for translation of tabular data to edits
     * @param writer
     *            the writer to which the QS should be written
     * @throws IOException
     */
    public void translateSchema(Project project, Engine engine, WikibaseSchema schema, Writer writer)
            throws IOException {
        List<ItemUpdate> items = schema.evaluate(project, engine);
        translateItemList(items, writer);
    }

    public void translateItemList(List<ItemUpdate> updates, Writer writer)
            throws IOException {
        QuickStatementsUpdateScheduler scheduler = new QuickStatementsUpdateScheduler();
        try {
            List<ItemUpdate> scheduled = scheduler.schedule(updates);
            for (ItemUpdate item : scheduled) {
                translateItem(item, writer);
            }
        } catch (ImpossibleSchedulingException e) {
            writer.write(impossibleSchedulingErrorMessage);
        }

    }

    protected void translateNameDescr(String qid, Set<MonolingualTextValue> values, String prefix, ItemIdValue id,
            Writer writer)
            throws IOException {
        for (MonolingualTextValue value : values) {
            writer.write(qid + "\t");
            writer.write(prefix);
            writer.write(value.getLanguageCode());
            writer.write("\t\"");
            writer.write(value.getText());
            writer.write("\"\n");
        }
    }

    protected void translateItem(ItemUpdate item, Writer writer)
            throws IOException {
        String qid = item.getItemId().getId();
        if (item.isNew()) {
            writer.write("CREATE\n");
            qid = "LAST";
            item = item.normalizeLabelsAndAliases();
        }

        translateNameDescr(qid, item.getLabels(), "L", item.getItemId(), writer);
        translateNameDescr(qid, item.getLabelsIfNew(), "L", item.getItemId(), writer);
        translateNameDescr(qid, item.getDescriptions(), "D", item.getItemId(), writer);
        translateNameDescr(qid, item.getDescriptionsIfNew(), "D", item.getItemId(), writer);
        translateNameDescr(qid, item.getAliases(), "A", item.getItemId(), writer);

        for (Statement s : item.getAddedStatements()) {
            translateStatement(qid, s, s.getClaim().getMainSnak().getPropertyId().getId(), true, writer);
        }
        for (Statement s : item.getDeletedStatements()) {
            translateStatement(qid, s, s.getClaim().getMainSnak().getPropertyId().getId(), false, writer);
        }
    }

    protected void translateStatement(String qid, Statement statement, String pid, boolean add, Writer writer)
            throws IOException {
        Claim claim = statement.getClaim();

        Value val = claim.getValue();
        ValueVisitor<String> vv = new QSValuePrinter();
        String targetValue = val.accept(vv);
        if (targetValue != null) {
            if (!add) {
                writer.write("- ");
            }
            writer.write(qid + "\t" + pid + "\t" + targetValue);
            for (SnakGroup q : claim.getQualifiers()) {
                translateSnakGroup(q, false, writer);
            }
            for (Reference r : statement.getReferences()) {
                for (SnakGroup g : r.getSnakGroups()) {
                    translateSnakGroup(g, true, writer);
                }
                break; // QS only supports one reference
            }
            writer.write("\n");
        }
    }

    protected void translateSnakGroup(SnakGroup sg, boolean reference, Writer writer)
            throws IOException {
        for (Snak s : sg.getSnaks()) {
            translateSnak(s, reference, writer);
        }
    }

    protected void translateSnak(Snak s, boolean reference, Writer writer)
            throws IOException {
        String pid = s.getPropertyId().getId();
        if (reference) {
            pid = pid.replace('P', 'S');
        }
        Value val = s.getValue();
        ValueVisitor<String> vv = new QSValuePrinter();
        String valStr = val.accept(vv);
        if (valStr != null) {
            writer.write("\t" + pid + "\t" + valStr);
        }
    }

}
