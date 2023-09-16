
package org.openrefine.wikibase.schema.strategies;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;

import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;

/**
 * Generic matcher which attempts to equate values which should generally be considered equivalent in most data import
 * contexts.
 * 
 * @author Antonin Delpeuch
 *
 */
public class LaxValueMatcher implements ValueMatcher {

    @Override
    public boolean match(Value existing, Value added) {
        if (existing instanceof EntityIdValue && added instanceof EntityIdValue) {
            // only compare the string ids, not the siteIRIs, to avoid
            // federation-related issues. We expect that in a given context,
            // only entities from a given Wikibase can appear.
            // TODO revisit this when (if?) federation support makes it possible
            // to mix up entities from different Wikibases in the same data slot
            return ((EntityIdValue) existing).getId().equals(((EntityIdValue) added).getId());

        } else if (existing instanceof StringValue && added instanceof StringValue) {
            // disregard trailing whitespace differences
            String existingStr = ((StringValue) existing).getString().trim();
            String addedStr = ((StringValue) added).getString().trim();
            // if they look like URLs, then http(s) and trailing slashes do not matter
            try {
                URI existingUrl = extraURINormalize(new URI(existingStr).normalize());
                URI addedUrl = extraURINormalize(new URI(addedStr).normalize());
                return existingUrl.equals(addedUrl);
            } catch (URISyntaxException e) {
                ; // fall back on basic comparison
            }
            return existingStr.equals(addedStr);

        } else if (existing instanceof MonolingualTextValue && added instanceof MonolingualTextValue) {
            // ignore differences of trailing whitespace
            MonolingualTextValue existingMTV = (MonolingualTextValue) existing;
            MonolingualTextValue addedMTV = (MonolingualTextValue) added;
            return (existingMTV.getLanguageCode().equals(addedMTV.getLanguageCode()) &&
                    existingMTV.getText().trim().equals(addedMTV.getText().trim()));

        } else if (existing instanceof QuantityValue && added instanceof QuantityValue) {
            QuantityValue existingQuantity = (QuantityValue) existing;
            QuantityValue addedQuantity = (QuantityValue) added;
            BigDecimal existingLowerBound = existingQuantity.getLowerBound();
            BigDecimal addedLowerBound = addedQuantity.getLowerBound();
            BigDecimal existingUpperBound = existingQuantity.getUpperBound();
            BigDecimal addedUpperBound = addedQuantity.getUpperBound();
            // artificially set bounds for quantities which have neither lower nor upper bounds
            if (existingLowerBound == null && existingUpperBound == null) {
                existingLowerBound = existingQuantity.getNumericValue();
                existingUpperBound = existingQuantity.getNumericValue();
            }
            if (addedLowerBound == null && addedUpperBound == null) {
                addedLowerBound = addedQuantity.getNumericValue();
                addedUpperBound = addedQuantity.getNumericValue();
            }

            if (existingQuantity.getUnit().equals(addedQuantity.getUnit()) &&
                    (existingLowerBound != null) && (addedLowerBound != null) &&
                    (existingUpperBound != null) && (addedUpperBound != null)) {
                // Consider the two values to be equal when their confidence interval overlaps
                return ((existingLowerBound.compareTo(addedLowerBound) <= 0 && addedLowerBound.compareTo(existingUpperBound) <= 0) ||
                        (addedLowerBound.compareTo(existingLowerBound) <= 0 && existingLowerBound.compareTo(addedUpperBound) <= 0));
            }

        } else if (existing instanceof GlobeCoordinatesValue && added instanceof GlobeCoordinatesValue) {
            GlobeCoordinatesValue addedCoords = (GlobeCoordinatesValue) added;
            GlobeCoordinatesValue existingCoords = (GlobeCoordinatesValue) existing;
            if (!addedCoords.getGlobeItemId().getId().equals(existingCoords.getGlobeItemId().getId())) {
                return false;
            }

            double addedMinLon = addedCoords.getLongitude() - addedCoords.getPrecision();
            double addedMaxLon = addedCoords.getLongitude() + addedCoords.getPrecision();
            double addedMinLat = addedCoords.getLatitude() - addedCoords.getPrecision();
            double addedMaxLat = addedCoords.getLatitude() + addedCoords.getPrecision();
            double existingMinLon = existingCoords.getLongitude() - existingCoords.getPrecision();
            double existingMaxLon = existingCoords.getLongitude() + existingCoords.getPrecision();
            double existingMinLat = existingCoords.getLatitude() - existingCoords.getPrecision();
            double existingMaxLat = existingCoords.getLatitude() + existingCoords.getPrecision();

            // return true when the two "rectangles" (in coordinate space) overlap (not strictly)
            return ((addedMinLon <= existingMinLon && addedMinLat <= existingMinLat && existingMinLon <= addedMaxLon
                    && existingMinLat <= addedMaxLat) ||
                    (existingMinLon <= addedMinLon && existingMinLat <= addedMinLat && addedMinLon <= existingMaxLon
                            && addedMinLat <= existingMaxLat));

        } else if (existing instanceof TimeValue && added instanceof TimeValue) {
            TimeValue existingTime = (TimeValue) existing;
            TimeValue addedTime = (TimeValue) added;

            if (!existingTime.getPreferredCalendarModel().equals(addedTime.getPreferredCalendarModel())) {
                return false;
            }

            int minPrecision = Math.min(existingTime.getPrecision(), addedTime.getPrecision());
            if (minPrecision <= 9) {
                // the precision is a multiple of years
                long yearPrecision = (long) Math.pow(10, 9 - minPrecision);
                long addedValue = addedTime.getYear() / yearPrecision;
                long existingValue = existingTime.getYear() / yearPrecision;
                return addedValue == existingValue;
            } else if (minPrecision == 10) {
                // month precision
                return (addedTime.getYear() == existingTime.getYear()
                        && addedTime.getMonth() == existingTime.getMonth());
            } else if (minPrecision == 11) {
                // day precision
                return (addedTime.getYear() == existingTime.getYear()
                        && addedTime.getMonth() == existingTime.getMonth()
                        && addedTime.getDay() == existingTime.getDay());
            }

            // TODO possible improvements: bounds support, timezone support

        }
        // fall back to exact comparison for other datatypes
        return existing.equals(added);
    }

    // utility function to remove some more differences from URLs
    protected URI extraURINormalize(URI uri) throws URISyntaxException {
        String scheme = uri.getScheme();
        String userInfo = uri.getUserInfo();
        String host = uri.getHost();
        int port = uri.getPort();
        String path = uri.getPath();
        String query = uri.getQuery();
        String fragment = uri.getFragment();
        if ("https".equals(scheme)) {
            scheme = "http";
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return new URI(scheme, userInfo, host, port, path, query, fragment);
    }

    @Override
    public String toString() {
        return "LaxValueMatcher";
    }

    @Override
    public int hashCode() {
        // constant because this object has no fields
        return 2127;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        return getClass() == obj.getClass();
    }
}
