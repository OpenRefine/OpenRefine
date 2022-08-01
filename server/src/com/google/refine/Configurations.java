/*

Copyright 2010, Google Inc.
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
    * Neither the name of Google Inc. nor the names of its
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

package com.google.refine;

/**
 * Centralized configuration facility.
 */
public class Configurations {

    public static String get(final String name) {
        return System.getProperty(name);
    }

    public static String get(final String name, final String def) {
        final String val = get(name);
        return (val == null) ? def : val;
    }

    public static boolean getBoolean(final String name, final boolean def) {
        final String val = get(name);
        return (val == null) ? def : Boolean.parseBoolean(val);
    }

    public static int getInteger(final String name, final int def) {
        final String val = get(name);
        try {
            return (val == null) ? def : Integer.parseInt(val);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Could not parse '" + val + "' as an integer number.", e);
        }
    }

    public static float getFloat(final String name, final float def) {
        final String val = get(name);
        try {
            return (val == null) ? def : Float.parseFloat(val);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Could not parse '" + val + "' as a floating point number.", e);
        }
    }

}
