
package org.openrefine.wikibase.schema;

import static org.testng.Assert.assertEquals;

import java.util.Collections;
import java.util.stream.Collectors;

import org.openrefine.wikibase.qa.QAWarning;
import org.openrefine.wikibase.qa.QAWarning.Severity;
import org.openrefine.wikibase.schema.entityvalues.ReconMediaInfoIdValue;
import org.openrefine.wikibase.schema.strategies.StatementEditingMode;
import org.openrefine.wikibase.schema.strategies.StatementMerger;
import org.openrefine.wikibase.testing.JacksonSerializationTest;
import org.openrefine.wikibase.updates.MediaInfoEdit;
import org.openrefine.wikibase.updates.MediaInfoEditBuilder;
import org.openrefine.wikibase.updates.StatementEdit;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;

import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ColumnModel;
import com.google.refine.model.ModelException;
import com.google.refine.util.TestUtils;

public class WbMediaInfoEditExprTest extends WbExpressionTest<MediaInfoEdit> {

    public WbMediaInfoEditExpr expr;
    Cell matchedCell = recon("M23");
    Cell matchedCellWrongType = recon("Q23");
    EntityIdValue subject = new ReconMediaInfoIdValue(matchedCell.recon, (String) matchedCell.value);
    MonolingualTextValue label = Datamodel.makeMonolingualTextValue("my label", "en");
    Snak mainsnak = Datamodel.makeValueSnak(Datamodel.makeWikidataPropertyIdValue("P908"),
            Datamodel.makeGlobeCoordinatesValue(3.898, 4.389,
                    WbLocationConstant.defaultPrecision, GlobeCoordinatesValue.GLOBE_EARTH));
    Claim fullClaim = Datamodel.makeClaim(subject, mainsnak,
            Collections.emptyList());
    Statement evaluatedStatement = Datamodel.makeStatement(fullClaim, Collections.emptyList(),
            StatementRank.NORMAL, "");
    StatementEdit fullStatement = new StatementEdit(
            evaluatedStatement,
            StatementMerger.FORMER_DEFAULT_STRATEGY,
            StatementEditingMode.ADD_OR_MERGE);

    public String jsonRepresentation;

    public WbMediaInfoEditExprTest() {
        WbStatementGroupExprTest sgt = new WbStatementGroupExprTest();
        WbNameDescExpr nde = new WbNameDescExpr(WbNameDescExpr.NameDescType.LABEL,
                new WbMonolingualExpr(new WbLanguageConstant("en", "English"), new WbStringVariable("column D")));
        WbEntityVariable subjectExpr = new WbEntityVariable("column E");
        expr = new WbMediaInfoEditExpr(subjectExpr, Collections.singletonList(nde), Collections.singletonList(sgt.expr), null, null, null,
                false);

        jsonRepresentation = "{\"type\":\"wbmediainfoeditexpr\",\"subject\":{\"type\":\"wbentityvariable\",\"columnName\":\"column E\"},"
                + "\"filePath\":null,"
                + "\"fileName\":null,"
                + "\"wikitext\":null,"
                + "\"overrideWikitext\":false,"
                + "\"nameDescs\":[{\"name_type\":\"LABEL\",\"value\":{\"type\":\"wbmonolingualexpr\",\"language\":"
                + "{\"type\":\"wblanguageconstant\",\"id\":\"en\",\"label\":\"English\"},"
                + "\"value\":{\"type\":\"wbstringvariable\",\"columnName\":\"column D\"}}}" + "],\"statementGroups\":["
                + sgt.jsonRepresentation + "]}";
    }

    @Test
    public void testEvaluate() {
        setRow("", "", "3.898,4.389", "my label", matchedCell);
        MediaInfoEdit result = new MediaInfoEditBuilder(subject).addLabel(label, true).addStatement(fullStatement)
                .build();
        evaluatesTo(result, expr);
    }

