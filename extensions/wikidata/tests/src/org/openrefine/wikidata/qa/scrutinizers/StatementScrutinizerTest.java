package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

public abstract class StatementScrutinizerTest extends ScrutinizerTest {

    public void scrutinize(Statement statement) {
        ItemUpdate update = new ItemUpdateBuilder((ItemIdValue)statement.getClaim().getSubject())
                .addStatement(statement).build();
        scrutinize(update);
    }

}
