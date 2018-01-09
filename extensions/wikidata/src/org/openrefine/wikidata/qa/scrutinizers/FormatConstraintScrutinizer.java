package org.openrefine.wikidata.qa.scrutinizers;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.openrefine.wikidata.qa.ConstraintFetcher;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
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
    
    private Map<String, Pattern> _patterns;
    private ConstraintFetcher _fetcher;
    
    public FormatConstraintScrutinizer() {
        _patterns = new HashMap<>();
        _fetcher = new ConstraintFetcher();
    }
    
    /**
     * Loads the regex for a property and compiles it to a pattern
     * (this is cached upstream, plus we are doing it only once per
     * property and batch).
     * @param pid the id of the property to fetch the constraints for
     * @return
     */
    protected Pattern getPattern(String pid) {
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
            String pid = snak.getPropertyId().getId();
            Pattern pattern = getPattern(pid);
            if (!pattern.matcher(value).matches()) {
                if (added) {
                    important("add-statements-with-invalid-format");
                } else {
                    info("remove-statements-with-invalid-format");
                }
            }
        }

    }

}