    @Test
    public void testValidate() throws ModelException {
        ColumnModel columnModel = new ColumnModel();
        columnModel.addColumn(0, new Column(0, "column A"), false);
        columnModel.addColumn(0, new Column(0, "column B"), false);
        columnModel.addColumn(0, new Column(0, "column C"), false);
        columnModel.addColumn(0, new Column(0, "column D"), false);
        columnModel.addColumn(0, new Column(0, "column E"), false);

        hasNoValidationError(expr, columnModel);
        hasValidationError("No subject provided", new WbMediaInfoEditExpr(null, null, null, null, null, null, false));
        hasValidationError("Null term in MediaInfo entity", new WbMediaInfoEditExpr(new WbEntityVariable("column E"),
                Collections.singletonList(null), null, null, null, null, false), columnModel);
        hasValidationError("Null statement in MediaInfo entity", new WbMediaInfoEditExpr(new WbEntityVariable("column E"),
                null, Collections.singletonList(null), null, null, null, false), columnModel);
    }

    @Test
    public void testEvaluateWrongSubjectType() {
        setRow("", "", "3.898,4.389", "my label", matchedCellWrongType);
        QAWarning warning = new QAWarning(WbMediaInfoEditExpr.INVALID_SUBJECT_WARNING_TYPE, "", Severity.CRITICAL, 1);
        warning.setProperty("example", "Q23");
        evaluatesToWarning(warning, expr);
    }

    @Test
    public void testSubjectSkipped() {
        setRow("", "", "3.898,4.389", "my label", "not reconciled");
        isSkipped(expr);
    }

    @Test
    public void testStatementSkipped() {
        setRow("", "", "3.898,invalid4.389", "my label", matchedCell);
        MediaInfoEdit result = new MediaInfoEditBuilder(subject).addLabel(label, true).build();
        evaluatesTo(result, expr);
    }

    @Test
    public void testLabelSkipped() {
        setRow("", "", "3.898,4.389", "", matchedCell);
        MediaInfoEdit result = new MediaInfoEditBuilder(subject).addStatement(fullStatement).build();
        evaluatesTo(result, expr);
    }

    @Test
    public void testFilePathSerialization() {
        WbMediaInfoEditExpr filePathExpr = new WbMediaInfoEditExpr(
                new WbEntityVariable("column E"),
                Collections.emptyList(), Collections.emptyList(), new WbStringConstant("C:\\Foo.png"), null, null, false);
        String expressionJson = "{\"type\":\"wbmediainfoeditexpr\",\"subject\":{\"type\":\"wbentityvariable\",\"columnName\":\"column E\"},"
                + "\"filePath\":{\"type\":\"wbstringconstant\",\"value\":\"C:\\\\Foo.png\"},"
                + "\"fileName\":null,"
                + "\"wikitext\":null,"
                + "\"overrideWikitext\":false,"
                + "\"nameDescs\":[],\"statementGroups\":[]}";
        TestUtils.isSerializedTo(filePathExpr, expressionJson);
    }

    @Test
    public void testFilePathEvaluationWithLocalPath() {
        WbMediaInfoEditExpr filePathExpr = new WbMediaInfoEditExpr(
                new WbEntityVariable("column E"),
                Collections.emptyList(), Collections.emptyList(), new WbStringConstant("C:\\Foo.png"), null, null, false);

        setRow("", "", "3.898,4.389", "my label", matchedCell);

        MediaInfoEdit result = new MediaInfoEditBuilder(subject).addFilePath("C:\\Foo.png").build();
        evaluatesTo(result, filePathExpr);
    }

    @Test
    public void testFilePathEvaluationWithURL() {
        WbMediaInfoEditExpr filePathExpr = new WbMediaInfoEditExpr(
                new WbEntityVariable("column E"),
                Collections.emptyList(), Collections.emptyList(), new WbStringConstant("C:\\Foo.png"), null, null, false);

        setRow("", "", "3.898,4.389", "my label", matchedCell);

        MediaInfoEdit result = new MediaInfoEditBuilder(subject).addFilePath("C:\\Foo.png").build();
        evaluatesTo(result, filePathExpr);
    }

