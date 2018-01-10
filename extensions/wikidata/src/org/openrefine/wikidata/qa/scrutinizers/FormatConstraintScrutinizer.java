package org.openrefine.wikidata.qa.scrutinizers;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.openrefine.wikidata.qa.QAWarning;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

/**
 * A scrutinizer that detects incorrect formats in text values
 * (mostly identifiers).
 * 
 * @author antonin
 *
 */
public class FormatConstraintScrutinizer extends SnakScrutinizer {
    
    private Map<PropertyIdValue, Pattern> _patterns;
    
    public FormatConstraintScrutinizer() {
        _patterns = new HashMap<>();
    }
    
    /**
     * Loads the regex for a property and compiles it to a pattern
     * (this is cached upstream, plus we are doing it only once per
     * property and batch).
     * @param pid the id of the property to fetch the constraints for
     * @return
     */
    protected Pattern getPattern(PropertyIdValue pid) {
        if(_patterns.containsKey(pid)) {
            return _patterns.get(pid);
        } else {
            String regex = _fetcher.getFormatRegex(pid);
            Pattern pattern = null;
            if (regex != null) {
                pattern = Pattern.compile(regex);
            }
            _patterns.put(pid, pattern);
            return pattern;
        }
    }

    @Override
    public void scrutinize(Snak snak, EntityIdValue entityId, boolean added) {
        if(StringValue.class.isInstance(snak.getValue())) {
            String value = ((StringValue) snak.getValue()).getString();
            PropertyIdValue pid = snak.getPropertyId();
            Pattern pattern = getPattern(pid);
            if (!pattern.matcher(value).matches()) {
                if (added) {
                    QAWarning issue = new QAWarning(
                            "add-statements-with-invalid-format",
                            pid.getId(),
                            QAWarning.Severity.IMPORTANT,
                            1);
                    issue.setProperty("property_entity", pid);
                    issue.setProperty("regex", pattern.toString());
                    issue.setProperty("example_value", value);
                    issue.setProperty("example_item_entity", entityId);
                    addIssue(issue);
                } else {
                    info("remove-statements-with-invalid-format");
                }
            }
        }

    }

}
