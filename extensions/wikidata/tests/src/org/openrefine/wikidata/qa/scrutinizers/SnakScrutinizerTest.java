package org.openrefine.wikidata.qa.scrutinizers;

import java.util.Collections;
import java.util.List;

import org.openrefine.wikidata.testing.TestingDataGenerator;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;

public abstract class SnakScrutinizerTest extends StatementScrutinizerTest {
    
    public static Snak defaultMainSnak = Datamodel.makeNoValueSnak(Datamodel.makeWikidataPropertyIdValue("P3928"));

    public void scrutinize(Snak snak) {
        Claim claim = Datamodel.makeClaim(TestingDataGenerator.existingId, snak,
                Collections.emptyList());
        Statement statement = Datamodel.makeStatement(claim, Collections.emptyList(), StatementRank.NORMAL, "");
        scrutinize(statement);
    }
    
    public void scrutinizeAsQualifier(Snak snak) {
        Claim claim = Datamodel.makeClaim(TestingDataGenerator.existingId, defaultMainSnak,
                toSnakGroups(snak));
        Statement statement = Datamodel.makeStatement(claim, Collections.emptyList(), StatementRank.NORMAL, "");
        scrutinize(statement);
    }
    
    public void scrutinizeAsReference(Snak snak) {
        Claim claim = Datamodel.makeClaim(TestingDataGenerator.existingId, defaultMainSnak,
                Collections.emptyList());
        Statement statement = Datamodel.makeStatement(claim,
                Collections.singletonList(Datamodel.makeReference(toSnakGroups(snak))), StatementRank.NORMAL, "");
        scrutinize(statement);
    }
    
    private List<SnakGroup> toSnakGroups(Snak snak) {
        return Collections.singletonList(Datamodel.makeSnakGroup(Collections.singletonList(snak)));
    }
}
