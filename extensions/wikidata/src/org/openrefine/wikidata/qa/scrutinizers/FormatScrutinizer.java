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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.openrefine.wikidata.qa.QAWarning;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

/**
 * A scrutinizer that detects incorrect formats in text values (mostly
 * identifiers).
 * 
 * @author Antonin Delpeuch
 *
 */
public class FormatScrutinizer extends SnakScrutinizer {

    public static final String type = "add-statements-with-invalid-format";

    private Map<PropertyIdValue, Pattern> _patterns;

    public FormatScrutinizer() {
        _patterns = new HashMap<>();
    }

    /**
     * Loads the regex for a property and compiles it to a pattern (this is cached
     * upstream, plus we are doing it only once per property and batch).
     * 
     * @param pid
     *            the id of the property to fetch the constraints for
     * @return
     */
    protected Pattern getPattern(PropertyIdValue pid) {
        if (_patterns.containsKey(pid)) {
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
        if (StringValue.class.isInstance(snak.getValue())) {
            String value = ((StringValue) snak.getValue()).getString();
            PropertyIdValue pid = snak.getPropertyId();
            Pattern pattern = getPattern(pid);
            if (pattern == null) {
                return;
            }
            if (!pattern.matcher(value).matches()) {
                if (added) {
                    QAWarning issue = new QAWarning(type, pid.getId(), QAWarning.Severity.IMPORTANT, 1);
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
