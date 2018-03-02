package org.openrefine.wikidata.qa.scrutinizers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openrefine.wikidata.qa.MockConstraintFetcher;
import org.openrefine.wikidata.testing.TestingDataGenerator;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;

public class QualifierCompatibilityScrutinizerTest extends StatementScrutinizerTest {
    private Snak disallowedQualifier = Datamodel.makeNoValueSnak(MockConstraintFetcher.qualifierPid);
    private Snak mandatoryQualifier = Datamodel.makeNoValueSnak(MockConstraintFetcher.mandatoryQualifierPid);
    private Snak allowedQualifier = Datamodel.makeNoValueSnak(MockConstraintFetcher.allowedQualifierPid);

    @Override
    public EditScrutinizer getScrutinizer() {
        return new QualifierCompatibilityScrutinizer();
    }
    
    @Test
    public void testDisallowedQualifier() {
        
        scrutinize(makeStatement(disallowedQualifier,mandatoryQualifier));
        assertWarningsRaised(QualifierCompatibilityScrutinizer.disallowedQualifiersType);
    }
    
    @Test
    public void testMissingQualifier() {
        scrutinize(makeStatement());
        assertWarningsRaised(QualifierCompatibilityScrutinizer.missingMandatoryQualifiersType);
    }
    
    @Test
    public void testGoodEdit() {
        scrutinize(makeStatement(allowedQualifier,mandatoryQualifier));
        assertNoWarningRaised();
    }
    
    private Statement makeStatement(Snak... qualifiers) {
        Claim claim = Datamodel.makeClaim(TestingDataGenerator.existingId, 
                Datamodel.makeNoValueSnak(MockConstraintFetcher.mainSnakPid), makeQualifiers(qualifiers));
        return Datamodel.makeStatement(claim, Collections.emptyList(), StatementRank.NORMAL, "");
    }
    private List<SnakGroup> makeQualifiers(Snak[] qualifiers) {
         List<Snak> snaks = Arrays.asList(qualifiers);
         return snaks.stream()
                 .map((Snak q) -> Datamodel.makeSnakGroup(Collections.<Snak>singletonList(q)))
                 .collect(Collectors.toList());
    }

}
