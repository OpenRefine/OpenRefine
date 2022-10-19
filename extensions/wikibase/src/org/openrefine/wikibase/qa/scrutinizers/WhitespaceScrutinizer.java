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

import org.openrefine.wikibase.qa.QAWarning;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * Scrutinizes strings for trailing / leading whitespace, and others
 * 
 * @author Antonin Delpeuch
 *
 */
public class WhitespaceScrutinizer extends ValueScrutinizer {

    private Map<String, Pattern> _issuesMap;

    public static final String duplicateWhitespaceType = "duplicate-whitespace";
    public static final String nonPrintableCharsType = "non-printable-characters";

    public WhitespaceScrutinizer() {
        _issuesMap = new HashMap<>();
        _issuesMap.put(duplicateWhitespaceType, Pattern.compile("\\s\\s"));

        // https://stackoverflow.com/questions/14565934/regular-expression-to-remove-all-non-printable-characters
        _issuesMap.put(nonPrintableCharsType, Pattern.compile("[\\x00\\x03\\x08\\x0B\\x0C\\x0E-\\x1F]"));
    }

    @Override
    public boolean prepareDependencies() {
        return true;
    }

    @Override
    public void scrutinize(Value value) {
        String str = null;
        if (value instanceof MonolingualTextValue) {
            str = ((MonolingualTextValue) value).getText();
        } else if (value instanceof StringValue) {
            str = ((StringValue) value).getString();
        }

        if (str != null) {
            for (Entry<String, Pattern> entry : _issuesMap.entrySet()) {
                if (entry.getValue().matcher(str).find()) {
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
