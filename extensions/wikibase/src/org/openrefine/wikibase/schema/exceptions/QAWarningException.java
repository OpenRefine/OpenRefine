
package org.openrefine.wikibase.schema.exceptions;

import org.openrefine.wikibase.qa.QAWarning;

/**
 * Exception thrown during schema evaluation to report an error as a QA validation warning.
 * 
 * Throwing this exception will halt schema evaluation and report this warning to the user, so it should be used for
 * rather important warnings.
 * 
 * @author Antonin Delpeuch
 *
 */
public class QAWarningException extends Exception {

    private static final long serialVersionUID = 9108065465354881096L;

    private QAWarning warning;

    public QAWarningException(QAWarning warning) {
        this.warning = warning;
    }

    public QAWarning getWarning() {
        return warning;
    }
}
