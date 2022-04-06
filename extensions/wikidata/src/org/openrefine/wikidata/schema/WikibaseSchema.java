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
package org.openrefine.wikidata.schema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openrefine.wikidata.qa.QAWarningStore;
import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.openrefine.wikidata.updates.TermedStatementEntityEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.model.OverlayModel;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.ParsingUtilities;

/**
 * Main class representing a skeleton of Wikibase edits with OpenRefine columns
 * as variables.
 * 
 * @author Antonin Delpeuch
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WikibaseSchema implements OverlayModel {

    final static Logger logger = LoggerFactory.getLogger("RdfSchema");

    @JsonProperty("itemDocuments")
    protected List<WbEntityDocumentExpr> entityDocumentExprs = new ArrayList<>();

    @JsonProperty("siteIri")
    protected String siteIri;
    
    @JsonProperty("entityTypeSiteIRI")
    protected Map<String, String> entityTypeSiteIri;

    @JsonProperty("mediaWikiApiEndpoint")
    protected String mediaWikiApiEndpoint;

    /**
     * Constructor.
     * @todo remove this, it does not create a valid schema.
     */
    public WikibaseSchema() {
    	entityTypeSiteIri = Collections.emptyMap();
    }

    /**
     * Constructor for deserialization via Jackson
     */
    @JsonCreator
    public WikibaseSchema(@JsonProperty("itemDocuments") List<WbEntityDocumentExpr> exprs,
                          @JsonProperty("siteIri") String siteIri,
                          @JsonProperty("entityTypeSiteIRI") Map<String, String> entityTypeSiteIri,
                          @JsonProperty("mediaWikiApiEndpoint") String mediaWikiApiEndpoint) {
        this.entityDocumentExprs = exprs;
        this.siteIri = siteIri;
        this.entityTypeSiteIri = entityTypeSiteIri != null ? entityTypeSiteIri : Collections.emptyMap();
        this.mediaWikiApiEndpoint = mediaWikiApiEndpoint != null ? mediaWikiApiEndpoint : ApiConnection.URL_WIKIDATA_API;
    }

    /**
     * @return the site IRI of the Wikibase instance referenced by this schema
     */
    @JsonProperty("siteIri")
    public String getSiteIri() {
        return siteIri;
    }
    
    /**
     * @return the site IRI of the Wikibase instance referenced by this schema
     */
    @JsonProperty("entityTypeSiteIRI")
    public Map<String, String> getEntityTypeSiteIri() {
        return entityTypeSiteIri;
    }

    /**
     * @return the list of document expressions for this schema
     */
    @JsonProperty("itemDocuments")
    public List<WbEntityDocumentExpr> getEntityDocumentExpressions() {
        return Collections.unmodifiableList(entityDocumentExprs);
    }

    @JsonProperty("mediaWikiApiEndpoint")
    public String getMediaWikiApiEndpoint() {
        return mediaWikiApiEndpoint;
    }

    /**
     * Evaluates all entity documents in a particular expression context. This
     * specifies, among others, a row where the values of the variables will be
     * read.
     * 
     * @param ctxt
     *            the context in which the schema should be evaluated.
     * @return
     */
    public List<TermedStatementEntityEdit> evaluateEntityDocuments(ExpressionContext ctxt) {
        List<TermedStatementEntityEdit> result = new ArrayList<>();
        for (WbEntityDocumentExpr expr : entityDocumentExprs) {

            try {
                result.add(expr.evaluate(ctxt));
            } catch (SkipSchemaExpressionException e) {
                continue;
            }
        }
        return result;
    }

    /**
     * Evaluates the schema on a project, returning a list of EntityUpdates generated
     * by the schema.
     * 
     * Some warnings will be emitted in the warning store: those are only the ones
     * that are generated at evaluation time (such as invalid formats for dates).
     * Issues detected on candidate statements (such as constraint violations) are
     * not included at this stage.
     * 
     * @param project
     *            the project on which the schema should be evaluated
     * @param engine
     *            the engine, which gives access to the current facets
     * @param warningStore
     *            a store in which issues will be emitted
     * @return entity updates are stored in their generating order (not merged yet).
     */
    public List<TermedStatementEntityEdit> evaluate(Project project, Engine engine, QAWarningStore warningStore) {
        List<TermedStatementEntityEdit> result = new ArrayList<>();
        FilteredRows filteredRows = engine.getAllFilteredRows();
        filteredRows.accept(project, new EvaluatingRowVisitor(result, warningStore));
        return result;
    }

    /**
     * Same as above, ignoring any warnings.
     */
    public List<TermedStatementEntityEdit> evaluate(Project project, Engine engine) {
        return evaluate(project, engine, null);
    }

    protected class EvaluatingRowVisitor implements RowVisitor {

        private List<TermedStatementEntityEdit> result;
        private QAWarningStore warningStore;

        public EvaluatingRowVisitor(List<TermedStatementEntityEdit> result, QAWarningStore warningStore) {
            this.result = result;
            this.warningStore = warningStore;
        }

        @Override
        public void start(Project project) {
            ;
        }

        @Override
        public boolean visit(Project project, int rowIndex, Row row) {
            ExpressionContext ctxt = new ExpressionContext(siteIri, entityTypeSiteIri, mediaWikiApiEndpoint, rowIndex, row, project.columnModel, warningStore);
            result.addAll(evaluateEntityDocuments(ctxt));
            return false;
        }

        @Override
        public void end(Project project) {
            ;
        }
    }
    
    static public WikibaseSchema reconstruct(String json) throws IOException {
    	return ParsingUtilities.mapper.readValue(json, WikibaseSchema.class);
    }

    static public WikibaseSchema load(Project project, String obj)
            throws Exception {
        return reconstruct(obj);
    }

    @Override
    public void onBeforeSave(Project project) {
    }

    @Override
    public void onAfterSave(Project project) {
    }

    @Override
    public void dispose(Project project) {

    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !WikibaseSchema.class.isInstance(other)) {
            return false;
        }
        WikibaseSchema otherSchema = (WikibaseSchema) other;
        return entityDocumentExprs.equals(otherSchema.getEntityDocumentExpressions());
    }
}
