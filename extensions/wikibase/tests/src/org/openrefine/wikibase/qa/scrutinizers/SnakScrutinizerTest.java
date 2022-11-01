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

package org.openrefine.wikibase.qa.scrutinizers;

import java.util.Collections;
import java.util.List;

import org.openrefine.wikibase.testing.TestingData;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;

public abstract class SnakScrutinizerTest extends StatementScrutinizerTest {

    public static Snak defaultMainSnak = Datamodel.makeNoValueSnak(Datamodel.makeWikidataPropertyIdValue("P3928"));

    public void scrutinize(Snak snak) {
        Claim claim = Datamodel.makeClaim(TestingData.existingId, snak, Collections.emptyList());
        Statement statement = Datamodel.makeStatement(claim, Collections.emptyList(), StatementRank.NORMAL, "");
        scrutinize(statement);
    }

    public void scrutinizeAsQualifier(Snak snak) {
        Claim claim = Datamodel.makeClaim(TestingData.existingId, defaultMainSnak, toSnakGroups(snak));
        Statement statement = Datamodel.makeStatement(claim, Collections.emptyList(), StatementRank.NORMAL, "");
        scrutinize(statement);
    }

    public void scrutinizeAsReference(Snak snak) {
        Claim claim = Datamodel.makeClaim(TestingData.existingId, defaultMainSnak, Collections.emptyList());
        Statement statement = Datamodel.makeStatement(claim,
                Collections.singletonList(Datamodel.makeReference(toSnakGroups(snak))), StatementRank.NORMAL, "");
        scrutinize(statement);
    }

    private List<SnakGroup> toSnakGroups(Snak snak) {
        return Collections.singletonList(Datamodel.makeSnakGroup(Collections.singletonList(snak)));
    }
}
