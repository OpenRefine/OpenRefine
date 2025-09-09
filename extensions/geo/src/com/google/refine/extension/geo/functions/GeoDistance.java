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

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.Function;

public class GeoDistance implements Function {

    private static final double EARTH_RADIUS_M = 6371000.0;

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length < 4 || args.length > 5) {
            return new EvalError("geoDistance() expects 4 or 5 arguments: lat1, lng1, lat2, lng2, and optional unit ('m', 'km', 'mi')");
        }

        Double lat1 = extractCoordinate(args[0], "lat1");
        if (lat1 == null) return new EvalError(EvalErrorMessage.expects_first_param_number(ControlFunctionRegistry.getFunctionName(this)));

        Double lng1 = extractCoordinate(args[1], "lng1");
        if (lng1 == null) return new EvalError(EvalErrorMessage.expects_second_param_number(ControlFunctionRegistry.getFunctionName(this)));

        Double lat2 = extractCoordinate(args[2], "lat2");
        if (lat2 == null) return new EvalError("geoDistance() third argument (lat2) must be a number");

        Double lng2 = extractCoordinate(args[3], "lng2");
        if (lng2 == null) return new EvalError("geoDistance() fourth argument (lng2) must be a number");
        if (lat1 < -90 || lat1 > 90) {
            return new EvalError("lat1 must be between -90 and 90 degrees");
        }
        if (lat2 < -90 || lat2 > 90) {
            return new EvalError("lat2 must be between -90 and 90 degrees");
        }
        if (lng1 < -180 || lng1 > 180) {
            return new EvalError("lng1 must be between -180 and 180 degrees");
        }
        if (lng2 < -180 || lng2 > 180) {
            return new EvalError("lng2 must be between -180 and 180 degrees");
        }

        String unit = "m";
        if (args.length == 5) {
            if (args[4] != null && args[4] instanceof String) {
                unit = ((String) args[4]).toLowerCase();
                if (!"m".equals(unit) && !"km".equals(unit) && !"mi".equals(unit)) {
                    return new EvalError("geoDistance() unit must be 'm' (meters), 'km' (kilometers), or 'mi' (miles)");
                }
            } else {
                return new EvalError("geoDistance() fifth argument (unit) must be a string");
            }
        }
        double distance = haversineDistance(lat1, lng1, lat2, lng2);

        switch (unit) {
            case "km":
                distance = distance / 1000.0;
                break;
            case "mi":
                distance = distance / 1609.344;
                break;
        }

        return distance;
    }

    private Double extractCoordinate(Object arg, String paramName) {
        if (!(arg instanceof Number)) {
            return null;
        }
        return ((Number) arg).doubleValue();
    }

    private double haversineDistance(double lat1, double lng1, double lat2, double lng2) {
        double lat1Rad = Math.toRadians(lat1);
        double lng1Rad = Math.toRadians(lng1);
        double lat2Rad = Math.toRadians(lat2);
        double lng2Rad = Math.toRadians(lng2);

        double dlat = lat2Rad - lat1Rad;
        double dlng = lng2Rad - lng1Rad;

        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(dlng / 2) * Math.sin(dlng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_M * c;
    }

    @Override
    public String getDescription() {
        return "Calculates the great circle distance between two coordinate pairs using the Haversine formula. Usage: geoDistance(lat1, lng1, lat2, lng2) or geoDistance(lat1, lng1, lat2, lng2, unit)";
    }

    @Override
    public String getParams() {
        return "number lat1, number lng1, number lat2, number lng2, optional string unit";
    }

    @Override
    public String getReturns() {
        return "number";
    }
}
