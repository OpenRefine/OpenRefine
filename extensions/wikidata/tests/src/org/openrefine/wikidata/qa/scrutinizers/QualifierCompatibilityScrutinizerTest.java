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
package org.openrefine.wikidata.qa.scrutinizers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openrefine.wikidata.qa.MockConstraintFetcher;
import org.openrefine.wikidata.testing.TestingData;
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

        scrutinize(makeStatement(disallowedQualifier, mandatoryQualifier));
        assertWarningsRaised(QualifierCompatibilityScrutinizer.disallowedQualifiersType);
    }

    @Test
    public void testMissingQualifier() {
        scrutinize(makeStatement());
        assertWarningsRaised(QualifierCompatibilityScrutinizer.missingMandatoryQualifiersType);
    }

    @Test
    public void testGoodEdit() {
        scrutinize(makeStatement(allowedQualifier, mandatoryQualifier));
        assertNoWarningRaised();
    }

    private Statement makeStatement(Snak... qualifiers) {
        Claim claim = Datamodel.makeClaim(TestingData.existingId,
                Datamodel.makeNoValueSnak(MockConstraintFetcher.mainSnakPid), makeQualifiers(qualifiers));
        return Datamodel.makeStatement(claim, Collections.emptyList(), StatementRank.NORMAL, "");
    }

    private List<SnakGroup> makeQualifiers(Snak[] qualifiers) {
        List<Snak> snaks = Arrays.asList(qualifiers);
        return snaks.stream().map((Snak q) -> Datamodel.makeSnakGroup(Collections.<Snak> singletonList(q)))
                .collect(Collectors.toList());
    }

}
