package org.openrefine.wikidata.qa.scrutinizers;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.openrefine.wikidata.qa.QAWarning;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;

/**
 * Scrutinizes strings for trailing / leading whitespace, and others
 * @author antonin
 *
 */
public class WhitespaceScrutinizer extends ValueScrutinizer {
    
    private Map<String,Pattern> _issuesMap;
    
    public WhitespaceScrutinizer() {
        _issuesMap = new HashMap<>();
        _issuesMap.put("leading-whitespace", Pattern.compile("^\\s"));
        _issuesMap.put("trailing-whitespace", Pattern.compile("\\s$"));
        _issuesMap.put("duplicate-whitespace", Pattern.compile("\\s\\s"));
        
        // https://stackoverflow.com/questions/14565934/regular-expression-to-remove-all-non-printable-characters
        _issuesMap.put("non-printable-characters", Pattern.compile("[\\x00\\x08\\x0B\\x0C\\x0E-\\x1F]"));
    }

    @Override
    public void scrutinize(Value value) {
        String str = null;
        if(MonolingualTextValue.class.isInstance(value)) {
            str = ((MonolingualTextValue)value).getText();
        } else if (StringValue.class.isInstance(value)) {
            str = ((StringValue)value).getString();
        }
        
        if (str != null) {
            for(Entry<String,Pattern> entry : _issuesMap.entrySet()) {
                if(entry.getValue().matcher(str).find()) {
                    emitWarning(entry.getKey(), str);
                }
            }
        }
    }
    
    private void emitWarning(String type, String example) {
        QAWarning warning = new QAWarning(type, null, QAWarning.Severity.WARNING, 1);
        warning.setProperty("example_string", example);
        addIssue(warning);
    }

}
