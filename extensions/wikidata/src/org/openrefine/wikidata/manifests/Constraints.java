package org.openrefine.wikidata.manifests;

import org.openrefine.wikidata.manifests.constraints.*;

public interface Constraints {

    AllowedEntityTypesConstraint getAllowedEntityTypesConstraint();

    AllowedQualifiersConstraint getAllowedQualifiersConstraint();

    AllowedUnitsConstraint getAllowedUnitsConstraint();

    CitationNeededConstraint getCitationNeededConstraint();

    ConflictsWithConstraint getConflictsWithConstraint();

    ContemporaryConstraint getContemporaryConstraint();

    DifferenceWithinRangeConstraint getDifferenceWithinRangeConstraint();

    DistinctValuesConstraint getDistinctValuesConstraint();

    FormatConstraint getFormatConstraint();

    IntegerConstraint getIntegerConstraint();

    InverseConstraint getInverseConstraint();

    ItemRequiresStatementConstraint getItemRequiresStatementConstraint();

    MandatoryQualifierConstraint getMandatoryQualifierConstraint();

    MultiValueConstraint getMultiValueConstraint();

    NoBoundsConstraint getNoBoundsConstraint();

    NoneOfConstraint getNoneOfConstraint();

    OneOfConstraint getOneOfConstraint();

    OneOfQualifierValuePropertyConstraint getOneOfQualifierValuePropertyConstraint();

    PropertyScopeConstraint getPropertyScopeConstraint();

    RangeConstraint getRangeConstraint();

    SingleBestValueConstraint getSingleBestValueConstraint();

    SingleValueConstraint getSingleValueConstraint();

    SymmetricConstraint getSymmetricConstraint();

    TypeConstraint getTypeConstraint();

    ValueRequiresStatementConstraint getValueRequiresStatementConstraint();

    ValueTypeConstraint getValueTypeConstraint();
}
