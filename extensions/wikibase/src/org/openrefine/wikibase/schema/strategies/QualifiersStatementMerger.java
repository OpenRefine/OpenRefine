
package org.openrefine.wikibase.schema.strategies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.NoValueSnak;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.SomeValueSnak;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Merging strategy which compares statements using not just their main value, but also their qualifiers. It is possible
 * to specify the pids of the qualifiers to take into account when comparing statements.
 * 
 * @author Antonin Delpeuch
 */
public class QualifiersStatementMerger implements StatementMerger {

    protected final ValueMatcher valueMatcher;
    protected final Set<String> pids;

    @JsonCreator
    public QualifiersStatementMerger(
            @JsonProperty("valueMatcher") ValueMatcher valueMatcher,
            @JsonProperty("pids") Set<String> pids) {
        this.valueMatcher = valueMatcher;
        if (pids == null) {
            pids = Collections.emptySet();
        }
        this.pids = pids;
    }

    @Override
    public boolean match(Statement existing, Statement added) {
        // Select the discriminating SnakGroups
        List<SnakGroup> existingDiscriminatingSnaks = discriminatingSnaks(existing.getQualifiers());
        List<SnakGroup> addedDiscriminatingSnaks = discriminatingSnaks(added.getQualifiers());

        return snakGroupsEqual(existingDiscriminatingSnaks, addedDiscriminatingSnaks);
    }

    @Override
    public Statement merge(Statement existing, Statement added) {
        List<SnakGroup> finalQualifiers = new ArrayList<>(existing.getQualifiers());
        for (SnakGroup addedSnakGroup : added.getQualifiers()) {
            PropertyIdValue pid = addedSnakGroup.getProperty();
            // if this is a discriminating qualifier group, then we know it is already in the existing qualifiers.
            // otherwise:
            if (!pids.contains(pid.getId())) {
                OptionalInt index = IntStream.range(0, finalQualifiers.size())
                        .filter(i -> finalQualifiers.get(i).getProperty().getId().equals(pid.getId()))
                        .findFirst();
                if (index.isEmpty()) {
                    finalQualifiers.add(addedSnakGroup);
                } else {
                    finalQualifiers.set(index.getAsInt(),
                            mergeSnakGroups(finalQualifiers.get(index.getAsInt()), addedSnakGroup));
                }
            }
        }
        List<Reference> allReferences = new ArrayList<>(existing.getReferences());
        Set<Reference> seenReferences = new HashSet<>(existing.getReferences());
        for (Reference reference : added.getReferences()) {
            if (!seenReferences.contains(reference)) {
                seenReferences.add(reference);
                allReferences.add(reference);
            }
        }

        Statement merged = Datamodel.makeStatement(
                Datamodel.makeClaim(existing.getSubject(), existing.getMainSnak(), finalQualifiers),
                allReferences,
                existing.getRank(),
                existing.getStatementId());
        return merged;
    }

    /**
     * Merge two snak groups, ensuring no duplicate snak appears with respect to the {@link ValueMatcher} provided.
     * 
     * @param snakGroup1
     * @param snakGroup2
     * @return
     */
    protected SnakGroup mergeSnakGroups(SnakGroup snakGroup1, SnakGroup snakGroup2) {
        List<Snak> finalSnaks = new ArrayList<>(snakGroup1.getSnaks());
        for (Snak snak : snakGroup2.getSnaks()) {
            if (!finalSnaks.stream().anyMatch(finalSnak -> snakEquality(finalSnak, snak))) {
                finalSnaks.add(snak);
            }
        }
        return Datamodel.makeSnakGroup(finalSnaks);
    }

    /**
     * Given a list of qualifiers, extract the discriminating ones.
     */
    public List<SnakGroup> discriminatingSnaks(List<SnakGroup> snaks) {
        if (pids.isEmpty()) {
            return snaks;
        } else {
            return snaks.stream()
                    .filter(snak -> pids.contains(snak.getProperty().getId()))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Are these lists of snak groups equal up to the {@link ValueMatcher} provided?
     */
    protected boolean snakGroupsEqual(List<SnakGroup> snakGroups1, List<SnakGroup> snakGroups2) {
        return snakGroupsIncluded(snakGroups1, snakGroups2) && snakGroupsIncluded(snakGroups2, snakGroups1);
    }

    /**
     * Is the first list of snak groups included in the second one?
     */
    protected boolean snakGroupsIncluded(List<SnakGroup> snakGroups1, List<SnakGroup> snakGroups2) {
        return snakGroups1.stream()
                .allMatch(snakGroup1 -> snakGroups2.stream().anyMatch(
                        snakGroup2 -> snakGroupEquality(snakGroup1, snakGroup2)));
    }

    /**
     * Are these two snak groups equal up to the {@link ValueMatcher} provided?
     * 
     * @param snakGroup1
     * @param snakGroup2
     * @return
     */
    protected boolean snakGroupEquality(SnakGroup snakGroup1, SnakGroup snakGroup2) {
        return snaksIncluded(snakGroup1.getSnaks(), snakGroup2.getSnaks()) &&
                snaksIncluded(snakGroup2.getSnaks(), snakGroup1.getSnaks());
    }

    /**
     * Is this list of snak included in the other, up to the {@link ValueMatcher} provided?
     * 
     * @param snaks1
     * @param snaks2
     * @return
     */
    protected boolean snaksIncluded(List<Snak> snaks1, List<Snak> snaks2) {
        return snaks1.stream()
                .allMatch(snak1 -> snaks2.stream()
                        .anyMatch(snak2 -> snakEquality(snak1, snak2)));
    }

    /**
     * Are these two snaks equal up to the {@link ValueMatcher} provided?
     * 
     * @param snak1
     * @param snak2
     * @return
     */
    protected boolean snakEquality(Snak snak1, Snak snak2) {
        if (!snak1.getPropertyId().equals(snak2.getPropertyId())) {
            return false;
        }
        if (snak1 instanceof NoValueSnak && snak2 instanceof NoValueSnak) {
            return true;
        } else if (snak1 instanceof SomeValueSnak && snak2 instanceof SomeValueSnak) {
            return true;
        } else {
            Value value1 = ((ValueSnak) snak1).getValue();
            Value value2 = ((ValueSnak) snak2).getValue();
            return valueMatcher.match(value1, value2);
        }
    }

    @JsonProperty("valueMatcher")
    public ValueMatcher getValueMatcher() {
        return valueMatcher;
    }

    @JsonProperty("pids")
    public Set<String> getPids() {
        return pids;
    }

    @Override
    public String toString() {
        return "QualifiersStatementMerger [valueMatcher=" + valueMatcher +
                ", pids=" + pids + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(pids, valueMatcher);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        QualifiersStatementMerger other = (QualifiersStatementMerger) obj;
        return Objects.equals(pids, other.pids) && Objects.equals(valueMatcher, other.valueMatcher);
    }
}
