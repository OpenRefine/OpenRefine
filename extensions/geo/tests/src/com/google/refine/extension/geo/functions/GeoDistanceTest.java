/*

Copyright 2025, OpenRefine contributors
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of the copyright holder nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.extension.geo.functions;

import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;

public class GeoDistanceTest {

    private static Properties bindings = new Properties();
    private static final double TOLERANCE = 1.0; // 1 meter tolerance for floating point calculations

    @Test
    public void testBasicDistanceCalculation() {
        GeoDistance function = new GeoDistance();

        // Test distance between New York and Los Angeles (approx 3935 km)
        Object result = function.call(bindings, new Object[] { 40.7128, -74.0060, 34.0522, -118.2437 });
        Assert.assertTrue(result instanceof Number);
        double distance = ((Number) result).doubleValue();
        Assert.assertTrue(Math.abs(distance - 3935000) < 50000); // Within 50km tolerance
    }

    @Test
    public void testSamePointDistance() {
        GeoDistance function = new GeoDistance();

        // Test distance between same point (should be 0)
        Object result = function.call(bindings, new Object[] { 40.7128, -74.0060, 40.7128, -74.0060 });
        Assert.assertTrue(result instanceof Number);
        double distance = ((Number) result).doubleValue();
        Assert.assertTrue(Math.abs(distance) < TOLERANCE);
    }

    @Test
    public void testDistanceWithKilometers() {
        GeoDistance function = new GeoDistance();

        // Test with kilometers unit
        Object result = function.call(bindings, new Object[] { 40.7128, -74.0060, 34.0522, -118.2437, "km" });
        Assert.assertTrue(result instanceof Number);
        double distance = ((Number) result).doubleValue();
        Assert.assertTrue(Math.abs(distance - 3935) < 50); // Within 50km tolerance
    }

    @Test
    public void testDistanceWithMiles() {
        GeoDistance function = new GeoDistance();

        // Test with miles unit
        Object result = function.call(bindings, new Object[] { 40.7128, -74.0060, 34.0522, -118.2437, "mi" });
        Assert.assertTrue(result instanceof Number);
        double distance = ((Number) result).doubleValue();
        Assert.assertTrue(Math.abs(distance - 2445) < 50); // Within 50 mile tolerance (approx 3935 km)
    }

    @Test
    public void testShortDistance() {
        GeoDistance function = new GeoDistance();

        // Test short distance (about 1 km)
        Object result = function.call(bindings, new Object[] { 40.7128, -74.0060, 40.7228, -74.0060 });
        Assert.assertTrue(result instanceof Number);
        double distance = ((Number) result).doubleValue();
        Assert.assertTrue(distance > 1000 && distance < 1200); // Approximately 1.1 km
    }

    @Test
    public void testPolarDistance() {
        GeoDistance function = new GeoDistance();

        // Test distance from North Pole to South Pole (should be approximately half Earth's circumference)
        Object result = function.call(bindings, new Object[] { 90.0, 0.0, -90.0, 0.0 });
        Assert.assertTrue(result instanceof Number);
        double distance = ((Number) result).doubleValue();
        Assert.assertTrue(Math.abs(distance - 20015000) < 100000); // Within 100km of expected ~20,015 km
    }

    @Test
    public void testEquatorDistance() {
        GeoDistance function = new GeoDistance();

        // Test distance along equator (90 degrees longitude difference)
        Object result = function.call(bindings, new Object[] { 0.0, 0.0, 0.0, 90.0 });
        Assert.assertTrue(result instanceof Number);
        double distance = ((Number) result).doubleValue();
        Assert.assertTrue(Math.abs(distance - 10007543) < 10000); // Quarter of Earth's circumference at equator
    }

    @Test
    public void testInvalidArgumentCount() {
        GeoDistance function = new GeoDistance();

        // Test too few arguments
        Object result = function.call(bindings, new Object[] { 40.7128, -74.0060 });
        Assert.assertTrue(result instanceof EvalError);

        // Test too many arguments
        result = function.call(bindings, new Object[] { 40.7128, -74.0060, 34.0522, -118.2437, "km", "extra" });
        Assert.assertTrue(result instanceof EvalError);
    }

    @Test
    public void testInvalidCoordinateTypes() {
        GeoDistance function = new GeoDistance();

        // Test non-numeric coordinates
        Object result = function.call(bindings, new Object[] { "40.7128", -74.0060, 34.0522, -118.2437 });
        Assert.assertTrue(result instanceof EvalError);

        result = function.call(bindings, new Object[] { 40.7128, "not-a-number", 34.0522, -118.2437 });
        Assert.assertTrue(result instanceof EvalError);
    }

    @Test
    public void testInvalidCoordinateRanges() {
        GeoDistance function = new GeoDistance();

        // Test latitude out of range
        Object result = function.call(bindings, new Object[] { 91.0, -74.0060, 34.0522, -118.2437 });
        Assert.assertTrue(result instanceof EvalError);

        result = function.call(bindings, new Object[] { 40.7128, -74.0060, -91.0, -118.2437 });
        Assert.assertTrue(result instanceof EvalError);

        // Test longitude out of range
        result = function.call(bindings, new Object[] { 40.7128, 181.0, 34.0522, -118.2437 });
        Assert.assertTrue(result instanceof EvalError);

        result = function.call(bindings, new Object[] { 40.7128, -74.0060, 34.0522, -181.0 });
        Assert.assertTrue(result instanceof EvalError);
    }

    @Test
    public void testInvalidUnit() {
        GeoDistance function = new GeoDistance();

        // Test invalid unit string
        Object result = function.call(bindings, new Object[] { 40.7128, -74.0060, 34.0522, -118.2437, "invalid" });
        Assert.assertTrue(result instanceof EvalError);

        // Test non-string unit
        result = function.call(bindings, new Object[] { 40.7128, -74.0060, 34.0522, -118.2437, 123 });
        Assert.assertTrue(result instanceof EvalError);
    }

    @Test
    public void testValidCoordinateBoundaries() {
        GeoDistance function = new GeoDistance();

        // Test valid boundary coordinates
        Object result = function.call(bindings, new Object[] { 90.0, 180.0, -90.0, -180.0 });
        Assert.assertFalse(result instanceof EvalError);
        Assert.assertTrue(result instanceof Number);
    }

    @Test
    public void testAcrossDateLine() {
        GeoDistance function = new GeoDistance();

        // Test distance calculation across International Date Line
        Object result = function.call(bindings, new Object[] { 0.0, 179.0, 0.0, -179.0 });
        Assert.assertTrue(result instanceof Number);
        double distance = ((Number) result).doubleValue();
        // This should be approximately 222 km (2 degrees at equator)
        Assert.assertTrue(distance > 200000 && distance < 250000);
    }

    @Test
    public void testAllUnits() {
        GeoDistance function = new GeoDistance();

        // Test all supported units return valid results
        Object resultM = function.call(bindings, new Object[] { 40.0, -74.0, 41.0, -74.0, "m" });
        Object resultKm = function.call(bindings, new Object[] { 40.0, -74.0, 41.0, -74.0, "km" });
        Object resultMi = function.call(bindings, new Object[] { 40.0, -74.0, 41.0, -74.0, "mi" });

        Assert.assertTrue(resultM instanceof Number);
        Assert.assertTrue(resultKm instanceof Number);
        Assert.assertTrue(resultMi instanceof Number);

        double distanceM = ((Number) resultM).doubleValue();
        double distanceKm = ((Number) resultKm).doubleValue();
        double distanceMi = ((Number) resultMi).doubleValue();

        // Verify unit conversions are approximately correct
        Assert.assertTrue(Math.abs(distanceM / 1000 - distanceKm) < 1);
        Assert.assertTrue(Math.abs(distanceM / 1609.344 - distanceMi) < 1);
    }
}
