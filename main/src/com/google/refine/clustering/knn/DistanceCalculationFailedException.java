
package com.google.refine.clustering.knn;

public class DistanceCalculationFailedException extends Exception {

    private String value1, value2;

    public DistanceCalculationFailedException(String message, String value1, String value2) {
        super(message + value1 + " and " + value2);
    }
}