    @Test
    public void testFilePathEvaluationWithInvalidPath() {
        WbMediaInfoEditExpr filePathExpr = new WbMediaInfoEditExpr(
                new WbEntityVariable("column E"),
                Collections.emptyList(), Collections.emptyList(), new WbStringConstant("C:\\Foo.png"), null, null, false);

        setRow("", "", "3.898,4.389", "my label", matchedCell);

        MediaInfoEdit result = new MediaInfoEditBuilder(subject).addFilePath("C:\\Foo.png").build();
        evaluatesTo(result, filePathExpr);
    }

    @Test
    public void testFileNameSerialization() {
        WbMediaInfoEditExpr filePathExpr = new WbMediaInfoEditExpr(
                new WbEntityVariable("column E"),
                Collections.emptyList(), Collections.emptyList(), null, new WbStringConstant("Foo.png"), null, false);
        String expressionJson = "{\"type\":\"wbmediainfoeditexpr\",\"subject\":{\"type\":\"wbentityvariable\",\"columnName\":\"column E\"},"
                + "\"fileName\":{\"type\":\"wbstringconstant\",\"value\":\"Foo.png\"},"
                + "\"filePath\":null,"
                + "\"wikitext\":null,"
                + "\"overrideWikitext\":false,"
                + "\"nameDescs\":[],\"statementGroups\":[]}";
        TestUtils.isSerializedTo(filePathExpr, expressionJson);
    }

    @Test
    public void testFileNameEvaluation() {
        WbMediaInfoEditExpr filePathExpr = new WbMediaInfoEditExpr(
                new WbEntityVariable("column E"),
                Collections.emptyList(), Collections.emptyList(), null, new WbStringConstant("Foo.png"), null, false);

        setRow("", "", "3.898,4.389", "my label", matchedCell);

        MediaInfoEdit result = new MediaInfoEditBuilder(subject).addFileName("Foo.png").build();
        evaluatesTo(result, filePathExpr);
    }

    @Test
    public void testFileNameNormalization() {
        WbMediaInfoEditExpr filePathExpr = new WbMediaInfoEditExpr(
                new WbEntityVariable("column E"),
                Collections.emptyList(), Collections.emptyList(), null, new WbStringConstant("Foo:bar.png"), null, false);

        setRow("", "", "3.898,4.389", "my label", matchedCell);

        MediaInfoEdit result = new MediaInfoEditBuilder(subject).addFileName("Foo-bar.png").build();
        evaluatesTo(result, filePathExpr);
        assertEquals(warningStore.getWarnings().stream().map(QAWarning::getType).collect(Collectors.toList()),
                Collections.singletonList(WbMediaInfoEditExpr.REPLACED_CHARACTERS_IN_FILENAME));
    }

    @Test
    public void testWikitextEvaluation() {
        WbMediaInfoEditExpr wikitextExpr = new WbMediaInfoEditExpr(
                new WbEntityVariable("column E"),
                Collections.emptyList(), Collections.emptyList(), null, null, new WbStringConstant("my new wikitext"), true);

        setRow("", "", "3.898,4.389", "my label", matchedCell);
        MediaInfoEdit result = new MediaInfoEditBuilder(subject)
                .addWikitext("my new wikitext")
                .setOverrideWikitext(true)
                .build();
        evaluatesTo(result, wikitextExpr);
    }

    @Test
    public void testSerialize() {
        JacksonSerializationTest.canonicalSerialization(WbMediaInfoEditExpr.class, expr, jsonRepresentation);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testUnmodifiableNameDescsList() {
        expr.getNameDescs().clear();
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testUnmodifiableStatementGroupsList() {
        expr.getStatementGroups().clear();
    }
}
