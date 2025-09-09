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

public class DecToGMSTest {

    private static Properties bindings = new Properties();

    @Test
    public void testBasicConversionWithoutCoordType() {
        DecToGMS function = new DecToGMS();

        // Test positive decimal degrees
        Object result = function.call(bindings, new Object[] { 40.7128 });
        Assert.assertEquals(result, "40° 42' 46.08\"");

        // Test negative decimal degrees
        result = function.call(bindings, new Object[] { -74.0060 });
        Assert.assertEquals(result, "74° 0' 21.60\" (-)");
    }

    @Test
    public void testLatitudeConversion() {
        DecToGMS function = new DecToGMS();

        // Test positive latitude (North)
        Object result = function.call(bindings, new Object[] { 40.7128, "lat" });
        Assert.assertEquals(result, "40° 42' 46.08\" N");

        // Test negative latitude (South)
        result = function.call(bindings, new Object[] { -33.8688, "lat" });
        Assert.assertEquals(result, "33° 52' 7.68\" S");
    }

    @Test
    public void testLongitudeConversion() {
        DecToGMS function = new DecToGMS();

        // Test positive longitude (East)
        Object result = function.call(bindings, new Object[] { 151.2093, "lng" });
        Assert.assertEquals(result, "151° 12' 33.48\" E");

        // Test negative longitude (West)
        result = function.call(bindings, new Object[] { -74.0060, "lng" });
        Assert.assertEquals(result, "74° 0' 21.60\" W");

        // Test "lon" alias
        result = function.call(bindings, new Object[] { -74.0060, "lon" });
        Assert.assertEquals(result, "74° 0' 21.60\" W");
    }

    @Test
    public void testExactDegrees() {
        DecToGMS function = new DecToGMS();

        // Test exact degrees
        Object result = function.call(bindings, new Object[] { 45.0, "lat" });
        Assert.assertEquals(result, "45° 0' 0.00\" N");

        result = function.call(bindings, new Object[] { -90.0, "lat" });
        Assert.assertEquals(result, "90° 0' 0.00\" S");
    }

    @Test
    public void testPolarCoordinates() {
        DecToGMS function = new DecToGMS();

        // Test North Pole
        Object result = function.call(bindings, new Object[] { 90.0, "lat" });
        Assert.assertEquals(result, "90° 0' 0.00\" N");

        // Test South Pole
        result = function.call(bindings, new Object[] { -90.0, "lat" });
        Assert.assertEquals(result, "90° 0' 0.00\" S");
    }

    @Test
    public void testDateLineCoordinates() {
        DecToGMS function = new DecToGMS();

        // Test International Date Line
        Object result = function.call(bindings, new Object[] { 180.0, "lng" });
        Assert.assertEquals(result, "180° 0' 0.00\" E");

        result = function.call(bindings, new Object[] { -180.0, "lng" });
        Assert.assertEquals(result, "180° 0' 0.00\" W");
    }

    @Test
    public void testInvalidArguments() {
        DecToGMS function = new DecToGMS();

        // Test no arguments
        Object result = function.call(bindings, new Object[] {});
        Assert.assertTrue(result instanceof EvalError);

        // Test too many arguments
        result = function.call(bindings, new Object[] { 40.7128, "lat", "extra" });
        Assert.assertTrue(result instanceof EvalError);

        // Test non-numeric first argument
        result = function.call(bindings, new Object[] { "not-a-number" });
        Assert.assertTrue(result instanceof EvalError);

        // Test invalid coordinate type
        result = function.call(bindings, new Object[] { 40.7128, "invalid" });
        Assert.assertTrue(result instanceof EvalError);

        // Test non-string coordinate type
        result = function.call(bindings, new Object[] { 40.7128, 123 });
        Assert.assertTrue(result instanceof EvalError);
    }

    @Test
    public void testLatitudeRangeValidation() {
        DecToGMS function = new DecToGMS();

        // Test latitude out of range (too high)
        Object result = function.call(bindings, new Object[] { 91.0, "lat" });
        Assert.assertTrue(result instanceof EvalError);

        // Test latitude out of range (too low)
        result = function.call(bindings, new Object[] { -91.0, "lat" });
        Assert.assertTrue(result instanceof EvalError);

        // Test valid latitude boundary
        result = function.call(bindings, new Object[] { 90.0, "lat" });
        Assert.assertFalse(result instanceof EvalError);

        result = function.call(bindings, new Object[] { -90.0, "lat" });
        Assert.assertFalse(result instanceof EvalError);
    }

    @Test
    public void testLongitudeRangeValidation() {
        DecToGMS function = new DecToGMS();

        // Test longitude out of range (too high)
        Object result = function.call(bindings, new Object[] { 181.0, "lng" });
        Assert.assertTrue(result instanceof EvalError);

        // Test longitude out of range (too low)
        result = function.call(bindings, new Object[] { -181.0, "lng" });
        Assert.assertTrue(result instanceof EvalError);

        // Test valid longitude boundary
        result = function.call(bindings, new Object[] { 180.0, "lng" });
        Assert.assertFalse(result instanceof EvalError);

        result = function.call(bindings, new Object[] { -180.0, "lng" });
        Assert.assertFalse(result instanceof EvalError);
    }

    @Test
    public void testPrecisionHandling() {
        DecToGMS function = new DecToGMS();

        // Test small decimal values
        Object result = function.call(bindings, new Object[] { 0.0001, "lat" });
        Assert.assertEquals(result, "0° 0' 0.36\" N");

        // Test precision with many decimal places
        result = function.call(bindings, new Object[] { 40.712345678, "lat" });
        Assert.assertTrue(((String) result).contains("40°"));
        Assert.assertTrue(((String) result).contains("N"));
    }
}
