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

package org.openrefine.wikibase.exporters;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.openrefine.wikibase.schema.WikibaseSchema;
import org.openrefine.wikibase.schema.strategies.StatementEditingMode;
import org.openrefine.wikibase.schema.validation.ValidationState;
import org.openrefine.wikibase.updates.EntityEdit;
import org.openrefine.wikibase.updates.ItemEdit;
import org.openrefine.wikibase.updates.MediaInfoEdit;
import org.openrefine.wikibase.updates.StatementEdit;
import org.openrefine.wikibase.updates.scheduler.ImpossibleSchedulingException;
import org.openrefine.wikibase.updates.scheduler.QuickStatementsUpdateScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

import com.google.refine.browsing.Engine;
import com.google.refine.exporters.WriterExporter;
import com.google.refine.model.Project;

public class QuickStatementsExporter implements WriterExporter {

    final static Logger logger = LoggerFactory.getLogger("QuickStatementsExporter");

    public static final String impossibleSchedulingErrorMessage = "This edit batch cannot be performed with QuickStatements due to the structure of its new entities. QuickStatements does not support creating two new entities which refer to each other. Consider uploading your edits directly with OpenRefine.";
    public static final String noSchemaErrorMessage = "No schema was provided. You need to align your project with Wikibase first.";

    protected final QSSnakPrinter mainSnakPrinter;
    protected final QSSnakPrinter referenceSnakPrinter;

    public QuickStatementsExporter() {
        mainSnakPrinter = new QSSnakPrinter(false);
        referenceSnakPrinter = new QSSnakPrinter(true);
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
        // Validate the schema
        ValidationState validation = new ValidationState(project.columnModel);
        schema.validate(validation);
        if (!validation.getValidationErrors().isEmpty()) {
            throw new IllegalStateException("Schema is incomplete");
        }
        // Evaluate the schema
        List<EntityEdit> items = schema.evaluate(project, engine);

        // First, check the entity edits for any problems, and only start the translation if there are no problems
        String errorMessage = null;
        Optional<EntityEdit> unsupportedEntityTypeEdit = items.stream()
                .filter(entityEdit -> !(entityEdit instanceof ItemEdit || entityEdit instanceof MediaInfoEdit))
                .findAny();
        if (unsupportedEntityTypeEdit.isPresent()) {
            errorMessage = "Unable to export updates on " +
                    unsupportedEntityTypeEdit.get().getEntityId() + " with QuickStatements, not supported for this entity type";
        }
        Optional<EntityEdit> newMediaInfo = items.stream()
                .filter(entityEdit -> entityEdit instanceof MediaInfoEdit && entityEdit.isNew())
                .findAny();
        if (newMediaInfo.isPresent()) {
            errorMessage = "Unable to create a new mediainfo entity " +
                    unsupportedEntityTypeEdit.get().getEntityId() + " with QuickStatements, not supported";
        }

        if (errorMessage != null) {
            writer.write(errorMessage);
        } else {
            translateItemList(items, writer);
        }
    }

    public void translateItemList(List<EntityEdit> updates, Writer writer)
            throws IOException {
        QuickStatementsUpdateScheduler scheduler = new QuickStatementsUpdateScheduler();
        try {
            List<EntityEdit> scheduled = scheduler.schedule(updates);
            for (EntityEdit entityEdit : scheduled) {
                if (entityEdit instanceof ItemEdit) {
                    translateItem((ItemEdit) entityEdit, writer);
                } else if (entityEdit instanceof MediaInfoEdit) {
                    translateMediaInfo((MediaInfoEdit) entityEdit, writer);
                } else {
                    // prevented by the earlier checks above
                    throw new IllegalStateException();
                }
            }
        } catch (ImpossibleSchedulingException e) {
            writer.write(impossibleSchedulingErrorMessage);
        }

    }

    protected void translateNameDescr(String qid, Set<MonolingualTextValue> values, String prefix, EntityIdValue id,
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

    protected void translateItem(ItemEdit item, Writer writer)
            throws IOException {
        String qid = item.getEntityId().getId();
        if (item.isNew()) {
            writer.write("CREATE\n");
            qid = "LAST";
        }

        translateNameDescr(qid, item.getLabels(), "L", item.getEntityId(), writer);
        translateNameDescr(qid, item.getLabelsIfNew(), "L", item.getEntityId(), writer);
        translateNameDescr(qid, item.getDescriptions(), "D", item.getEntityId(), writer);
        translateNameDescr(qid, item.getDescriptionsIfNew(), "D", item.getEntityId(), writer);
        translateNameDescr(qid, item.getAliases(), "A", item.getEntityId(), writer);

        for (StatementEdit s : item.getStatementEdits()) {
            translateStatement(qid, s.getStatement(), s.getPropertyId().getId(), s.getMode() == StatementEditingMode.ADD_OR_MERGE, writer);
        }
    }

    protected void translateMediaInfo(MediaInfoEdit item, Writer writer)
            throws IOException {
        String qid = item.getEntityId().getId();
        if (item.isNew()) {
            throw new IllegalStateException();
        }

        translateNameDescr(qid, item.getLabels(), "L", item.getEntityId(), writer);
        translateNameDescr(qid, item.getLabelsIfNew(), "L", item.getEntityId(), writer);

        for (StatementEdit s : item.getStatementEdits()) {
            translateStatement(qid, s.getStatement(), s.getPropertyId().getId(), s.getMode() == StatementEditingMode.ADD_OR_MERGE, writer);
        }
    }

    protected void translateStatement(String qid, Statement statement, String pid, boolean add, Writer writer)
            throws IOException {
        Claim claim = statement.getClaim();

        Snak mainSnak = claim.getMainSnak();
        String mainSnakQS = mainSnak.accept(mainSnakPrinter);
        if (!add) {
            // According to: https://www.wikidata.org/wiki/Help:QuickStatements#Removing_statements,
            // Removing statements won't be followed by qualifiers or references.
            writer.write("- ");
            writer.write(qid + mainSnakQS);
            writer.write("\n");
        } else { // add statements
            if (statement.getReferences().isEmpty()) {
                writer.write(qid + mainSnakQS);
                for (SnakGroup q : claim.getQualifiers()) {
                    translateSnakGroup(q, false, writer);
                }
                writer.write("\n");
            } else {
                // According to: https://www.wikidata.org/wiki/Help:QuickStatements#Add_statement_with_sources
                // Existing statements with an exact match (property and value) will not be added again;
                // however additional references might be added to the statement.

                // So, to handle multiple references, we can duplicate the statement just with different references.
                for (Reference r : statement.getReferences()) {
                    writer.write(qid + mainSnakQS);
                    for (SnakGroup q : claim.getQualifiers()) {
                        translateSnakGroup(q, false, writer);
                    }
                    for (SnakGroup g : r.getSnakGroups()) {
                        translateSnakGroup(g, true, writer);
                    }
                    writer.write("\n");
                }
            }
        }
    }

    protected void translateSnakGroup(SnakGroup sg, boolean reference, Writer writer)
            throws IOException {
        for (Snak s : sg.getSnaks()) {
            if (reference) {
                writer.write(s.accept(referenceSnakPrinter));
            } else {
                writer.write(s.accept(mainSnakPrinter));
            }
        }
    }

}
