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

import org.openrefine.wikidata.manifests.Manifest;
import org.openrefine.wikidata.manifests.ManifestException;
import org.openrefine.wikidata.manifests.ManifestParser;
import org.openrefine.wikidata.qa.ConstraintFetcher;
import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.qa.QAWarningStore;
import org.openrefine.wikidata.testing.TestingData;
import org.openrefine.wikidata.updates.TermedStatementEntityUpdate;
import org.testng.annotations.BeforeMethod;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public abstract class ScrutinizerTest {

    private static Manifest manifest;

    static {
        try {
            String json = TestingData.jsonFromFile("manifest/wikidata-manifest-v1.0.json");
            manifest = ManifestParser.parse(json);
        } catch (IOException | ManifestException e) {
            e.printStackTrace();
        }
    }

    public abstract EditScrutinizer getScrutinizer();

    private EditScrutinizer scrutinizer;
    private QAWarningStore store;

    @BeforeMethod
    public void setUp() {
        store = new QAWarningStore();
        scrutinizer = getScrutinizer();
        scrutinizer.setStore(store);
        scrutinizer.setManifest(manifest);
        scrutinizer.prepareDependencies();
    }

    public void scrutinize(TermedStatementEntityUpdate... updates) {
        scrutinizer.batchIsBeginning();
        for (TermedStatementEntityUpdate update : Arrays.asList(updates)) {
            if (!update.isNull()) {
                scrutinizer.scrutinize(update);
            }
        }
        scrutinizer.batchIsFinished();
    }

    public void assertWarningsRaised(String... types) {
        assertEquals(getWarningTypes(), Arrays.asList(types).stream().collect(Collectors.toSet()));
    }

    public void assertWarningRaised(QAWarning warning) {
        assertTrue(store.getWarnings().contains(warning));
    }

    public void assertNoWarningRaised() {
        assertWarningsRaised();
    }

    public Set<String> getWarningTypes() {
        return store.getWarnings().stream().map(w -> w.getType()).collect(Collectors.toSet());
    }

    public void setFetcher(ConstraintFetcher fetcher) {
        scrutinizer.setFetcher(fetcher);
    }

    public List<Statement> constraintParameterStatementList(ItemIdValue itemIdValue, List<SnakGroup> listSnakGroup) {
        PropertyIdValue propertyIdValue = Datamodel.makeWikidataPropertyIdValue("P2302");
        Snak snakValue = Datamodel.makeValueSnak(propertyIdValue, itemIdValue);

        Claim claim = Datamodel.makeClaim(itemIdValue, snakValue, listSnakGroup);

        Reference reference = Datamodel.makeReference(listSnakGroup);
        List<Reference> referenceList = Collections.singletonList(reference);

        Statement statement = Datamodel.makeStatement(claim, referenceList, StatementRank.NORMAL,
                "P2302$77BD7FE4-C051-4776-855C-543F0CE697D0");
        List<Statement> statements = Collections.singletonList(statement);

        return statements;
    }

    public List<SnakGroup> makeSnakGroupList(Snak... snaks) {
        Map<PropertyIdValue, List<Snak>> propertySnakMap = new HashMap<>();
        for (Snak snak : snaks) {
            PropertyIdValue pid = snak.getPropertyId();
            List<Snak> snakList;
            if (propertySnakMap.containsKey(pid)) {
                snakList = propertySnakMap.get(pid);
            } else {
                snakList = new ArrayList<>();
            }
            snakList.add(snak);
            propertySnakMap.put(pid, snakList);
        }

        List<SnakGroup> snakGroupList = new ArrayList<>();
        for (List<Snak> snakList : propertySnakMap.values()) {
            snakGroupList.add(Datamodel.makeSnakGroup(snakList));
        }

        return snakGroupList;
    }

}
