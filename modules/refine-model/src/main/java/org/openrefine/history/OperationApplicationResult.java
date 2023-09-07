
package org.openrefine.history;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.operations.ChangeResult;
import org.openrefine.operations.exceptions.OperationException;

/**
 * Result of the application of an operation, which can either succeed in adding a new history entry to the project, or
 * fail with an error. In both cases, the operation has the opportunity to expose additional data:
 * <ul>
 * <li>For success, return a subclass of {@link ChangeResult} which stores additional information. This information
 * should be serialized in JSON as well.</li>
 * <li>For failure, the exception itself can carry more data (and should also be serializable in JSON).</li>
 * </ul>
 * 
 * @author Antonin Delpeuch
 *
 */
public class OperationApplicationResult {

    private final boolean success;
    private final HistoryEntry historyEntry;
    private final ChangeResult changeResult;
    private final OperationException exception;

    /**
     * Construct an instance representing a successful application.
     */
    public OperationApplicationResult(
            HistoryEntry historyEntry,
            ChangeResult changeResult) {
        this.success = true;
        this.historyEntry = historyEntry;
        this.changeResult = changeResult;
        this.exception = null;
    }

    /**
     * Construct an instance representing an unsuccessful application.
     */
    public OperationApplicationResult(
            OperationException exception) {
        this.success = false;
        this.historyEntry = null;
        this.changeResult = null;
        this.exception = exception;
    }

    @JsonProperty("success")
    public boolean isSuccess() {
        return success;
    }

    @JsonProperty("historyEntry")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public HistoryEntry getHistoryEntry() {
        return historyEntry;
    }

    @JsonProperty("changeResult")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public ChangeResult getChangeResult() {
        return changeResult;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("error")
    public OperationException getException() {
        return exception;
    }

    @Override
    public int hashCode() {
        return Objects.hash(changeResult, exception, historyEntry, success);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OperationApplicationResult other = (OperationApplicationResult) obj;
        return Objects.equals(changeResult, other.changeResult) && Objects.equals(exception, other.exception)
                && Objects.equals(historyEntry, other.historyEntry) && success == other.success;
    }
}
