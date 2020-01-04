/*

Copyright 2010,2011 Google Inc.
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

package org.openrefine.grel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openrefine.expr.functions.Coalesce;
import org.openrefine.expr.functions.Cross;
import org.openrefine.expr.functions.FacetCount;
import org.openrefine.expr.functions.Get;
import org.openrefine.expr.functions.HasField;
import org.openrefine.expr.functions.Jsonize;
import org.openrefine.expr.functions.Length;
import org.openrefine.expr.functions.Slice;
import org.openrefine.expr.functions.ToDate;
import org.openrefine.expr.functions.ToNumber;
import org.openrefine.expr.functions.ToString;
import org.openrefine.expr.functions.Type;
import org.openrefine.expr.functions.arrays.InArray;
import org.openrefine.expr.functions.arrays.Join;
import org.openrefine.expr.functions.arrays.Reverse;
import org.openrefine.expr.functions.arrays.Sort;
import org.openrefine.expr.functions.arrays.Uniques;
import org.openrefine.expr.functions.booleans.And;
import org.openrefine.expr.functions.booleans.Not;
import org.openrefine.expr.functions.booleans.Or;
import org.openrefine.expr.functions.booleans.Xor;
import org.openrefine.expr.functions.date.DatePart;
import org.openrefine.expr.functions.date.Inc;
import org.openrefine.expr.functions.date.Now;
import org.openrefine.expr.functions.html.InnerHtml;
import org.openrefine.expr.functions.html.ParseHtml;
import org.openrefine.expr.functions.math.ACos;
import org.openrefine.expr.functions.math.ASin;
import org.openrefine.expr.functions.math.ATan;
import org.openrefine.expr.functions.math.ATan2;
import org.openrefine.expr.functions.math.Abs;
import org.openrefine.expr.functions.math.Ceil;
import org.openrefine.expr.functions.math.Combin;
import org.openrefine.expr.functions.math.Cos;
import org.openrefine.expr.functions.math.Cosh;
import org.openrefine.expr.functions.math.Degrees;
import org.openrefine.expr.functions.math.Even;
import org.openrefine.expr.functions.math.Exp;
import org.openrefine.expr.functions.math.Fact;
import org.openrefine.expr.functions.math.FactN;
import org.openrefine.expr.functions.math.Floor;
import org.openrefine.expr.functions.math.GreatestCommonDenominator;
import org.openrefine.expr.functions.math.LeastCommonMultiple;
import org.openrefine.expr.functions.math.Ln;
import org.openrefine.expr.functions.math.Log;
import org.openrefine.expr.functions.math.Max;
import org.openrefine.expr.functions.math.Min;
import org.openrefine.expr.functions.math.Mod;
import org.openrefine.expr.functions.math.Multinomial;
import org.openrefine.expr.functions.math.Odd;
import org.openrefine.expr.functions.math.Pow;
import org.openrefine.expr.functions.math.Quotient;
import org.openrefine.expr.functions.math.Radians;
import org.openrefine.expr.functions.math.RandomNumber;
import org.openrefine.expr.functions.math.Round;
import org.openrefine.expr.functions.math.Sin;
import org.openrefine.expr.functions.math.Sinh;
import org.openrefine.expr.functions.math.Sum;
import org.openrefine.expr.functions.math.Tan;
import org.openrefine.expr.functions.math.Tanh;
import org.openrefine.expr.functions.strings.Chomp;
import org.openrefine.expr.functions.strings.Contains;
import org.openrefine.expr.functions.strings.Diff;
import org.openrefine.expr.functions.strings.EndsWith;
import org.openrefine.expr.functions.strings.Escape;
import org.openrefine.expr.functions.strings.Find;
import org.openrefine.expr.functions.strings.Fingerprint;
import org.openrefine.expr.functions.strings.IndexOf;
import org.openrefine.expr.functions.strings.LastIndexOf;
import org.openrefine.expr.functions.strings.MD5;
import org.openrefine.expr.functions.strings.Match;
import org.openrefine.expr.functions.strings.NGram;
import org.openrefine.expr.functions.strings.NGramFingerprint;
import org.openrefine.expr.functions.strings.ParseJson;
import org.openrefine.expr.functions.strings.Partition;
import org.openrefine.expr.functions.strings.Phonetic;
import org.openrefine.expr.functions.strings.RPartition;
import org.openrefine.expr.functions.strings.Range;
import org.openrefine.expr.functions.strings.Reinterpret;
import org.openrefine.expr.functions.strings.Replace;
import org.openrefine.expr.functions.strings.ReplaceChars;
import org.openrefine.expr.functions.strings.SHA1;
import org.openrefine.expr.functions.strings.SmartSplit;
import org.openrefine.expr.functions.strings.Split;
import org.openrefine.expr.functions.strings.SplitByCharType;
import org.openrefine.expr.functions.strings.SplitByLengths;
import org.openrefine.expr.functions.strings.StartsWith;
import org.openrefine.expr.functions.strings.ToLowercase;
import org.openrefine.expr.functions.strings.ToTitlecase;
import org.openrefine.expr.functions.strings.ToUppercase;
import org.openrefine.expr.functions.strings.Trim;
import org.openrefine.expr.functions.strings.Unescape;
import org.openrefine.expr.functions.strings.Unicode;
import org.openrefine.expr.functions.strings.UnicodeType;
import org.openrefine.expr.functions.xml.InnerXml;
import org.openrefine.expr.functions.xml.OwnText;
import org.openrefine.expr.functions.xml.ParseXml;
import org.openrefine.expr.functions.xml.SelectXml;
import org.openrefine.expr.functions.xml.XmlAttr;
import org.openrefine.expr.functions.xml.XmlText;
import org.openrefine.grel.controls.Filter;
import org.openrefine.grel.controls.ForEach;
import org.openrefine.grel.controls.ForEachIndex;
import org.openrefine.grel.controls.ForNonBlank;
import org.openrefine.grel.controls.ForRange;
import org.openrefine.grel.controls.If;
import org.openrefine.grel.controls.IsBlank;
import org.openrefine.grel.controls.IsEmptyString;
import org.openrefine.grel.controls.IsError;
import org.openrefine.grel.controls.IsNonBlank;
import org.openrefine.grel.controls.IsNotNull;
import org.openrefine.grel.controls.IsNull;
import org.openrefine.grel.controls.IsNumeric;
import org.openrefine.grel.controls.With;

public class ControlFunctionRegistry {

    static private Map<String, Function> s_nameToFunction = new HashMap<String, Function>();
    static private Map<Function, String> s_functionToName = new HashMap<Function, String>();

    static private Map<String, Control> s_nameToControl = new HashMap<String, Control>();
    static private Map<Control, String> s_controlToName = new HashMap<Control, String>();

    static public Function getFunction(String name) {
        return s_nameToFunction.get(name);
    }
    static public String getFunctionName(Function f) {
        return s_functionToName.get(f);
    }
    static public Set<Entry<String, Function>> getFunctionMapping() {
        return s_nameToFunction.entrySet();
    }
    static public Map<String,Function> getFunctionMap() {
        return Collections.unmodifiableMap(s_nameToFunction);
    }

    static public Control getControl(String name) {
        return s_nameToControl.get(name);
    }
    static public String getControlName(Control f) {
        return s_controlToName.get(f);
    }
    static public Set<Entry<String, Control>> getControlMapping() {
        return s_nameToControl.entrySet();
    }
    static public Map<String,Control> getControlMap() {
        return Collections.unmodifiableMap(s_nameToControl);
    }

    static public void registerFunction(String name, Function f) {
        s_nameToFunction.put(name, f);
        s_functionToName.put(f, name);
    }

    static public void registerControl(String name, Control c) {
        s_nameToControl.put(name, c);
        s_controlToName.put(c, name);
    }

    static {
        registerFunction("coalesce", new Coalesce());
        registerFunction("type", new Type());

        registerFunction("toString", new ToString());
        registerFunction("toNumber", new ToNumber());
        registerFunction("toDate", new ToDate());

        registerFunction("toUppercase", new ToUppercase());
        registerFunction("toLowercase", new ToLowercase());
        registerFunction("toTitlecase", new ToTitlecase());

        registerFunction("hasField", new HasField());
        registerFunction("get", new Get());
        registerFunction("slice", new Slice());
        registerFunction("substring", new Slice());
        registerFunction("replace", new Replace());
        registerFunction("replaceChars", new ReplaceChars());
        registerFunction("range", new Range());
        registerFunction("split", new Split());
        registerFunction("smartSplit", new SmartSplit());
        registerFunction("splitByCharType", new SplitByCharType());
        registerFunction("splitByLengths", new SplitByLengths());
        registerFunction("partition", new Partition());
        registerFunction("rpartition", new RPartition());
        registerFunction("trim", new Trim());
        registerFunction("strip", new Trim());
        registerFunction("contains", new Contains());
        registerFunction("escape", new Escape());
        registerFunction("unescape", new Unescape());
        registerFunction("length", new Length());
        registerFunction("sha1", new SHA1());
        registerFunction("md5", new MD5());
        registerFunction("unicode", new Unicode());
        registerFunction("unicodeType", new UnicodeType());
        registerFunction("diff", new Diff());
        registerFunction("chomp", new Chomp());
        registerFunction("fingerprint", new Fingerprint());
        registerFunction("ngramFingerprint", new NGramFingerprint());
        registerFunction("phonetic", new Phonetic());
        registerFunction("reinterpret", new Reinterpret());
        registerFunction("jsonize", new Jsonize());
        registerFunction("parseJson", new ParseJson());
        registerFunction("ngram", new NGram());
        registerFunction("match", new Match());
        registerFunction("find", new Find());

        // XML and HTML functions from JSoup
        registerFunction("parseXml", new ParseXml());
        registerFunction("parseHtml", new ParseHtml());
        registerFunction("select", new SelectXml());
        registerFunction("xmlAttr", new XmlAttr());
        registerFunction("htmlAttr", new XmlAttr());
        registerFunction("xmlText", new XmlText());
        registerFunction("htmlText", new XmlText());
        registerFunction("innerXml", new InnerXml());
        registerFunction("innerHtml", new InnerHtml());
        registerFunction("ownText", new OwnText());

        registerFunction("indexOf", new IndexOf());
        registerFunction("lastIndexOf", new LastIndexOf());
        registerFunction("startsWith", new StartsWith());
        registerFunction("endsWith", new EndsWith());
        registerFunction("join", new Join());
        registerFunction("reverse", new Reverse());
        registerFunction("sort", new Sort());
        registerFunction("uniques", new Uniques());
        registerFunction("inArray", new InArray());

        registerFunction("now", new Now());
        registerFunction("inc", new Inc());
        registerFunction("datePart", new DatePart());

        registerFunction("acos", new ACos());
        registerFunction("asin", new ASin());
        registerFunction("atan", new ATan());
        registerFunction("atan2", new ATan2());
        registerFunction("cos", new Cos());
        registerFunction("cosh", new Cosh());
        registerFunction("sin", new Sin());
        registerFunction("sinh", new Sinh());
        registerFunction("tan", new Tan());
        registerFunction("tanh", new Tanh());
        registerFunction("round", new Round());
        registerFunction("floor", new Floor());
        registerFunction("ceil", new Ceil());
        registerFunction("even", new Even());
        registerFunction("odd", new Odd());
        registerFunction("abs", new Abs());
        registerFunction("mod", new Mod());
        registerFunction("max", new Max());
        registerFunction("min", new Min());
        registerFunction("log", new Log());
        registerFunction("ln", new Ln());
        registerFunction("pow", new Pow());
        registerFunction("exp", new Exp());
        registerFunction("sum", new Sum());
        registerFunction("fact", new Fact());
        registerFunction("factn", new FactN());
        registerFunction("combin", new Combin());
        registerFunction("degrees", new Degrees());
        registerFunction("radians", new Radians());
        registerFunction("randomNumber", new RandomNumber());
        registerFunction("gcd", new GreatestCommonDenominator());
        registerFunction("lcm", new LeastCommonMultiple());
        registerFunction("multinomial", new Multinomial());
        registerFunction("quotient", new Quotient());

        registerFunction("and", new And());
        registerFunction("or", new Or());
        registerFunction("not", new Not());
        registerFunction("xor", new Xor());

        registerFunction("cross", new Cross());

        registerFunction("facetCount", new FacetCount());

        registerControl("if", new If());
        registerControl("with", new With());
        registerControl("forEach", new ForEach());
        registerControl("forEachIndex", new ForEachIndex());
        registerControl("forRange", new ForRange());
        registerControl("filter", new Filter());
        registerControl("forNonBlank", new ForNonBlank());

        registerControl("isNull", new IsNull());
        registerControl("isNotNull", new IsNotNull());
        registerControl("isEmptyString", new IsEmptyString());
        registerControl("isBlank", new IsBlank());
        registerControl("isNonBlank", new IsNonBlank());
        registerControl("isNumeric", new IsNumeric());
        registerControl("isError", new IsError());
    }
}
